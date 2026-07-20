package fail.failure.grok.agents

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
    val createdAt: String = java.time.Instant.now().toString(),
)

@Serializable
data class Chat(
    val id: String,
    val title: String,
    val model: String? = null,
    val createdAt: String,
    val updatedAt: String,
    val messages: List<ChatMessage> = emptyList(),
    val latestRunId: String? = null,
    val status: String? = null,
)

/** Local chat history — Grok Build has no mobile cloud-agents list like Cursor did. */
class ChatStore(context: Context) {
    private val prefs = context.getSharedPreferences("grok_chats", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun list(): List<Chat> {
        val raw = prefs.getString(KEY_INDEX, null) ?: return emptyList()
        return try {
            json.decodeFromString<List<String>>(raw).mapNotNull { load(it) }
                .sortedByDescending { it.updatedAt }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun load(id: String): Chat? {
        val raw = prefs.getString(keyFor(id), null) ?: return null
        return try {
            json.decodeFromString(raw)
        } catch (_: Exception) {
            null
        }
    }

    fun save(chat: Chat) {
        prefs.edit()
            .putString(keyFor(chat.id), json.encodeToString(chat))
            .apply()
        val ids = list().map { it.id }.toMutableSet()
        ids.add(chat.id)
        prefs.edit().putString(KEY_INDEX, json.encodeToString(ids.toList())).apply()
    }

    fun delete(id: String) {
        prefs.edit().remove(keyFor(id)).apply()
        val ids = list().map { it.id }.filter { it != id }
        prefs.edit().putString(KEY_INDEX, json.encodeToString(ids)).apply()
    }

    fun create(
        prompt: String,
        model: String?,
    ): Chat {
        val now = java.time.Instant.now().toString()
        val runId = UUID.randomUUID().toString()
        val chat = Chat(
            id = UUID.randomUUID().toString(),
            title = prompt.trim().take(48).ifBlank { "New chat" },
            model = model,
            createdAt = now,
            updatedAt = now,
            messages = listOf(ChatMessage(role = "user", content = prompt)),
            latestRunId = runId,
            status = "RUNNING",
        )
        save(chat)
        return chat
    }

    fun appendMessage(id: String, message: ChatMessage, status: String? = null, runId: String? = null): Chat? {
        val existing = load(id) ?: return null
        val updated = existing.copy(
            messages = existing.messages + message,
            updatedAt = java.time.Instant.now().toString(),
            status = status ?: existing.status,
            latestRunId = runId ?: existing.latestRunId,
        )
        save(updated)
        return updated
    }

    fun updateStatus(id: String, status: String, runId: String? = null): Chat? {
        val existing = load(id) ?: return null
        val updated = existing.copy(
            status = status,
            updatedAt = java.time.Instant.now().toString(),
            latestRunId = runId ?: existing.latestRunId,
        )
        save(updated)
        return updated
    }

    private fun keyFor(id: String) = "chat_$id"

    private companion object {
        const val KEY_INDEX = "chat_ids"
    }
}
