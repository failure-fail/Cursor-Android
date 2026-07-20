package fail.failure.cursor.ui.agents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fail.failure.cursor.network.ApiClient
import fail.failure.cursor.network.model.Agent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AgentsUiState(
    val agents: List<Agent> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

class AgentsViewModel(private val apiClient: ApiClient) : ViewModel() {

    private val _uiState = MutableStateFlow(AgentsUiState(isLoading = true))
    val uiState: StateFlow<AgentsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val hadAgentsAlready = _uiState.value.agents.isNotEmpty()
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = !hadAgentsAlready,
                isRefreshing = hadAgentsAlready,
                error = null,
            )
            try {
                val response = apiClient.service.listAgents()
                _uiState.value = AgentsUiState(agents = response.agents, isLoading = false, isRefreshing = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = e.message ?: "Failed to load agents",
                )
            }
        }
    }
}
