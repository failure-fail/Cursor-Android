package fail.failure.grok.ui.agents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fail.failure.grok.auth.AuthRepository
import fail.failure.grok.network.ApiClient
import fail.failure.grok.network.grokApiErrorMessage
import fail.failure.grok.network.isUnauthorized
import fail.failure.grok.network.model.CreateAgentRequest
import fail.failure.grok.network.model.ModelInfo
import fail.failure.grok.network.model.ModelSelectionInput
import fail.failure.grok.network.model.PromptInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NewAgentUiState(
    val models: List<ModelInfo> = emptyList(),
    val selectedModel: ModelInfo? = null,
    val prompt: String = "",
    val isLoadingOptions: Boolean = true,
    val isSubmitting: Boolean = false,
    val createdAgentId: String? = null,
    val needsApiKey: Boolean = false,
    val apiKeyError: String? = null,
    val error: String? = null,
)

class NewAgentViewModel(
    private val apiClient: ApiClient,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewAgentUiState())
    val uiState: StateFlow<NewAgentUiState> = _uiState.asStateFlow()

    init {
        loadOptions()
    }

    private fun loadOptions() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingOptions = true, needsApiKey = false, error = null)
            try {
                val models = apiClient.service.models().models
                _uiState.value = _uiState.value.copy(
                    models = models,
                    selectedModel = models.firstOrNull(),
                    isLoadingOptions = false,
                )
            } catch (e: Exception) {
                val unauthorized = e.isUnauthorized()
                val triedAKey = !authRepository.session.value.apiKey.isNullOrBlank()
                _uiState.value = _uiState.value.copy(
                    isLoadingOptions = false,
                    needsApiKey = unauthorized,
                    apiKeyError = if (unauthorized && triedAKey) {
                        e.grokApiErrorMessage() ?: "That key wasn't accepted. Double-check it and try again."
                    } else {
                        null
                    },
                    error = if (unauthorized) {
                        null
                    } else {
                        e.grokApiErrorMessage() ?: e.message ?: "Failed to load models"
                    },
                )
            }
        }
    }

    fun signInWithApiKey(apiKey: String) {
        authRepository.signInWithApiKey(apiKey)
        loadOptions()
    }

    fun updatePrompt(text: String) {
        _uiState.value = _uiState.value.copy(prompt = text)
    }

    fun selectModel(model: ModelInfo) {
        _uiState.value = _uiState.value.copy(selectedModel = model)
    }

    fun submit() {
        val state = _uiState.value
        if (state.prompt.isBlank() || state.isSubmitting) return
        viewModelScope.launch {
            _uiState.value = state.copy(isSubmitting = true, needsApiKey = false, error = null)
            try {
                val response = apiClient.service.createAgent(
                    CreateAgentRequest(
                        prompt = PromptInput(text = state.prompt),
                        model = state.selectedModel?.let { ModelSelectionInput(id = it.id) },
                    ),
                )
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    createdAgentId = response.agent.id,
                )
            } catch (e: Exception) {
                val unauthorized = e.isUnauthorized()
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    needsApiKey = unauthorized,
                    apiKeyError = if (unauthorized) e.grokApiErrorMessage() else null,
                    error = if (unauthorized) {
                        null
                    } else {
                        e.grokApiErrorMessage() ?: e.message ?: "Failed to start chat"
                    },
                )
            }
        }
    }
}
