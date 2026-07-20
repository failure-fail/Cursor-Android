package fail.failure.cursor.ui.agents

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fail.failure.cursor.agents.TranscriptLineDto
import fail.failure.cursor.agents.TranscriptStore
import fail.failure.cursor.network.ApiClient
import fail.failure.cursor.network.cursorApiErrorMessage
import fail.failure.cursor.network.model.Agent
import fail.failure.cursor.network.model.CreateRunRequest
import fail.failure.cursor.network.model.GitInfo
import fail.failure.cursor.network.model.PromptInput
import fail.failure.cursor.network.model.Run
import fail.failure.cursor.network.model.RunEvent
import fail.failure.cursor.notification.Notifications
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

/** Prefers the server's own error message (e.g. Cursor's `{"error":{"message":...}}` body) over a
 * raw "HTTP 409"-style string, with a 409-specific fallback for the one case actually seen in
 * practice: trying to send a follow-up while a run is still active. */
private fun Throwable.friendlyMessage(fallback: String): String {
    cursorApiErrorMessage()?.let { return it }
    if (this is HttpException && code() == 409) {
        return "This agent already has an active run - wait for it to finish before sending a follow-up."
    }
    return message ?: fallback
}

/** One line of the on-screen transcript for the currently streamed run. */
sealed interface TranscriptLine {
    data class Assistant(val text: String) : TranscriptLine
    data class Thinking(val text: String) : TranscriptLine
    data class Tool(val name: String, val status: String) : TranscriptLine
    data class SystemNote(val text: String) : TranscriptLine
}

private fun TranscriptLine.toDto(): TranscriptLineDto = when (this) {
    is TranscriptLine.Assistant -> TranscriptLineDto.Assistant(text)
    is TranscriptLine.Thinking -> TranscriptLineDto.Thinking(text)
    is TranscriptLine.Tool -> TranscriptLineDto.Tool(name, status)
    is TranscriptLine.SystemNote -> TranscriptLineDto.SystemNote(text)
}

private fun TranscriptLineDto.toDomain(): TranscriptLine = when (this) {
    is TranscriptLineDto.Assistant -> TranscriptLine.Assistant(text)
    is TranscriptLineDto.Thinking -> TranscriptLine.Thinking(text)
    is TranscriptLineDto.Tool -> TranscriptLine.Tool(name, status)
    is TranscriptLineDto.SystemNote -> TranscriptLine.SystemNote(text)
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
    val runHistory: List<Run> = emptyList(),
    val isLoadingHistory: Boolean = false,
)

class AgentDetailViewModel(
    private val apiClient: ApiClient,
    private val agentId: String,
    private val appContext: Context,
    private val transcriptStore: TranscriptStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AgentDetailUiState(transcript = transcriptStore.load(agentId).map { it.toDomain() }),
    )
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
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.friendlyMessage("Failed to load agent"))
            }
        }
    }

    private fun persist() {
        transcriptStore.save(agentId, _uiState.value.transcript.map { it.toDto() })
    }

    private fun startStreaming(runId: String) {
        streamJob?.cancel()
        _uiState.value = _uiState.value.copy(currentRunId = runId, error = null)
        val assistantBuffer = StringBuilder()
        // Guards against merging this run's first assistant delta into a *previous* run's bubble
        // that happens to be the last transcript entry (e.g. restored from local history, or left
        // over from before this follow-up was sent) - without it, two unrelated runs' text could
        // get concatenated into one bubble.
        var startedNewAssistantBubble = false
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
                        if (startedNewAssistantBubble && last is TranscriptLine.Assistant) {
                            lines[lines.lastIndex] = TranscriptLine.Assistant(assistantBuffer.toString())
                        } else {
                            lines.add(TranscriptLine.Assistant(assistantBuffer.toString()))
                            startedNewAssistantBubble = true
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
                        startedNewAssistantBubble = false
                    }
                    is RunEvent.Result -> {
                        _uiState.value = current.copy(
                            runStatus = event.status,
                            transcript = if (event.text != null) {
                                current.transcript + TranscriptLine.SystemNote("Finished: ${event.status}")
                            } else {
                                current.transcript
                            },
                        )
                        notifyProgress(current.agent?.name, event.status, null)
                        fetchGitInfo(runId)
                    }
                    is RunEvent.Error -> {
                        // A run that finishes very fast can close its stream before this app ever
                        // attaches to it - Cursor's own docs confirm a stream scoped to an
                        // already-finished run won't replay anything, and in practice the server
                        // answers with a flat "stream is no longer available" rather than the
                        // final status. Fetching the run directly recovers from that instead of
                        // leaving a stale status badge next to a dead error.
                        fallbackToRunStatus(runId, event.message)
                    }
                    RunEvent.Done, RunEvent.Heartbeat -> Unit
                    is RunEvent.Unknown -> Unit
                }
                persist()
            }
        }
    }

    private fun fallbackToRunStatus(runId: String, streamError: String?) {
        viewModelScope.launch {
            try {
                val run = apiClient.service.getRun(agentId, runId)
                val current = _uiState.value
                val alreadyHasResult = run.result != null &&
                    current.transcript.any { it is TranscriptLine.Assistant && it.text == run.result }
                _uiState.value = current.copy(
                    runStatus = run.status,
                    error = null,
                    transcript = if (run.result != null && !alreadyHasResult) {
                        current.transcript + TranscriptLine.Assistant(run.result)
                    } else {
                        current.transcript
                    },
                )
                persist()
                if (run.git != null) {
                    _uiState.value = _uiState.value.copy(gitInfo = run.git)
                }
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(error = streamError ?: "Run stream is no longer available")
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

    fun loadRunHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingHistory = true)
            try {
                val runs = apiClient.service.listRuns(agentId).runs.sortedByDescending { it.createdAt }
                _uiState.value = _uiState.value.copy(runHistory = runs, isLoadingHistory = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingHistory = false,
                    error = e.friendlyMessage("Failed to load run history"),
                )
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
                _uiState.value = _uiState.value.copy(isActionRunning = false, error = e.friendlyMessage("Failed to cancel"))
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
                _uiState.value = _uiState.value.copy(isActionRunning = false, error = e.friendlyMessage("Failed to delete"))
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
                _uiState.value = _uiState.value.copy(isActionRunning = false, error = e.friendlyMessage("Action failed"))
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
                    error = e.friendlyMessage("Failed to send follow-up"),
                )
            }
        }
    }

    override fun onCleared() {
        streamJob?.cancel()
        super.onCleared()
    }
}
