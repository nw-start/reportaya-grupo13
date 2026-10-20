package pe.edu.cibertec.reportaya.data.remote

import com.google.gson.Gson
import retrofit2.Response

object ApiErrorParser {
    private data class ErrorBody(val detail: String? = null)

    fun message(response: Response<*>): String {
        val raw = runCatching { response.errorBody()?.string() }.getOrNull().orEmpty()
        if (raw.isNotBlank()) {
            val detail = runCatching { Gson().fromJson(raw, ErrorBody::class.java)?.detail }.getOrNull()
            if (!detail.isNullOrBlank()) return detail
        }
        return when (response.code()) {
            400 -> "La solicitud no es válida."
            401 -> "El correo o la contraseña son incorrectos."
            403 -> "No tienes permiso para realizar esta operación."
            404 -> "El servicio solicitado no está disponible."
            409 -> "La operación entra en conflicto con el estado actual."
            422 -> "Los datos ingresados no son válidos."
            429 -> "Demasiados intentos. Inténtalo nuevamente en unos minutos."
            else -> "El servidor respondió con HTTP ${response.code()}."
        }
    }
}
