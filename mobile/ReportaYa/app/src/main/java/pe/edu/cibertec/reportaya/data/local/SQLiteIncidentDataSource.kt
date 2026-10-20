package pe.edu.cibertec.reportaya.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import pe.edu.cibertec.reportaya.data.IncidentUiDataSource
import pe.edu.cibertec.reportaya.data.UserSession
import pe.edu.cibertec.reportaya.model.IncidentState
import pe.edu.cibertec.reportaya.model.IncidentUiModel
import pe.edu.cibertec.reportaya.model.LocalSyncStatus
import pe.edu.cibertec.reportaya.data.local.ReportaYaDbHelper as Db

/**
 * Persistencia local de incidencias con SQLite.
 *
 * La tabla conserva tanto la información visible de la incidencia como el estado
 * de sincronización utilizado por la sincronización con la API.
 */
class SQLiteIncidentDataSource(context: Context) : IncidentUiDataSource {
    private val helper = Db(context)

    override fun listIncidents(): List<IncidentUiModel> =
        queryIncidents(
            selection = "${Db.COL_IS_DELETED} = ? AND ${Db.COL_USER} = ?",
            selectionArgs = arrayOf("0", currentUser())
        )

    override fun searchIncidents(query: String, state: IncidentState?): List<IncidentUiModel> {
        val text = query.trim()
        val clauses = mutableListOf("${Db.COL_IS_DELETED} = ?", "${Db.COL_USER} = ?")
        val args = mutableListOf("0", currentUser())

        if (state != null) {
            clauses += "${Db.COL_STATE} = ?"
            args += state.label
        }
        if (text.isNotBlank()) {
            clauses += """(
                ${Db.COL_CODE} LIKE ? COLLATE NOCASE OR
                ${Db.COL_TYPE} LIKE ? COLLATE NOCASE OR
                ${Db.COL_DESCRIPTION} LIKE ? COLLATE NOCASE OR
                ${Db.COL_LOCATION} LIKE ? COLLATE NOCASE OR
                ${Db.COL_REGISTERED_AT} LIKE ? COLLATE NOCASE
            )""".trimIndent()
            repeat(5) { args += "%$text%" }
        }
        return queryIncidents(clauses.joinToString(" AND "), args.toTypedArray())
    }

    override fun findIncidentById(id: Long): IncidentUiModel? =
        queryIncidents(
            selection = "${Db.COL_ID} = ? AND ${Db.COL_IS_DELETED} = ? AND ${Db.COL_USER} = ?",
            selectionArgs = arrayOf(id.toString(), "0", currentUser()),
            limit = "1"
        ).firstOrNull()

    override fun createIncident(
        type: String,
        description: String,
        location: String,
        photoUri: String?,
        user: String
    ): IncidentUiModel {
        val db = helper.writableDatabase
        val now = System.currentTimeMillis()
        val localUuid = UUID.randomUUID().toString()
        var localId = -1L
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put(Db.COL_LOCAL_UUID, localUuid)
                put(Db.COL_CODE, "TMP-${localUuid.take(8)}")
                put(Db.COL_TYPE, type.trim())
                put(Db.COL_DESCRIPTION, description.trim())
                put(Db.COL_LOCATION, location.trim())
                put(Db.COL_STATE, IncidentState.PENDING.label)
                put(Db.COL_REGISTERED_AT, DATE_FORMAT.format(Date(now)))
                put(Db.COL_USER, user.ifBlank { DEFAULT_USER })
                put(Db.COL_PHOTO_URI, photoUri)
                put(Db.COL_SYNC_STATUS, LocalSyncStatus.PENDING_CREATE.dbValue)
                put(Db.COL_IS_DELETED, 0)
                put(Db.COL_UPDATED_AT, now)
            }
            localId = db.insertOrThrow(Db.TABLE_INCIDENT, null, values)
            val localCode = "LOC-${localId.toString().padStart(4, '0')}"
            db.update(
                Db.TABLE_INCIDENT,
                ContentValues().apply { put(Db.COL_CODE, localCode) },
                "${Db.COL_ID} = ?",
                arrayOf(localId.toString())
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return requireNotNull(findIncidentById(localId))
    }

    override fun updateIncident(
        id: Long,
        type: String,
        description: String,
        location: String,
        photoUri: String?
    ): IncidentUiModel? {
        val current = queryByIdIncludingDeleted(id) ?: return null
        if (current.state != IncidentState.PENDING || current.isDeleted) return null

        val nextSync = if (current.serverId == null) {
            LocalSyncStatus.PENDING_CREATE
        } else {
            LocalSyncStatus.PENDING_UPDATE
        }
        val values = ContentValues().apply {
            put(Db.COL_TYPE, type.trim())
            put(Db.COL_DESCRIPTION, description.trim())
            put(Db.COL_LOCATION, location.trim())
            put(Db.COL_PHOTO_URI, photoUri)
            put(Db.COL_SYNC_STATUS, nextSync.dbValue)
            put(Db.COL_UPDATED_AT, System.currentTimeMillis())
            putNull(Db.COL_LAST_ERROR)
        }
        val rows = helper.writableDatabase.update(
            Db.TABLE_INCIDENT,
            values,
            "${Db.COL_ID} = ? AND ${Db.COL_IS_DELETED} = 0",
            arrayOf(id.toString())
        )
        return if (rows == 1) findIncidentById(id) else null
    }

    override fun deleteIncident(id: Long): Boolean {
        val current = queryByIdIncludingDeleted(id) ?: return false
        if (current.state != IncidentState.PENDING || current.isDeleted) return false
        val db = helper.writableDatabase

        // Si nunca llegó al servidor, basta con quitar el registro local.
        if (current.serverId == null) {
            // El servidor nunca conoció esta fila (incluye errores de creación),
            // así que no existe nada remoto que eliminar.
            return db.delete(Db.TABLE_INCIDENT, "${Db.COL_ID} = ?", arrayOf(id.toString())) == 1
        }

        // Si ya existía en servidor, se oculta de la UI pero se conserva en la cola
        // para enviar la eliminación al servidor.
        val values = ContentValues().apply {
            put(Db.COL_IS_DELETED, 1)
            put(Db.COL_SYNC_STATUS, LocalSyncStatus.PENDING_DELETE.dbValue)
            put(Db.COL_UPDATED_AT, System.currentTimeMillis())
            putNull(Db.COL_LAST_ERROR)
        }
        return db.update(Db.TABLE_INCIDENT, values, "${Db.COL_ID} = ?", arrayOf(id.toString())) == 1
    }

    override fun listPendingOperations(): List<IncidentUiModel> =
        queryIncidents(
            selection = "${Db.COL_SYNC_STATUS} <> ? AND ${Db.COL_USER} = ?",
            selectionArgs = arrayOf(LocalSyncStatus.SYNCED.dbValue, currentUser()),
            includeDeleted = true
        )

    override fun pendingCount(): Int {
        helper.readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM ${Db.TABLE_INCIDENT} WHERE ${Db.COL_SYNC_STATUS} <> ? AND ${Db.COL_USER} = ?",
            arrayOf(LocalSyncStatus.SYNCED.dbValue, currentUser())
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    override fun markAsSynced(
        localId: Long,
        serverId: Long,
        serverCode: String,
        serverState: IncidentState
    ): Boolean {
        val current = queryByIdIncludingDeleted(localId) ?: return false
        val db = helper.writableDatabase
        if (current.syncStatus == LocalSyncStatus.PENDING_DELETE) {
            // El servidor confirmó la eliminación: ya no se necesita conservar la fila local.
            return db.delete(Db.TABLE_INCIDENT, "${Db.COL_ID} = ?", arrayOf(localId.toString())) == 1
        }
        val values = ContentValues().apply {
            put(Db.COL_SERVER_ID, serverId)
            put(Db.COL_CODE, serverCode)
            put(Db.COL_STATE, serverState.label)
            put(Db.COL_SYNC_STATUS, LocalSyncStatus.SYNCED.dbValue)
            put(Db.COL_IS_DELETED, 0)
            put(Db.COL_UPDATED_AT, System.currentTimeMillis())
            putNull(Db.COL_LAST_ERROR)
        }
        return db.update(Db.TABLE_INCIDENT, values, "${Db.COL_ID} = ?", arrayOf(localId.toString())) == 1
    }

    override fun markSyncError(localId: Long, message: String): Boolean {
        val values = ContentValues().apply {
            put(Db.COL_SYNC_STATUS, LocalSyncStatus.ERROR.dbValue)
            put(Db.COL_LAST_ERROR, message.take(250))
            put(Db.COL_UPDATED_AT, System.currentTimeMillis())
        }
        return helper.writableDatabase.update(
            Db.TABLE_INCIDENT,
            values,
            "${Db.COL_ID} = ?",
            arrayOf(localId.toString())
        ) == 1
    }


    override fun upsertServerIncident(
        serverId: Long,
        serverCode: String,
        type: String,
        description: String,
        location: String,
        state: IncidentState,
        registeredAt: String,
        user: String,
        attentionDate: String?,
        attentionObservation: String?
    ): IncidentUiModel {
        val existing = queryByServerIdIncludingDeleted(serverId)
        if (existing != null && existing.syncStatus != LocalSyncStatus.SYNCED) {
            // No pisar una creación/edición/eliminación local aún no confirmada.
            return existing
        }

        val values = ContentValues().apply {
            put(Db.COL_SERVER_ID, serverId)
            put(Db.COL_CODE, serverCode)
            put(Db.COL_TYPE, type)
            put(Db.COL_DESCRIPTION, description)
            put(Db.COL_LOCATION, location)
            put(Db.COL_STATE, state.label)
            put(Db.COL_REGISTERED_AT, registeredAt)
            put(Db.COL_USER, user)
            put(Db.COL_ATTENTION_DATE, attentionDate)
            put(Db.COL_ATTENTION_OBSERVATION, attentionObservation)
            put(Db.COL_SYNC_STATUS, LocalSyncStatus.SYNCED.dbValue)
            put(Db.COL_IS_DELETED, 0)
            put(Db.COL_UPDATED_AT, System.currentTimeMillis())
            putNull(Db.COL_LAST_ERROR)
        }

        val db = helper.writableDatabase
        val localId = if (existing == null) {
            values.put(Db.COL_LOCAL_UUID, "server-$serverId")
            db.insertOrThrow(Db.TABLE_INCIDENT, null, values)
        } else {
            db.update(
                Db.TABLE_INCIDENT,
                values,
                "${Db.COL_ID} = ?",
                arrayOf(existing.id.toString())
            )
            existing.id
        }
        return requireNotNull(queryByIdIncludingDeleted(localId))
    }

    override fun pruneSyncedServerIncidents(validServerIds: Set<Long>): Int {
        val db = helper.writableDatabase
        val clauses = mutableListOf(
            "${Db.COL_USER} = ?",
            "${Db.COL_SYNC_STATUS} = ?",
            "${Db.COL_SERVER_ID} IS NOT NULL"
        )
        val args = mutableListOf(currentUser(), LocalSyncStatus.SYNCED.dbValue)
        if (validServerIds.isNotEmpty()) {
            clauses += "${Db.COL_SERVER_ID} NOT IN (${validServerIds.joinToString(",") { "?" }})"
            args += validServerIds.map { it.toString() }
        }
        return db.delete(Db.TABLE_INCIDENT, clauses.joinToString(" AND "), args.toTypedArray())
    }

    private fun queryByServerIdIncludingDeleted(serverId: Long): IncidentUiModel? =
        queryIncidents(
            selection = "${Db.COL_SERVER_ID} = ? AND ${Db.COL_USER} = ?",
            selectionArgs = arrayOf(serverId.toString(), currentUser()),
            includeDeleted = true,
            limit = "1"
        ).firstOrNull()

    private fun queryByIdIncludingDeleted(id: Long): IncidentUiModel? =
        queryIncidents(
            selection = "${Db.COL_ID} = ? AND ${Db.COL_USER} = ?",
            selectionArgs = arrayOf(id.toString(), currentUser()),
            includeDeleted = true,
            limit = "1"
        ).firstOrNull()

    private fun queryIncidents(
        selection: String,
        selectionArgs: Array<String>,
        includeDeleted: Boolean = false,
        limit: String? = null
    ): List<IncidentUiModel> {
        val rows = mutableListOf<IncidentUiModel>()
        helper.readableDatabase.query(
            Db.TABLE_INCIDENT,
            ALL_COLUMNS,
            selection,
            selectionArgs,
            null,
            null,
            "${Db.COL_UPDATED_AT} DESC, ${Db.COL_ID} DESC",
            limit
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val item = cursor.toIncident()
                if (includeDeleted || !item.isDeleted) rows += item
            }
        }
        return rows
    }

    private fun Cursor.toIncident(): IncidentUiModel = IncidentUiModel(
        id = getLong(index(Db.COL_ID)),
        serverId = getLongOrNull(Db.COL_SERVER_ID),
        localUuid = getString(index(Db.COL_LOCAL_UUID)),
        code = getString(index(Db.COL_CODE)),
        type = getString(index(Db.COL_TYPE)),
        description = getString(index(Db.COL_DESCRIPTION)),
        location = getString(index(Db.COL_LOCATION)),
        state = IncidentState.fromLabel(getString(index(Db.COL_STATE))),
        registeredAt = getString(index(Db.COL_REGISTERED_AT)),
        user = getString(index(Db.COL_USER)),
        photoUri = getStringOrNull(Db.COL_PHOTO_URI),
        attentionDate = getStringOrNull(Db.COL_ATTENTION_DATE),
        attentionObservation = getStringOrNull(Db.COL_ATTENTION_OBSERVATION),
        syncStatus = LocalSyncStatus.fromDb(getString(index(Db.COL_SYNC_STATUS))),
        isDeleted = getInt(index(Db.COL_IS_DELETED)) == 1,
        updatedAtEpoch = getLong(index(Db.COL_UPDATED_AT)),
        lastSyncError = getStringOrNull(Db.COL_LAST_ERROR)
    )

    private fun Cursor.index(column: String): Int = getColumnIndexOrThrow(column)
    private fun Cursor.getStringOrNull(column: String): String? =
        index(column).let { if (isNull(it)) null else getString(it) }
    private fun Cursor.getLongOrNull(column: String): Long? =
        index(column).let { if (isNull(it)) null else getLong(it) }

    private fun currentUser(): String = UserSession.email.ifBlank { DEFAULT_USER }

    companion object {
        private const val DEFAULT_USER = "residente.demo@reportaya.pe"
        private val DATE_FORMAT = SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.forLanguageTag("es-PE"))
        private val ALL_COLUMNS = arrayOf(
            Db.COL_ID, Db.COL_SERVER_ID, Db.COL_LOCAL_UUID, Db.COL_CODE, Db.COL_TYPE,
            Db.COL_DESCRIPTION, Db.COL_LOCATION, Db.COL_STATE, Db.COL_REGISTERED_AT,
            Db.COL_USER, Db.COL_PHOTO_URI, Db.COL_ATTENTION_DATE, Db.COL_ATTENTION_OBSERVATION,
            Db.COL_SYNC_STATUS, Db.COL_IS_DELETED, Db.COL_UPDATED_AT, Db.COL_LAST_ERROR
        )
    }
}
