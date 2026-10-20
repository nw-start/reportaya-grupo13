package pe.edu.cibertec.reportaya.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import pe.edu.cibertec.reportaya.R
import pe.edu.cibertec.reportaya.data.AuthRepository
import pe.edu.cibertec.reportaya.data.LoginResult
import pe.edu.cibertec.reportaya.data.UserSession
import pe.edu.cibertec.reportaya.data.sync.IncidentSyncManager
import pe.edu.cibertec.reportaya.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (UserSession.isAuthenticated) {
            openHome()
            return
        }

        binding.btnLogin.setOnClickListener { validateAndLogin() }
    }

    private fun validateAndLogin() {
        val email = binding.etUser.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        val validEmail = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

        binding.tilUser.error = when {
            email.isBlank() -> getString(R.string.error_user_required)
            !validEmail -> getString(R.string.error_email_invalid)
            else -> null
        }
        binding.tilPassword.error = if (password.length < 8) getString(R.string.error_password_min) else null
        if (!validEmail || password.length < 8) return

        setLoading(true)
        lifecycleScope.launch {
            when (val result = AuthRepository.login(email, password)) {
                is LoginResult.Success -> {
                    // Login ya comprobó conectividad. Se aprovecha para traer el estado
                    // más reciente del residente antes de abrir el inicio.
                    IncidentSyncManager(this@LoginActivity).synchronize()
                    openHome()
                }
                is LoginResult.Error -> {
                    setLoading(false)
                    Snackbar.make(binding.root, result.message, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnLogin.isEnabled = !loading
        binding.etUser.isEnabled = !loading
        binding.etPassword.isEnabled = !loading
        binding.loginProgress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.text = getString(if (loading) R.string.login_connecting else R.string.login)
    }

    private fun openHome() {
        startActivity(Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
