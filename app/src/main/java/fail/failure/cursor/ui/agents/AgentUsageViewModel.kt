package fail.failure.cursor.ui.agents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fail.failure.cursor.network.ApiClient
import fail.failure.cursor.network.model.AgentUsageResponse
import fail.failure.cursor.network.model.Artifact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AgentUsageUiState(
    val usage: AgentUsageResponse? = null,
    val artifacts: List<Artifact> = emptyList(),
    val isLoading: Boolean = true,
    val pendingDownloadUrl: String? = null,
    val error: String? = null,
)

class AgentUsageViewModel(private val apiClient: ApiClient, private val agentId: String) : ViewModel() {

    private val _uiState = MutableStateFlow(AgentUsageUiState())
    val uiState: StateFlow<AgentUsageUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val usage = apiClient.service.getUsage(agentId)
                val artifacts = apiClient.service.listArtifacts(agentId).artifacts
                _uiState.value = AgentUsageUiState(usage = usage, artifacts = artifacts, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Failed to load usage")
            }
        }
    }

    /** Resolves the artifact's temporary presigned URL, then surfaces it once for the UI to open. */
    fun downloadArtifact(path: String) {
        viewModelScope.launch {
            try {
                val response = apiClient.service.getArtifactDownloadUrl(agentId, path)
                _uiState.value = _uiState.value.copy(pendingDownloadUrl = response.url)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.message ?: "Failed to resolve download link")
            }
        }
    }

    fun consumeDownloadUrl() {
        _uiState.value = _uiState.value.copy(pendingDownloadUrl = null)
    }
}
