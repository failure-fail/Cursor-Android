package fail.failure.cursor.ui.agents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fail.failure.cursor.auth.AuthRepository
import fail.failure.cursor.network.ApiClient
import fail.failure.cursor.network.cursorApiErrorMessage
import fail.failure.cursor.network.isUnauthorized
import fail.failure.cursor.network.model.Agent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AgentsUiState(
    val agents: List<Agent> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val needsApiKey: Boolean = false,
    val apiKeyError: String? = null,
    val error: String? = null,
    val searchQuery: String = "",
    /** Lowercase status, or null for "All". */
    val statusFilter: String? = null,
) {
    val filteredAgents: List<Agent>
        get() = agents.filter { agent ->
            val matchesQuery = searchQuery.isBlank() ||
                (agent.name ?: agent.id).contains(searchQuery, ignoreCase = true) ||
                agent.repos?.firstOrNull()?.url?.contains(searchQuery, ignoreCase = true) == true
            val matchesStatus = statusFilter == null || agent.status?.lowercase() == statusFilter
            matchesQuery && matchesStatus
        }
}

class AgentsViewModel(
    private val apiClient: ApiClient,
    private val authRepository: AuthRepository,
) : ViewModel() {

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
                needsApiKey = false,
                error = null,
            )
            try {
                val response = apiClient.service.listAgents()
                _uiState.value = _uiState.value.copy(
                    agents = response.agents,
                    isLoading = false,
                    isRefreshing = false,
                )
            } catch (e: Exception) {
                val unauthorized = e.isUnauthorized()
                val triedAKey = !authRepository.session.value.apiKey.isNullOrBlank()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    needsApiKey = unauthorized,
                    apiKeyError = if (unauthorized && triedAKey) {
                        e.cursorApiErrorMessage() ?: "That key wasn't accepted. Double-check it and try again."
                    } else {
                        null
                    },
                    error = if (unauthorized) null else e.message ?: "Failed to load agents",
                )
            }
        }
    }

    fun signInWithApiKey(apiKey: String) {
        authRepository.signInWithApiKey(apiKey)
        refresh()
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun updateStatusFilter(status: String?) {
        _uiState.value = _uiState.value.copy(statusFilter = status)
    }
}
