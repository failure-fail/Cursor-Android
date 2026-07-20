package fail.failure.grok.network.model

/** Parsed form of one Server-Sent Event from GET /v1/agents/{id}/runs/{runId}/stream. */
sealed interface RunEvent {
    data class Status(val runId: String?, val status: String?) : RunEvent
    data class AssistantDelta(val text: String) : RunEvent
    data class ThinkingDelta(val text: String) : RunEvent
    data class ToolCall(
        val callId: String?,
        val name: String?,
        val status: String?,
        val truncated: Boolean?,
    ) : RunEvent
    data class Result(
        val runId: String?,
        val status: String?,
        val text: String?,
        val durationMs: Long?,
    ) : RunEvent
    data object Heartbeat : RunEvent
    data object Done : RunEvent
    data class Error(val message: String?) : RunEvent
    data class Unknown(val type: String, val raw: String) : RunEvent
}
