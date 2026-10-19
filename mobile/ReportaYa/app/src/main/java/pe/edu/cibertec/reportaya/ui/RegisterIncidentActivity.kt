package pe.edu.cibertec.reportaya.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import pe.edu.cibertec.reportaya.R
import pe.edu.cibertec.reportaya.data.IncidentDataSourceRegistry
import pe.edu.cibertec.reportaya.data.UserSession
import pe.edu.cibertec.reportaya.data.sync.IncidentSyncManager
import pe.edu.cibertec.reportaya.databinding.ActivityIncidentFormBinding
import pe.edu.cibertec.reportaya.model.LocalSyncStatus

class RegisterIncidentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityIncidentFormBinding
    private var selectedPhoto: Uri? = null

    private val photoPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedPhoto = uri
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) { }
        }
        binding.ivPhotoPreview.setImageURI(uri)
        binding.ivPhotoPreview.visibility = if (uri == null) View.GONE else View.VISIBLE
        binding.tvPhotoHint.text = if (uri == null) getString(R.string.photo_optional) else getString(R.string.photo_selected)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIncidentFormBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        val types = resources.getStringArray(R.array.incident_types)
        binding.actvType.setAdapter(ArrayAdapter(this, android.R.layout.simple_list_item_1, types))
        binding.btnAddPhoto.setOnClickListener { photoPicker.launch(arrayOf("image/jpeg", "image/png")) }
        binding.btnSave.setOnClickListener { validateAndSubmit() }
    }

    private fun validateAndSubmit() {
        val type = binding.actvType.text?.toString()?.trim().orEmpty()
        val description = binding.etDescription.text?.toString()?.trim().orEmpty()
        val location = binding.etLocation.text?.toString()?.trim().orEmpty()
        val validTypes = resources.getStringArray(R.array.incident_types).toSet()

        binding.tilType.error = if (type !in validTypes) getString(R.string.error_type_required) else null
        binding.tilDescription.error = when {
            description.length < 10 -> getString(R.string.error_description_min)
            description.length > 250 -> getString(R.string.error_description_max)
            else -> null
        }
        binding.tilLocation.error = if (location.isBlank()) getString(R.string.error_location_required) else null
        if (type !in validTypes || description.length !in 10..250 || location.isBlank()) return

        binding.btnSave.isEnabled = false
        val created = IncidentDataSourceRegistry.current.createIncident(
            type = type,
            description = description,
            location = location,
            photoUri = selectedPhoto?.toString(),
            user = UserSession.email
        )

        lifecycleScope.launch {
            IncidentSyncManager(this@RegisterIncidentActivity).synchronize()
            val current = IncidentDataSourceRegistry.current.findIncidentById(created.id) ?: created
            val message = if (current.syncStatus == LocalSyncStatus.SYNCED) {
                getString(R.string.incident_registered_synced, current.code)
            } else {
                getString(R.string.incident_registered_pending, current.code)
            }
            MaterialAlertDialogBuilder(this@RegisterIncidentActivity)
                .setTitle(R.string.incident_registered_title)
                .setMessage(message)
                .setCancelable(false)
                .setNegativeButton(R.string.go_to_list) { _, _ ->
                    startActivity(Intent(this@RegisterIncidentActivity, IncidentListActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    })
                    finish()
                }
                .setPositiveButton(R.string.view_detail) { _, _ ->
                    startActivity(Intent(this@RegisterIncidentActivity, IncidentDetailActivity::class.java).apply {
                        putExtra(IncidentListActivity.EXTRA_INCIDENT_ID, current.id)
                    })
                    finish()
                }
                .show()
            binding.btnSave.isEnabled = true
        }
    }
}
