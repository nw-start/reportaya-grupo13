package pe.edu.cibertec.reportaya.data.remote

import com.google.gson.annotations.SerializedName

/** Modelos usados para intercambiar datos con la API de ReportaYa. */
data class LoginRequestDto(
    val correo: String,
    val contrasena: String
)

data class AuthResponseDto(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("tokenType") val tokenType: String,
    @SerializedName("usuario") val user: UserDto
)

data class UserDto(
    @SerializedName("idUsuario") val id: Long,
    @SerializedName("nombres") val name: String,
    @SerializedName("correo") val email: String,
    @SerializedName("rol") val role: String,
    @SerializedName("activo") val active: Boolean
)

data class IncidentTypeDto(
    @SerializedName("idTipoIncidencia") val id: Long,
    @SerializedName("nombre") val name: String,
    @SerializedName("activo") val active: Boolean
)

data class IncidentCreateRequestDto(
    @SerializedName("fechaRegistro") val registrationDate: String,
    @SerializedName("horaRegistro") val registrationTime: String,
    @SerializedName("descripcion") val description: String,
    @SerializedName("ubicacion") val location: String,
    @SerializedName("fotoUrl") val photoUrl: String? = null,
    @SerializedName("idTipoIncidencia") val incidentTypeId: Long,
    @SerializedName("idUsuarioRegistro") val registeredById: Long
)

data class IncidentUpdateRequestDto(
    @SerializedName("idTipoIncidencia") val incidentTypeId: Long? = null,
    @SerializedName("descripcion") val description: String? = null,
    @SerializedName("ubicacion") val location: String? = null
)

data class IncidentResponseDto(
    @SerializedName("idIncidencia") val id: Long,
    @SerializedName("codigo") val code: String,
    @SerializedName("fechaRegistro") val registrationDate: String,
    @SerializedName("horaRegistro") val registrationTime: String,
    @SerializedName("descripcion") val description: String,
    @SerializedName("ubicacion") val location: String,
    @SerializedName("estado") val state: String,
    @SerializedName("fotoUrl") val photoUrl: String?,
    @SerializedName("fechaAtencion") val attendedAt: String?,
    @SerializedName("observacionAtencion") val attentionNote: String?,
    @SerializedName("eliminado") val deleted: Boolean,
    @SerializedName("fechaActualizacion") val updatedAt: String,
    @SerializedName("idTipoIncidencia") val incidentTypeId: Long,
    @SerializedName("idUsuarioRegistro") val registeredById: Long,
    @SerializedName("tipoIncidencia") val incidentType: IncidentTypeDto,
    @SerializedName("usuarioRegistro") val registeredBy: UserSummaryDto
)

data class UserSummaryDto(
    @SerializedName("idUsuario") val id: Long,
    @SerializedName("nombres") val name: String
)
