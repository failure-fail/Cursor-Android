package fail.failure.cursor.agents

import android.content.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Mirrors [fail.failure.cursor.ui.agents.TranscriptLine] for storage - kept separate so the
 * UI-facing sealed interface doesn't need to carry serialization annotations. */
@Serializable
sealed interface TranscriptLineDto {
    @Serializable
    @SerialName("assistant")
    data class Assistant(val text: String) : TranscriptLineDto

    @Serializable
    @SerialName("thinking")
    data class Thinking(val text: String) : TranscriptLineDto

    @Serializable
    @SerialName("tool")
    data class Tool(val name: String, val status: String) : TranscriptLineDto

    @Serializable
    @SerialName("note")
    data class SystemNote(val text: String) : TranscriptLineDto
}

/**
 * Persists each agent's observed transcript locally. Cursor's API has no endpoint to fetch a
 * run's full message-by-message history after the fact - only a live stream while it's active, or
 * a final summary once it's done (confirmed against the docs) - so this is the only way "leave
 * and come back" doesn't lose everything: not a substitute for real server-side history, just a
 * durable local record of whatever this app has actually seen stream by.
 */
class TranscriptStore(context: Context) {
    private val prefs = context.getSharedPreferences("agent_transcripts", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun load(agentId: String): List<TranscriptLineDto> {
        val raw = prefs.getString(agentId, null) ?: return emptyList()
        return try {
            json.decodeFromString(raw)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(agentId: String, lines: List<TranscriptLineDto>) {
        prefs.edit().putString(agentId, json.encodeToString(lines)).apply()
    }
}
