package pe.edu.cibertec.reportaya.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import pe.edu.cibertec.reportaya.R
import pe.edu.cibertec.reportaya.data.IncidentDataSourceRegistry
import pe.edu.cibertec.reportaya.data.sync.IncidentSyncManager
import pe.edu.cibertec.reportaya.databinding.ActivityIncidentEditBinding
import pe.edu.cibertec.reportaya.model.IncidentState
import pe.edu.cibertec.reportaya.model.LocalSyncStatus

class IncidentEditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityIncidentEditBinding
    private var incidentId: Long = -1
    private var selectedPhoto: Uri? = null

    private val photoPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedPhoto = uri
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: SecurityException) { }
        }
        renderPhoto()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIncidentEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        incidentId = intent.getLongExtra(IncidentListActivity.EXTRA_INCIDENT_ID, -1)
        val types = resources.getStringArray(R.array.incident_types)
        binding.actvType.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, types))
        binding.btnChangePhoto.setOnClickListener { photoPicker.launch(arrayOf("image/jpeg", "image/png")) }
        binding.btnSaveChanges.setOnClickListener { validateAndSubmit() }
        populate()
    }

    private fun populate() {
        val item = IncidentDataSourceRegistry.current.findIncidentById(incidentId)
        if (item == null) {
            Snackbar.make(binding.root, R.string.incident_not_found, Snackbar.LENGTH_LONG).setAction(R.string.close) { finish() }.show()
            binding.btnSaveChanges.isEnabled = false
            return
        }
        binding.tvCode.text = item.code
        binding.tvState.text = item.state.label
        binding.actvType.setText(item.type, false)
        binding.etDescription.setText(item.description)
        binding.etLocation.setText(item.location)
        selectedPhoto = item.photoUri?.takeIf { it.isNotBlank() }?.let(Uri::parse)
        renderPhoto()
        val editable = item.state == IncidentState.PENDING
        binding.actvType.isEnabled = editable
        binding.etDescription.isEnabled = editable
        binding.etLocation.isEnabled = editable
        binding.btnChangePhoto.isEnabled = editable
        binding.btnSaveChanges.isEnabled = editable
        if (!editable) Snackbar.make(binding.root, R.string.only_pending_editable, Snackbar.LENGTH_LONG).show()
    }

    private fun renderPhoto() {
        val uri = selectedPhoto
        binding.ivPhotoPreview.visibility = if (uri == null) View.GONE else View.VISIBLE
        if (uri != null) binding.ivPhotoPreview.setImageURI(uri)
    }

    private fun validateAndSubmit() {
        val type = binding.actvType.text?.toString()?.trim().orEmpty()
        val description = binding.etDescription.text?.toString()?.trim().orEmpty()
        val location = binding.etLocation.text?.toString()?.trim().orEmpty()
        val validTypes = resources.getStringArray(R.array.incident_types).toSet()
        binding.tilType.error = if (type !in validTypes) getString(R.string.error_type_required) else null
        binding.tilDescription.error = if (description.length !in 10..250) getString(R.string.error_description_range) else null
        binding.tilLocation.error = if (location.isBlank()) getString(R.string.error_location_required) else null
        if (type !in validTypes || description.length !in 10..250 || location.isBlank()) return

        val updated = IncidentDataSourceRegistry.current.updateIncident(
            incidentId, type, description, location, selectedPhoto?.toString()
        ) ?: run {
            Snackbar.make(binding.root, R.string.update_not_allowed, Snackbar.LENGTH_LONG).show()
            return
        }
        binding.btnSaveChanges.isEnabled = false
        lifecycleScope.launch {
            IncidentSyncManager(this@IncidentEditActivity).synchronize()
            val current = IncidentDataSourceRegistry.current.findIncidentById(updated.id)
            val text = if (current?.syncStatus == LocalSyncStatus.SYNCED) {
                getString(R.string.incident_updated)
            } else {
                getString(R.string.incident_updated) + ". Pendiente de sincronización."
            }
            Snackbar.make(binding.root, text, Snackbar.LENGTH_SHORT)
                .addCallback(object : Snackbar.Callback() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) { finish() }
                }).show()
        }
    }
}
