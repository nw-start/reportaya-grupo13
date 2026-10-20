package pe.edu.cibertec.reportaya.ui

import pe.edu.cibertec.reportaya.model.IncidentState
import pe.edu.cibertec.reportaya.model.IncidentUiModel

object IncidentFilter {
    fun apply(
        source: List<IncidentUiModel>,
        query: String,
        state: IncidentState?
    ): List<IncidentUiModel> {
        val normalized = query.trim()
        return source.filter { item ->
            val stateMatches = state == null || item.state == state
            val queryMatches = normalized.isBlank() || listOf(
                item.code,
                item.type,
                item.description,
                item.location,
                item.state.label,
                item.registeredAt
            ).any { it.contains(normalized, ignoreCase = true) }
            stateMatches && queryMatches
        }
    }
}
