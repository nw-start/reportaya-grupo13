package pe.edu.cibertec.reportaya.data.sync

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pe.edu.cibertec.reportaya.data.IncidentDataSourceRegistry
import pe.edu.cibertec.reportaya.data.UserSession
import pe.edu.cibertec.reportaya.data.remote.ApiClient
import pe.edu.cibertec.reportaya.data.remote.ApiErrorParser
import pe.edu.cibertec.reportaya.data.remote.IncidentCreateRequestDto
import pe.edu.cibertec.reportaya.data.remote.IncidentResponseDto
import pe.edu.cibertec.reportaya.data.remote.IncidentTypeDto
import pe.edu.cibertec.reportaya.data.remote.IncidentUpdateRequestDto
import pe.edu.cibertec.reportaya.model.IncidentState
import pe.edu.cibertec.reportaya.model.IncidentUiModel
import pe.edu.cibertec.reportaya.model.LocalSyncStatus

/** Resultado resumido de una ejecución de sincronización local ↔ servidor. */
data class SyncResult(
    val attempted: Int,
    val succeeded: Int,
    val failed: Int,
    val downloaded: Int,
    val authExpired: Boolean = false,
    val message: String
) {
    val isSuccessful: Boolean get() = !authExpired && failed == 0
}

/**
 * Sincroniza la cola SQLite con FastAPI y después descarga el estado actual del
 * servidor. Mantiene un enfoque local-first: si la red falla, los datos siguen
 * disponibles y conservan su estado pendiente.
 */
class IncidentSyncManager(@Suppress("UNUSED_PARAMETER") context: Context) {
    private val local get() = IncidentDataSourceRegistry.current
    private val api get() = ApiClient.service

    suspend fun synchronize(): SyncResult = withContext(Dispatchers.IO) {
        if (!UserSession.isAuthenticated) {
            return@withContext SyncResult(0, 0, 0, 0, authExpired = true, message = "Debes iniciar sesión para sincronizar.")
        }

        try {
            val typesResponse = api.listIncidentTypes()
            if (typesResponse.code() == 401) return@withContext expired()
            val types = typesResponse.body()
            if (!typesResponse.isSuccessful || types == null) {
                return@withContext SyncResult(0, 0, 1, 0, message = ApiErrorParser.message(typesResponse))
            }
            val typesByName = types.associateBy { it.name.trim().lowercase() }

            val pending = local.listPendingOperations().sortedBy { it.updatedAtEpoch }
            var ok = 0
            var failed = 0

            for (item in pending) {
                val success = syncOne(item, typesByName)
                if (success) ok++ else failed++
            }

            val pullResponse = api.listIncidents()
            if (pullResponse.code() == 401) return@withContext expired(pending.size, ok, failed)
            var downloaded = 0
            if (pullResponse.isSuccessful) {
                val remoteItems = pullResponse.body().orEmpty()
                remoteItems.filter { it.registeredById == UserSession.userId }.forEach { remote ->
                    local.upsertServerIncident(
                        serverId = remote.id,
                        serverCode = remote.code,
                        type = remote.incidentType.name,
                        description = remote.description,
                        location = remote.location,
                        state = IncidentState.fromLabel(remote.state),
                        registeredAt = formatRemoteDateTime(remote.registrationDate, remote.registrationTime),
                        user = UserSession.email.ifBlank { remote.registeredBy.name },
                        attentionDate = remote.attendedAt?.let(::formatRemoteDateTime),
                        attentionObservation = remote.attentionNote
                    )
                }
                val ownRemoteIds = remoteItems.filter { it.registeredById == UserSession.userId }.map { it.id }.toSet()
                local.pruneSyncedServerIncidents(ownRemoteIds)
                downloaded = ownRemoteIds.size
            } else {
                failed++
            }

            val message = when {
                failed == 0 && pending.isEmpty() -> "Datos actualizados desde el servidor."
                failed == 0 -> "Sincronización completada: $ok operación(es) enviada(s)."
                else -> "Sincronización parcial: $ok correcta(s), $failed con error."
            }
            SyncResult(pending.size, ok, failed, downloaded, message = message)
        } catch (_: Exception) {
            SyncResult(
                attempted = local.pendingCount(),
                succeeded = 0,
                failed = local.pendingCount().coerceAtLeast(1),
                downloaded = 0,
                message = "No fue posible conectar con la API. Tus cambios permanecen guardados en SQLite."
            )
        }
    }

    private suspend fun syncOne(
        item: IncidentUiModel,
        typesByName: Map<String, IncidentTypeDto>
    ): Boolean {
        val operation = when {
            item.syncStatus == LocalSyncStatus.PENDING_DELETE || (item.syncStatus == LocalSyncStatus.ERROR && item.isDeleted) -> "DELETE"
            item.serverId == null -> "CREATE"
            else -> "UPDATE"
        }
        return when (operation) {
            "CREATE" -> createRemote(item, typesByName)
            "UPDATE" -> updateRemote(item, typesByName)
            else -> deleteRemote(item)
        }
    }

    private suspend fun createRemote(item: IncidentUiModel, typesByName: Map<String, IncidentTypeDto>): Boolean {
        val type = typesByName[item.type.trim().lowercase()]
            ?: return local.markSyncError(item.id, "El tipo '${item.type}' no existe en el servidor.").let { false }
        val (date, time) = localDateTimeForApi(item.registeredAt)
        val response = api.createIncident(
            IncidentCreateRequestDto(
                registrationDate = date,
                registrationTime = time,
                description = item.description,
                location = item.location,
                photoUrl = remotePhotoUrlOrNull(item.photoUri),
                incidentTypeId = type.id,
                registeredById = UserSession.userId
            )
        )
        if (response.code() == 401) {
            UserSession.clear()
            local.markSyncError(item.id, "Sesión vencida.")
            return false
        }
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            local.markAsSynced(item.id, body.id, body.code, IncidentState.fromLabel(body.state))
        } else {
            local.markSyncError(item.id, ApiErrorParser.message(response))
            false
        }
    }

    private suspend fun updateRemote(item: IncidentUiModel, typesByName: Map<String, IncidentTypeDto>): Boolean {
        val serverId = item.serverId ?: return false
        val type = typesByName[item.type.trim().lowercase()]
            ?: return local.markSyncError(item.id, "El tipo '${item.type}' no existe en el servidor.").let { false }
        val response = api.updateIncident(
            serverId,
            IncidentUpdateRequestDto(
                incidentTypeId = type.id,
                description = item.description,
                location = item.location
            )
        )
        if (response.code() == 401) {
            UserSession.clear()
            local.markSyncError(item.id, "Sesión vencida.")
            return false
        }
        val body = response.body()
        return if (response.isSuccessful && body != null) {
            local.markAsSynced(item.id, body.id, body.code, IncidentState.fromLabel(body.state))
        } else {
            local.markSyncError(item.id, ApiErrorParser.message(response))
            false
        }
    }

    private suspend fun deleteRemote(item: IncidentUiModel): Boolean {
        val serverId = item.serverId
        if (serverId == null) return true
        val response = api.deleteIncident(serverId)
        if (response.code() == 401) {
            UserSession.clear()
            local.markSyncError(item.id, "Sesión vencida.")
            return false
        }
        return if (response.isSuccessful || response.code() == 404) {
            local.markAsSynced(item.id, serverId, item.code, item.state)
        } else {
            local.markSyncError(item.id, ApiErrorParser.message(response))
            false
        }
    }

    private fun expired(attempted: Int = 0, succeeded: Int = 0, failed: Int = 0): SyncResult {
        UserSession.clear()
        return SyncResult(attempted, succeeded, failed, 0, authExpired = true, message = "Tu sesión venció. Inicia sesión nuevamente.")
    }

    companion object {
        /** Convierte "dd/MM/yyyy · HH:mm" a los formatos ISO esperados por FastAPI. */
        fun localDateTimeForApi(value: String): Pair<String, String> {
            val parts = value.split("·").map { it.trim() }
            val date = parts.getOrNull(0).orEmpty().split("/")
            val isoDate = if (date.size == 3) "${date[2]}-${date[1].padStart(2, '0')}-${date[0].padStart(2, '0')}" else value.take(10)
            val rawTime = parts.getOrNull(1).orEmpty()
            val isoTime = if (rawTime.count { it == ':' } == 1) "$rawTime:00" else rawTime.ifBlank { "00:00:00" }
            return isoDate to isoTime
        }

        fun formatRemoteDateTime(date: String, time: String): String {
            val d = date.take(10).split("-")
            val prettyDate = if (d.size == 3) "${d[2]}/${d[1]}/${d[0]}" else date.take(10)
            return "$prettyDate · ${time.take(5)}"
        }

        fun formatRemoteDateTime(dateTime: String): String {
            val normalized = dateTime.replace('T', ' ')
            val date = normalized.take(10)
            val time = normalized.drop(11).take(5)
            return formatRemoteDateTime(date, time)
        }

        private fun remotePhotoUrlOrNull(value: String?): String? =
            value?.takeIf { it.startsWith("https://") || it.startsWith("http://") }
    }
}
