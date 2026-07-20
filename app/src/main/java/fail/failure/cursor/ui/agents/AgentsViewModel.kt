package fail.failure.cursor.ui.agents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fail.failure.cursor.agents.PinnedAgentsStore
import fail.failure.cursor.auth.AuthRepository
import fail.failure.cursor.network.ApiClient
import fail.failure.cursor.network.cursorApiErrorMessage
import fail.failure.cursor.network.isUnauthorized
import fail.failure.cursor.network.model.Agent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

enum class AgentSortOrder(val label: String) {
    NEWEST("Newest first"),
    OLDEST("Oldest first"),
    NAME("Name"),
}

data class AgentsUiState(
    val agents: List<Agent> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val needsApiKey: Boolean = false,
    val apiKeyError: String? = null,
    val error: String? = null,
    val searchQuery: String = "",
    val sortOrder: AgentSortOrder = AgentSortOrder.NEWEST,
    val pinnedIds: Set<String> = emptySet(),
) {
    val filteredAgents: List<Agent>
        get() {
            val filtered = agents.filter { agent ->
                searchQuery.isBlank() ||
                    (agent.name ?: agent.id).contains(searchQuery, ignoreCase = true) ||
                    agent.repos?.firstOrNull()?.url?.contains(searchQuery, ignoreCase = true) == true
            }
            val sorted = when (sortOrder) {
                AgentSortOrder.NEWEST -> filtered.sortedByDescending { it.createdAt?.let(::parseInstantOrNull) }
                AgentSortOrder.OLDEST -> filtered.sortedBy { it.createdAt?.let(::parseInstantOrNull) }
                AgentSortOrder.NAME -> filtered.sortedBy { (it.name ?: it.id).lowercase() }
            }
            return sorted.sortedByDescending { it.id in pinnedIds }
        }
}

private fun parseInstantOrNull(iso: String): Instant? = try {
    Instant.parse(iso)
} catch (_: Exception) {
    null
}

class AgentsViewModel(
    private val apiClient: ApiClient,
    private val authRepository: AuthRepository,
    private val pinnedAgentsStore: PinnedAgentsStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgentsUiState(isLoading = true, pinnedIds = pinnedAgentsStore.pinnedIds()))
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
                    error = if (unauthorized) null else e.cursorApiErrorMessage() ?: e.message ?: "Failed to load agents",
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

    fun updateSortOrder(order: AgentSortOrder) {
        _uiState.value = _uiState.value.copy(sortOrder = order)
    }

    fun togglePin(agentId: String) {
        val pinned = agentId !in _uiState.value.pinnedIds
        pinnedAgentsStore.setPinned(agentId, pinned)
        _uiState.value = _uiState.value.copy(pinnedIds = pinnedAgentsStore.pinnedIds())
    }

    /** Optimistically drops [agentId] from the list (the swipe gesture already animated it away)
     * and puts it back if the archive call actually fails, rather than waiting on a round trip
     * before the row disappears. */
    fun archive(agentId: String) {
        val previousAgents = _uiState.value.agents
        _uiState.value = _uiState.value.copy(agents = previousAgents.filterNot { it.id == agentId })
        viewModelScope.launch {
            try {
                apiClient.service.archiveAgent(agentId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    agents = previousAgents,
                    error = e.cursorApiErrorMessage() ?: e.message ?: "Failed to archive",
                )
            }
        }
    }
}
