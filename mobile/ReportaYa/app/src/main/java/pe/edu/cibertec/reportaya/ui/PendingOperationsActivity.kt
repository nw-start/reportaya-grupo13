package pe.edu.cibertec.reportaya.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.launch
import pe.edu.cibertec.reportaya.R
import pe.edu.cibertec.reportaya.data.IncidentDataSourceRegistry
import pe.edu.cibertec.reportaya.data.UserSession
import pe.edu.cibertec.reportaya.data.sync.IncidentSyncManager
import pe.edu.cibertec.reportaya.databinding.ActivityPendingOperationsBinding

class PendingOperationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPendingOperationsBinding
    private lateinit var adapter: PendingOperationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPendingOperationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }
        adapter = PendingOperationAdapter { incident ->
            startActivity(Intent(this, IncidentDetailActivity::class.java).apply {
                putExtra(IncidentListActivity.EXTRA_INCIDENT_ID, incident.id)
            })
        }
        binding.rvPendingOperations.layoutManager = LinearLayoutManager(this)
        binding.rvPendingOperations.adapter = adapter
        binding.btnSyncNow.setOnClickListener { synchronizeNow() }
        binding.btnCreateIncident.setOnClickListener {
            startActivity(Intent(this, RegisterIncidentActivity::class.java))
        }
        binding.btnBackToHome.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            })
        }
    }

    override fun onResume() {
        super.onResume()
        renderPending()
    }

    private fun synchronizeNow() {
        if (!UserSession.isAuthenticated) {
            goToLogin()
            return
        }
        setSyncing(true)
        lifecycleScope.launch {
            val result = IncidentSyncManager(this@PendingOperationsActivity).synchronize()
            setSyncing(false)
            binding.tvSyncResult.visibility = View.VISIBLE
            binding.tvSyncResult.text = result.message
            renderPending()
            if (result.authExpired) goToLogin()
        }
    }

    private fun setSyncing(syncing: Boolean) {
        binding.btnSyncNow.isEnabled = !syncing
        binding.syncProgress.visibility = if (syncing) View.VISIBLE else View.GONE
        binding.btnSyncNow.text = getString(if (syncing) R.string.syncing else R.string.sync_now)
    }

    private fun renderPending() {
        val pending = IncidentDataSourceRegistry.current.listPendingOperations()
        adapter.submitList(pending)
        binding.tvPendingCount.text = resources.getQuantityString(
            R.plurals.pending_operations_count,
            pending.size,
            pending.size
        )
        val empty = pending.isEmpty()
        binding.emptyPendingState.visibility = if (empty) View.VISIBLE else View.GONE
        binding.rvPendingOperations.visibility = if (empty) View.GONE else View.VISIBLE
    }

    private fun goToLogin() {
        UserSession.clear()
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}
