package fail.failure.cursor.ui.agents

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fail.failure.cursor.network.ApiClient
import fail.failure.cursor.network.model.Agent
import fail.failure.cursor.network.model.CreateRunRequest
import fail.failure.cursor.network.model.GitInfo
import fail.failure.cursor.network.model.PromptInput
import fail.failure.cursor.network.model.RunEvent
import fail.failure.cursor.notification.Notifications
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
    val currentRunId: String? = null,
    val runStatus: String? = null,
    val gitInfo: GitInfo? = null,
    val transcript: List<TranscriptLine> = emptyList(),
    val isLoading: Boolean = true,
    val isFollowUpSending: Boolean = false,
    val isActionRunning: Boolean = false,
    val error: String? = null,
)

class AgentDetailViewModel(
    private val apiClient: ApiClient,
    private val agentId: String,
    private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AgentDetailUiState())
    val uiState: StateFlow<AgentDetailUiState> = _uiState.asStateFlow()

    private var streamJob: Job? = null
    private var assistantDeltaCount = 0

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
        _uiState.value = _uiState.value.copy(currentRunId = runId)
        val assistantBuffer = StringBuilder()
        streamJob = viewModelScope.launch {
            apiClient.sseClient.stream(apiClient.runStreamUrl(agentId, runId)).collect { event ->
                val current = _uiState.value
                when (event) {
                    is RunEvent.Status -> {
                        _uiState.value = current.copy(runStatus = event.status)
                        notifyProgress(current.agent?.name, event.status, null)
                    }
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
                        assistantDeltaCount++
                        if (assistantDeltaCount % 20 == 0) {
                            notifyProgress(current.agent?.name, current.runStatus, assistantBuffer.toString())
                        }
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
                        notifyProgress(current.agent?.name, event.status, null)
                        fetchGitInfo(runId)
                    }
                    is RunEvent.Error -> _uiState.value = current.copy(error = event.message)
                    RunEvent.Done, RunEvent.Heartbeat -> Unit
                    is RunEvent.Unknown -> Unit
                }
            }
        }
    }

    private fun notifyProgress(agentName: String?, status: String?, latestText: String?) {
        Notifications.showRunProgress(appContext, agentId, agentName ?: agentId, status, latestText)
    }

    private fun fetchGitInfo(runId: String) {
        viewModelScope.launch {
            try {
                val run = apiClient.service.getRun(agentId, runId)
                if (run.git != null) {
                    _uiState.value = _uiState.value.copy(gitInfo = run.git)
                }
            } catch (_: Exception) {
                // Best-effort; the transcript already reflects the terminal status.
            }
        }
    }

    fun cancelRun() {
        val runId = _uiState.value.currentRunId ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionRunning = true)
            try {
                apiClient.service.cancelRun(agentId, runId)
                _uiState.value = _uiState.value.copy(isActionRunning = false, runStatus = "cancelled")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isActionRunning = false, error = e.message ?: "Failed to cancel")
            }
        }
    }

    fun archive() = runAgentAction { apiClient.service.archiveAgent(agentId) }

    fun unarchive() = runAgentAction { apiClient.service.unarchiveAgent(agentId) }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionRunning = true)
            try {
                apiClient.service.deleteAgent(agentId)
                onDeleted()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isActionRunning = false, error = e.message ?: "Failed to delete")
            }
        }
    }

    private fun runAgentAction(block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionRunning = true)
            try {
                block()
                _uiState.value = _uiState.value.copy(isActionRunning = false)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isActionRunning = false, error = e.message ?: "Action failed")
            }
        }
    }

    fun sendFollowUp(prompt: String) {
        if (prompt.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFollowUpSending = true)
            try {
                val response = apiClient.service.createRun(agentId, CreateRunRequest(PromptInput(prompt)))
                _uiState.value = _uiState.value.copy(isFollowUpSending = false)
                startStreaming(response.run.id)
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
