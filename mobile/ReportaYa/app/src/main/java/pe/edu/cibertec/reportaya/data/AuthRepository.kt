package pe.edu.cibertec.reportaya.data

import pe.edu.cibertec.reportaya.data.remote.ApiClient
import pe.edu.cibertec.reportaya.data.remote.ApiErrorParser
import pe.edu.cibertec.reportaya.data.remote.LoginRequestDto

sealed class LoginResult {
    data class Success(val name: String, val role: String) : LoginResult()
    data class Error(val message: String) : LoginResult()
}

object AuthRepository {
    suspend fun login(email: String, password: String): LoginResult {
        return try {
            val response = ApiClient.service.login(LoginRequestDto(email.trim().lowercase(), password))
            val body = response.body()
            if (response.isSuccessful && body != null) {
                if (body.user.role != "Residente") {
                    LoginResult.Error("La aplicación móvil está habilitada para el rol Residente.")
                } else {
                    UserSession.start(body)
                    LoginResult.Success(body.user.name, body.user.role)
                }
            } else {
                LoginResult.Error(ApiErrorParser.message(response))
            }
        } catch (error: Exception) {
            LoginResult.Error("No se pudo conectar con ReportaYa. Verifica que la API esté encendida y tu conexión de red.")
        }
    }
}
