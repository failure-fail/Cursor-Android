package fail.failure.cursor.ui.agents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fail.failure.cursor.auth.AuthRepository
import fail.failure.cursor.network.ApiClient
import fail.failure.cursor.network.isUnauthorized
import fail.failure.cursor.network.model.CreateAgentRequest
import fail.failure.cursor.network.model.EnvInput
import fail.failure.cursor.network.model.ImageInput
import fail.failure.cursor.network.model.McpServerInput
import fail.failure.cursor.network.model.ModelInfo
import fail.failure.cursor.network.model.ModelParamInput
import fail.failure.cursor.network.model.ModelSelectionInput
import fail.failure.cursor.network.model.PromptInput
import fail.failure.cursor.network.model.RepoInput
import fail.failure.cursor.network.model.RepositoryInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class KeyValue(val key: String, val value: String)

data class NewAgentUiState(
    val repositories: List<RepositoryInfo> = emptyList(),
    val models: List<ModelInfo> = emptyList(),
    val selectedRepo: RepositoryInfo? = null,
    val selectedModel: ModelInfo? = null,
    val selectedModelParams: Map<String, String> = emptyMap(),
    val prompt: String = "",
    val images: List<ImageInput> = emptyList(),
    val envTarget: String = EnvInput.TYPE_CLOUD,
    val keepMachineAwake: Boolean = false,
    val envVars: List<KeyValue> = emptyList(),
    val mcpServers: List<KeyValue> = emptyList(),
    val showAdvanced: Boolean = false,
    val repoQuery: String = "",
    val isLoadingOptions: Boolean = true,
    val isSubmitting: Boolean = false,
    val createdAgentId: String? = null,
    val needsApiKey: Boolean = false,
    val error: String? = null,
) {
    val filteredRepositories: List<RepositoryInfo>
        get() = if (repoQuery.isBlank()) {
            repositories
        } else {
            repositories.filter {
                (it.fullName ?: "${it.owner}/${it.repo}").contains(repoQuery, ignoreCase = true)
            }
        }
}

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
                    needsApiKey = e.isUnauthorized(),
                    error = if (e.isUnauthorized()) null else e.message ?: "Failed to load repositories/models",
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

    fun updateRepoQuery(query: String) {
        _uiState.value = _uiState.value.copy(repoQuery = query)
    }

    fun selectRepo(repo: RepositoryInfo) {
        _uiState.value = _uiState.value.copy(selectedRepo = repo)
    }

    fun selectModel(model: ModelInfo) {
        _uiState.value = _uiState.value.copy(selectedModel = model, selectedModelParams = emptyMap())
    }

    fun selectModelParam(paramName: String, value: String) {
        _uiState.value = _uiState.value.copy(
            selectedModelParams = _uiState.value.selectedModelParams + (paramName to value),
        )
    }

    fun selectEnvTarget(target: String) {
        _uiState.value = _uiState.value.copy(envTarget = target)
    }

    fun setKeepMachineAwake(awake: Boolean) {
        _uiState.value = _uiState.value.copy(keepMachineAwake = awake)
    }

    fun toggleAdvanced() {
        _uiState.value = _uiState.value.copy(showAdvanced = !_uiState.value.showAdvanced)
    }

    fun addImage(image: ImageInput) {
        val current = _uiState.value.images
        if (current.size >= 5) return
        _uiState.value = _uiState.value.copy(images = current + image)
    }

    fun removeImageAt(index: Int) {
        _uiState.value = _uiState.value.copy(images = _uiState.value.images.filterIndexed { i, _ -> i != index })
    }

    fun addEnvVar() {
        _uiState.value = _uiState.value.copy(envVars = _uiState.value.envVars + KeyValue("", ""))
    }

    fun updateEnvVar(index: Int, key: String, value: String) {
        val updated = _uiState.value.envVars.toMutableList()
        if (index in updated.indices) updated[index] = KeyValue(key, value)
        _uiState.value = _uiState.value.copy(envVars = updated)
    }

    fun removeEnvVarAt(index: Int) {
        _uiState.value = _uiState.value.copy(envVars = _uiState.value.envVars.filterIndexed { i, _ -> i != index })
    }

    fun addMcpServer() {
        _uiState.value = _uiState.value.copy(mcpServers = _uiState.value.mcpServers + KeyValue("", ""))
    }

    fun updateMcpServer(index: Int, name: String, url: String) {
        val updated = _uiState.value.mcpServers.toMutableList()
        if (index in updated.indices) updated[index] = KeyValue(name, url)
        _uiState.value = _uiState.value.copy(mcpServers = updated)
    }

    fun removeMcpServerAt(index: Int) {
        _uiState.value = _uiState.value.copy(mcpServers = _uiState.value.mcpServers.filterIndexed { i, _ -> i != index })
    }

    fun submit() {
        val state = _uiState.value
        if (state.prompt.isBlank() || state.isSubmitting) return
        viewModelScope.launch {
            _uiState.value = state.copy(isSubmitting = true, needsApiKey = false, error = null)
            try {
                val request = CreateAgentRequest(
                    prompt = PromptInput(
                        text = state.prompt,
                        images = state.images.ifEmpty { null },
                    ),
                    model = state.selectedModel?.let {
                        ModelSelectionInput(
                            id = it.id,
                            params = state.selectedModelParams.ifEmpty { null }
                                ?.map { (name, value) -> ModelParamInput(id = name, value = value) },
                        )
                    },
                    repos = state.selectedRepo?.let {
                        listOf(RepoInput(url = "https://github.com/${it.owner}/${it.repo}"))
                    },
                    autoCreatePr = true,
                    env = EnvInput(
                        type = state.envTarget,
                        keepAwake = if (state.envTarget == EnvInput.TYPE_MACHINE) state.keepMachineAwake else null,
                    ),
                    envVars = state.envVars.filter { it.key.isNotBlank() }
                        .associate { it.key to it.value }
                        .ifEmpty { null },
                    mcpServers = state.mcpServers.filter { it.key.isNotBlank() && it.value.isNotBlank() }
                        .map { McpServerInput(name = it.key, url = it.value) }
                        .ifEmpty { null },
                )
                val response = apiClient.service.createAgent(request)
                _uiState.value = _uiState.value.copy(isSubmitting = false, createdAgentId = response.agent.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    needsApiKey = e.isUnauthorized(),
                    error = if (e.isUnauthorized()) null else e.message ?: "Failed to create agent",
                )
            }
        }
    }
}
