package fail.failure.grok.network

import fail.failure.grok.network.model.RunEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader

/**
 * Minimal Server-Sent-Events reader for GET /v1/agents/{id}/runs/{runId}/stream. No external
 * SSE library is pulled in since the format is just repeated `event:`/`data:` lines separated
 * by a blank line - trivial to parse directly off the OkHttp response body.
 */
class SseClient(private val httpClient: OkHttpClient) {

    private val json = Json { ignoreUnknownKeys = true }

    fun stream(url: String, lastEventId: String? = null): Flow<RunEvent> = callbackFlow {
        val requestBuilder = Request.Builder()
            .url(url)
            .header("Accept", "text/event-stream")
        if (!lastEventId.isNullOrBlank()) {
            requestBuilder.header("Last-Event-ID", lastEventId)
        }
        val call = httpClient.newCall(requestBuilder.build())

        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    trySend(RunEvent.Error("HTTP ${response.code}"))
                    close()
                    return@callbackFlow
                }
                val reader: BufferedReader = response.body?.charStream()?.buffered()
                    ?: run { close(); return@callbackFlow }

                var eventType = "message"
                val dataBuilder = StringBuilder()

                fun dispatch() {
                    if (dataBuilder.isNotEmpty()) {
                        trySend(parseEvent(eventType, dataBuilder.toString()))
                    }
                    eventType = "message"
                    dataBuilder.clear()
                }

                var line: String?
                while (true) {
                    line = reader.readLine() ?: break
                    when {
                        line.isEmpty() -> dispatch()
                        line.startsWith("event:") -> eventType = line.removePrefix("event:").trim()
                        line.startsWith("data:") -> {
                            if (dataBuilder.isNotEmpty()) dataBuilder.append('\n')
                            dataBuilder.append(line.removePrefix("data:").trim())
                        }
                        line.startsWith(":") -> Unit // comment/keepalive
                    }
                }
                dispatch()
                trySend(RunEvent.Done)
            }
        } catch (e: Exception) {
            trySend(RunEvent.Error(e.message))
        }

        close()
        awaitClose { call.cancel() }
    }.flowOn(Dispatchers.IO)

    private fun parseEvent(type: String, data: String): RunEvent {
        return try {
            val obj = json.parseToJsonElement(data) as? JsonObject
            when (type) {
                "status" -> RunEvent.Status(
                    runId = obj?.get("runId")?.jsonPrimitive?.content,
                    status = obj?.get("status")?.jsonPrimitive?.content,
                )
                "assistant" -> RunEvent.AssistantDelta(obj?.get("text")?.jsonPrimitive?.content.orEmpty())
                "thinking" -> RunEvent.ThinkingDelta(obj?.get("text")?.jsonPrimitive?.content.orEmpty())
                "tool_call" -> RunEvent.ToolCall(
                    callId = obj?.get("callId")?.jsonPrimitive?.content,
                    name = obj?.get("name")?.jsonPrimitive?.content,
                    status = obj?.get("status")?.jsonPrimitive?.content,
                    truncated = obj?.get("truncated")?.jsonPrimitive?.content?.toBooleanStrictOrNull(),
                )
                "result" -> RunEvent.Result(
                    runId = obj?.get("runId")?.jsonPrimitive?.content,
                    status = obj?.get("status")?.jsonPrimitive?.content,
                    text = obj?.get("text")?.jsonPrimitive?.content,
                    durationMs = obj?.get("durationMs")?.jsonPrimitive?.content?.toLongOrNull(),
                )
                "heartbeat" -> RunEvent.Heartbeat
                "done" -> RunEvent.Done
                "error" -> RunEvent.Error(obj?.get("message")?.jsonPrimitive?.content ?: data)
                else -> RunEvent.Unknown(type, data)
            }
        } catch (_: Exception) {
            RunEvent.Unknown(type, data)
        }
    }
}
