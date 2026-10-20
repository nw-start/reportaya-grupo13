package pe.edu.cibertec.reportaya.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import pe.edu.cibertec.reportaya.R
import pe.edu.cibertec.reportaya.databinding.ItemIncidentBinding
import pe.edu.cibertec.reportaya.model.IncidentState
import pe.edu.cibertec.reportaya.model.IncidentUiModel

class IncidentAdapter(
    private val onItemClick: (IncidentUiModel) -> Unit
) : ListAdapter<IncidentUiModel, IncidentAdapter.IncidentViewHolder>(DiffCallback) {

    inner class IncidentViewHolder(
        private val binding: ItemIncidentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: IncidentUiModel) = with(binding) {
            tvCode.text = item.code
            tvType.text = item.type
            tvDescription.text = item.description
            tvLocation.text = item.location
            tvDate.text = item.registeredAt
            tvState.text = item.state.label
            val (badgeBackground, badgeTextColor) = when (item.state) {
                IncidentState.PENDING -> R.drawable.badge_pending to R.color.state_pending_fg
                IncidentState.IN_PROGRESS -> R.drawable.badge_in_progress to R.color.state_progress_fg
                IncidentState.ATTENDED -> R.drawable.badge_attended to R.color.state_attended_fg
                IncidentState.REJECTED -> R.drawable.badge_rejected to R.color.state_rejected_fg
            }
            tvState.setBackgroundResource(badgeBackground)
            tvState.setTextColor(ContextCompat.getColor(root.context, badgeTextColor))
            ivPhotoIndicator.setImageResource(
                if (item.photoUri.isNullOrBlank()) R.drawable.ic_no_photo else R.drawable.ic_photo
            )
            root.contentDescription = root.context.getString(
                R.string.cd_incident_card,
                item.code,
                item.type,
                item.state.label
            )
            root.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncidentViewHolder {
        val binding = ItemIncidentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return IncidentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IncidentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private object DiffCallback : DiffUtil.ItemCallback<IncidentUiModel>() {
        override fun areItemsTheSame(oldItem: IncidentUiModel, newItem: IncidentUiModel) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: IncidentUiModel, newItem: IncidentUiModel) =
            oldItem == newItem
    }
}
