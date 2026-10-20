package pe.edu.cibertec.reportaya.ui

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import pe.edu.cibertec.reportaya.R
import pe.edu.cibertec.reportaya.data.IncidentDataSourceRegistry
import pe.edu.cibertec.reportaya.data.UserSession
import pe.edu.cibertec.reportaya.data.sync.IncidentSyncManager
import pe.edu.cibertec.reportaya.databinding.ActivityHomeBinding
import pe.edu.cibertec.reportaya.model.IncidentState

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!UserSession.isAuthenticated) {
            goToLogin()
            return
        }
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val greeting = UserSession.displayName.ifBlank { UserSession.email.substringBefore("@") }
        binding.tvGreeting.text = getString(R.string.home_greeting_user, greeting)

        binding.cardMyIncidents.setOnClickListener { startActivity(Intent(this, IncidentListActivity::class.java)) }
        binding.cardNewIncident.setOnClickListener { startActivity(Intent(this, RegisterIncidentActivity::class.java)) }
        binding.cardPendingOperations.setOnClickListener { startActivity(Intent(this, PendingOperationsActivity::class.java)) }
        binding.btnViewIncidents.setOnClickListener { startActivity(Intent(this, IncidentListActivity::class.java)) }
        binding.btnNewIncident.setOnClickListener { startActivity(Intent(this, RegisterIncidentActivity::class.java)) }
        binding.btnLogout.setOnClickListener { goToLogin() }
    }

    override fun onResume() {
        super.onResume()
        if (!::binding.isInitialized) return
        renderSummary()
        lifecycleScope.launch {
            val result = IncidentSyncManager(this@HomeActivity).synchronize()
            renderSummary()
            if (result.authExpired) goToLogin()
        }
    }

    private fun renderSummary() {
        val items = IncidentDataSourceRegistry.current.listIncidents()
        binding.root.findViewById<TextView>(R.id.tvTotalCount).text = items.size.toString()
        binding.root.findViewById<TextView>(R.id.tvPendingCount).text = items.count { it.state == IncidentState.PENDING }.toString()
        binding.root.findViewById<TextView>(R.id.tvInProgressCount).text = items.count { it.state == IncidentState.IN_PROGRESS }.toString()
        binding.root.findViewById<TextView>(R.id.tvAttendedCount).text = items.count { it.state == IncidentState.ATTENDED }.toString()
    }

    private fun goToLogin() {
        UserSession.clear()
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
