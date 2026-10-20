package pe.edu.cibertec.reportaya.data

import android.content.Context
import pe.edu.cibertec.reportaya.data.local.SQLiteIncidentDataSource
import pe.edu.cibertec.reportaya.model.IncidentState
import pe.edu.cibertec.reportaya.model.IncidentUiModel

/** Contrato local de datos de la aplicación móvil. */
interface IncidentUiDataSource {
    fun listIncidents(): List<IncidentUiModel>
    fun searchIncidents(query: String, state: IncidentState? = null): List<IncidentUiModel>
    fun findIncidentById(id: Long): IncidentUiModel?
    fun createIncident(
        type: String,
        description: String,
        location: String,
        photoUri: String?,
        user: String
    ): IncidentUiModel
    fun updateIncident(
        id: Long,
        type: String,
        description: String,
        location: String,
        photoUri: String?
    ): IncidentUiModel?
    fun deleteIncident(id: Long): Boolean
    fun listPendingOperations(): List<IncidentUiModel>
    fun pendingCount(): Int

    /** Confirma una operación enviada por REST. */
    fun markAsSynced(
        localId: Long,
        serverId: Long,
        serverCode: String,
        serverState: IncidentState
    ): Boolean

    fun markSyncError(localId: Long, message: String): Boolean

    /**
     * Inserta o refresca una incidencia obtenida desde FastAPI. Si existe un cambio
     * local pendiente para el mismo registro, ese cambio local no se sobreescribe.
     */
    fun upsertServerIncident(
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
    ): IncidentUiModel

    fun pruneSyncedServerIncidents(validServerIds: Set<Long>): Int
}

object IncidentDataSourceRegistry {
    @Volatile
    private var source: IncidentUiDataSource? = null

    fun initialize(context: Context) {
        if (source == null) {
            synchronized(this) {
                if (source == null) source = SQLiteIncidentDataSource(context.applicationContext)
            }
        }
    }

    val current: IncidentUiDataSource
        get() = checkNotNull(source) {
            "IncidentDataSourceRegistry debe inicializarse desde ReportaYaApplication."
        }
}
