package pe.edu.cibertec.reportaya.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import pe.edu.cibertec.reportaya.R
import pe.edu.cibertec.reportaya.databinding.ItemPendingOperationBinding
import pe.edu.cibertec.reportaya.model.IncidentUiModel
import pe.edu.cibertec.reportaya.model.LocalSyncStatus

class PendingOperationAdapter(
    private val onClick: (IncidentUiModel) -> Unit
) : ListAdapter<IncidentUiModel, PendingOperationAdapter.ViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemPendingOperationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemPendingOperationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: IncidentUiModel) = with(binding) {
            tvPendingCode.text = item.code
            tvPendingType.text = item.type
            tvPendingLocation.text = item.location
            tvPendingOperation.text = item.syncStatus.label
            tvPendingError.text = item.lastSyncError.orEmpty()
            tvPendingError.visibility = if (item.lastSyncError.isNullOrBlank()) android.view.View.GONE else android.view.View.VISIBLE
            val color = when (item.syncStatus) {
                LocalSyncStatus.ERROR -> R.color.state_rejected_fg
                LocalSyncStatus.PENDING_DELETE -> R.color.state_rejected_fg
                else -> R.color.state_pending_fg
            }
            tvPendingOperation.setTextColor(ContextCompat.getColor(root.context, color))
            root.setOnClickListener { if (!item.isDeleted) onClick(item) }
        }
    }

    private object Diff : DiffUtil.ItemCallback<IncidentUiModel>() {
        override fun areItemsTheSame(oldItem: IncidentUiModel, newItem: IncidentUiModel) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: IncidentUiModel, newItem: IncidentUiModel) = oldItem == newItem
    }
}
