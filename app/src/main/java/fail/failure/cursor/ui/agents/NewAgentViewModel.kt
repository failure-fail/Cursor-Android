package fail.failure.cursor.ui.agents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fail.failure.cursor.network.ApiClient
import fail.failure.cursor.network.model.CreateAgentRequest
import fail.failure.cursor.network.model.ModelInfo
import fail.failure.cursor.network.model.PromptInput
import fail.failure.cursor.network.model.RepoInput
import fail.failure.cursor.network.model.RepositoryInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NewAgentUiState(
    val repositories: List<RepositoryInfo> = emptyList(),
    val models: List<ModelInfo> = emptyList(),
    val selectedRepo: RepositoryInfo? = null,
    val selectedModel: ModelInfo? = null,
    val prompt: String = "",
    val isLoadingOptions: Boolean = true,
    val isSubmitting: Boolean = false,
    val createdAgentId: String? = null,
    val error: String? = null,
)

class NewAgentViewModel(private val apiClient: ApiClient) : ViewModel() {

    private val _uiState = MutableStateFlow(NewAgentUiState())
    val uiState: StateFlow<NewAgentUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val repos = apiClient.service.repositories().repositories
                val models = apiClient.service.models().models
                _uiState.value = _uiState.value.copy(
                    repositories = repos,
                    models = models,
                    selectedRepo = repos.firstOrNull(),
                    selectedModel = models.firstOrNull(),
                    isLoadingOptions = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingOptions = false,
                    error = e.message ?: "Failed to load repositories/models",
                )
            }
        }
    }

    fun updatePrompt(text: String) {
        _uiState.value = _uiState.value.copy(prompt = text)
    }

    fun selectRepo(repo: RepositoryInfo) {
        _uiState.value = _uiState.value.copy(selectedRepo = repo)
    }

    fun selectModel(model: ModelInfo) {
        _uiState.value = _uiState.value.copy(selectedModel = model)
    }

    fun submit() {
        val state = _uiState.value
        if (state.prompt.isBlank() || state.isSubmitting) return
        viewModelScope.launch {
            _uiState.value = state.copy(isSubmitting = true, error = null)
            try {
                val request = CreateAgentRequest(
                    prompt = PromptInput(state.prompt),
                    model = state.selectedModel?.id,
                    repos = state.selectedRepo?.let {
                        listOf(RepoInput(owner = it.owner, repo = it.repo))
                    },
                    autoCreatePr = true,
                )
                val response = apiClient.service.createAgent(request)
                _uiState.value = _uiState.value.copy(isSubmitting = false, createdAgentId = response.agent.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    error = e.message ?: "Failed to create agent",
                )
            }
        }
    }
}
