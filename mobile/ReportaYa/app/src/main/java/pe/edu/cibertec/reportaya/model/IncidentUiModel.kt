package pe.edu.cibertec.reportaya.model

data class IncidentUiModel(
    val id: Long,
    val code: String,
    val type: String,
    val description: String,
    val location: String,
    val state: IncidentState,
    val registeredAt: String,
    val user: String,
    val photoUri: String? = null,
    val attentionDate: String? = null,
    val attentionObservation: String? = null,
    val serverId: Long? = null,
    val localUuid: String = "",
    val syncStatus: LocalSyncStatus = LocalSyncStatus.SYNCED,
    val isDeleted: Boolean = false,
    val updatedAtEpoch: Long = 0L,
    val lastSyncError: String? = null
)

enum class IncidentState(val label: String) {
    PENDING("Pendiente"),
    IN_PROGRESS("En proceso"),
    ATTENDED("Atendido"),
    REJECTED("Rechazado");

    companion object {
        fun fromLabel(value: String?): IncidentState =
            entries.firstOrNull { it.label.equals(value, ignoreCase = true) } ?: PENDING
    }
}

enum class LocalSyncStatus(val dbValue: String, val label: String) {
    SYNCED("SINCRONIZADO", "Sincronizado"),
    PENDING_CREATE("PENDIENTE_CREAR", "Pendiente de envío"),
    PENDING_UPDATE("PENDIENTE_ACTUALIZAR", "Pendiente de actualización"),
    PENDING_DELETE("PENDIENTE_ELIMINAR", "Pendiente de eliminación"),
    ERROR("ERROR", "Error de sincronización");

    val isPending: Boolean
        get() = this != SYNCED

    companion object {
        fun fromDb(value: String?): LocalSyncStatus =
            entries.firstOrNull { it.dbValue == value } ?: SYNCED
    }
}
