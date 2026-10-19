package pe.edu.cibertec.reportaya.data

import android.content.Context
import android.content.SharedPreferences
import pe.edu.cibertec.reportaya.data.remote.AuthResponseDto
import pe.edu.cibertec.reportaya.security.SecureTokenStore

/** Sesión del residente. El token se cifra con Android Keystore; solo datos no secretos quedan en preferencias normales. */
object UserSession {
    private const val PREFS = "reportaya_session_meta"
    private lateinit var prefs: SharedPreferences
    private lateinit var tokenStore: SecureTokenStore

    fun initialize(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            tokenStore = SecureTokenStore(context)
            // Borra cualquier token plano de versiones anteriores.
            context.applicationContext.getSharedPreferences("reportaya_session", Context.MODE_PRIVATE).edit().clear().apply()
        }
    }

    val accessToken: String? get() = if (::tokenStore.isInitialized) tokenStore.read() else null
    val email: String get() = if (::prefs.isInitialized) prefs.getString("email", "") ?: "" else ""
    val displayName: String get() = if (::prefs.isInitialized) prefs.getString("name", "") ?: "" else ""
    val role: String get() = if (::prefs.isInitialized) prefs.getString("role", "") ?: "" else ""
    val userId: Long get() = if (::prefs.isInitialized) prefs.getLong("user_id", -1L) else -1L
    val isAuthenticated: Boolean get() = !accessToken.isNullOrBlank() && email.isNotBlank() && userId > 0 && role == "Residente"

    fun start(auth: AuthResponseDto) {
        check(::prefs.isInitialized && ::tokenStore.isInitialized) { "UserSession no inicializada." }
        require(auth.user.role == "Residente") { "La app móvil solo admite Residentes." }
        tokenStore.save(auth.accessToken)
        prefs.edit().putString("email", auth.user.email.trim().lowercase()).putString("name", auth.user.name)
            .putString("role", auth.user.role).putLong("user_id", auth.user.id).apply()
    }

    fun clear() {
        if (::prefs.isInitialized) prefs.edit().clear().apply()
        if (::tokenStore.isInitialized) tokenStore.clear()
    }
}
