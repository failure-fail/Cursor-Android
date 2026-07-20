package fail.failure.cursor.ui.agents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fail.failure.cursor.network.ApiClient
import fail.failure.cursor.network.model.Agent
import fail.failure.cursor.network.model.CreateRunRequest
import fail.failure.cursor.network.model.PromptInput
import fail.failure.cursor.network.model.RunEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** One line of the on-screen transcript for the currently streamed run. */
sealed interface TranscriptLine {
    data class Assistant(val text: String) : TranscriptLine
    data class Thinking(val text: String) : TranscriptLine
    data class Tool(val name: String, val status: String) : TranscriptLine
    data class SystemNote(val text: String) : TranscriptLine
}

data class AgentDetailUiState(
    val agent: Agent? = null,
    val runStatus: String? = null,
    val transcript: List<TranscriptLine> = emptyList(),
    val isLoading: Boolean = true,
    val isFollowUpSending: Boolean = false,
    val error: String? = null,
)

class AgentDetailViewModel(
    private val apiClient: ApiClient,
    private val agentId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgentDetailUiState())
    val uiState: StateFlow<AgentDetailUiState> = _uiState.asStateFlow()

    private var streamJob: Job? = null

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val agent = apiClient.service.getAgent(agentId)
                _uiState.value = _uiState.value.copy(agent = agent, isLoading = false)
                agent.latestRunId?.let { startStreaming(it) }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Failed to load agent")
            }
        }
    }

    private fun startStreaming(runId: String) {
        streamJob?.cancel()
        val assistantBuffer = StringBuilder()
        streamJob = viewModelScope.launch {
            apiClient.sseClient.stream(apiClient.runStreamUrl(agentId, runId)).collect { event ->
                val current = _uiState.value
                when (event) {
                    is RunEvent.Status -> _uiState.value = current.copy(runStatus = event.status)
                    is RunEvent.AssistantDelta -> {
                        assistantBuffer.append(event.text)
                        val lines = current.transcript.toMutableList()
                        val last = lines.lastOrNull()
                        if (last is TranscriptLine.Assistant) {
                            lines[lines.lastIndex] = TranscriptLine.Assistant(assistantBuffer.toString())
                        } else {
                            lines.add(TranscriptLine.Assistant(assistantBuffer.toString()))
                        }
                        _uiState.value = current.copy(transcript = lines)
                    }
                    is RunEvent.ThinkingDelta -> {
                        _uiState.value = current.copy(
                            transcript = current.transcript + TranscriptLine.Thinking(event.text),
                        )
                    }
                    is RunEvent.ToolCall -> {
                        _uiState.value = current.copy(
                            transcript = current.transcript + TranscriptLine.Tool(
                                name = event.name ?: "tool",
                                status = event.status ?: "running",
                            ),
                        )
                        assistantBuffer.clear()
                    }
                    is RunEvent.Result -> {
                        _uiState.value = current.copy(
                            runStatus = event.status,
                            transcript = if (event.text != null) {
                                current.transcript + TranscriptLine.SystemNote("Finished: ${event.status}")
                            } else current.transcript,
                        )
                    }
                    is RunEvent.Error -> _uiState.value = current.copy(error = event.message)
                    RunEvent.Done, RunEvent.Heartbeat -> Unit
                    is RunEvent.Unknown -> Unit
                }
            }
        }
    }

    fun sendFollowUp(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFollowUpSending = true)
            try {
                val run = apiClient.service.createRun(agentId, CreateRunRequest(PromptInput(prompt)))
                _uiState.value = _uiState.value.copy(isFollowUpSending = false)
                startStreaming(run.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isFollowUpSending = false,
                    error = e.message ?: "Failed to send follow-up",
                )
            }
        }
    }

    override fun onCleared() {
        streamJob?.cancel()
        super.onCleared()
    }
}
