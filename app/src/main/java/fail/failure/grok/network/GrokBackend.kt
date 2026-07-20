package fail.failure.grok.network

import fail.failure.grok.agents.Chat
import fail.failure.grok.agents.ChatMessage
import fail.failure.grok.agents.ChatStore
import fail.failure.grok.network.model.Agent
import fail.failure.grok.network.model.AgentListResponse
import fail.failure.grok.network.model.AgentUsageResponse
import fail.failure.grok.network.model.ApiKeyInfo
import fail.failure.grok.network.model.ArtifactDownloadResponse
import fail.failure.grok.network.model.ArtifactListResponse
import fail.failure.grok.network.model.ChatCompletionRequest
import fail.failure.grok.network.model.ChatMessageWire
import fail.failure.grok.network.model.CreateAgentRequest
import fail.failure.grok.network.model.CreateAgentResponse
import fail.failure.grok.network.model.CreateRunRequest
import fail.failure.grok.network.model.CreateRunResponse
import fail.failure.grok.network.model.ModelInfo
import fail.failure.grok.network.model.ModelListResponse
import fail.failure.grok.network.model.RepositoryListResponse
import fail.failure.grok.network.model.Run
import fail.failure.grok.network.model.RunListResponse
import java.util.UUID

/**
 * Maps the former Agents UI onto local chats + cli-chat-proxy
 * `POST /v1/chat/completions` (sandbox/environments returns 404 on the proxy).
 */
class GrokBackend(
    private val service: GrokApiService,
    private val chatStore: ChatStore,
) {

    suspend fun me(): ApiKeyInfo = ApiKeyInfo(name = "Grok Build")

    suspend fun models(): ModelListResponse {
        val response = runCatching { service.listModels() }.getOrNull()
        val models = response?.data?.map { ModelInfo(id = it.id, displayName = it.id) }
            ?.ifEmpty { null }
            ?: DEFAULT_MODELS
        return ModelListResponse(models = models)
    }

    suspend fun repositories(): RepositoryListResponse = RepositoryListResponse()

    suspend fun listAgents(
        limit: Int = 50,
        cursor: String? = null,
        includeArchived: Boolean = false,
    ): AgentListResponse {
        val agents = chatStore.list().take(limit).map { it.toAgent() }
        return AgentListResponse(agents = agents)
    }

    suspend fun createAgent(request: CreateAgentRequest): CreateAgentResponse {
        val model = request.model?.id ?: DEFAULT_MODELS.first().id
        val chat = chatStore.create(prompt = request.prompt.text, model = model)
        // Complete the first turn so opening the chat isn't an empty 404 stream.
        val reply = completeChat(chat)
        val refreshed = chatStore.load(chat.id) ?: chat
        val run = Run(
            id = refreshed.latestRunId ?: chat.latestRunId ?: UUID.randomUUID().toString(),
            status = refreshed.status ?: "FINISHED",
            result = reply,
        )
        return CreateAgentResponse(agent = refreshed.toAgent(), run = run)
    }

    suspend fun getAgent(id: String): Agent {
        return chatStore.load(id)?.toAgent() ?: error("Chat $id not found")
    }

    suspend fun createRun(id: String, request: CreateRunRequest): CreateRunResponse {
        val chat = chatStore.load(id) ?: error("Chat $id not found")
        val runId = UUID.randomUUID().toString()
        chatStore.appendMessage(
            id,
            ChatMessage(role = "user", content = request.prompt.text),
            status = "RUNNING",
            runId = runId,
        )
        val updated = chatStore.load(id) ?: chat
        val reply = completeChat(updated)
        return CreateRunResponse(
            run = Run(
                id = runId,
                status = "FINISHED",
                result = reply,
            ),
        )
    }

    suspend fun listRuns(id: String, limit: Int = 50): RunListResponse {
        val chat = chatStore.load(id) ?: return RunListResponse()
        val runId = chat.latestRunId ?: return RunListResponse()
        val lastAssistant = chat.messages.lastOrNull { it.role == "assistant" }?.content
        return RunListResponse(
            runs = listOf(
                Run(
                    id = runId,
                    status = chat.status ?: "FINISHED",
                    createdAt = chat.updatedAt,
                    result = lastAssistant,
                ),
            ),
        )
    }

    suspend fun getRun(id: String, runId: String): Run {
        val chat = chatStore.load(id) ?: error("Chat $id not found")
        val lastAssistant = chat.messages.lastOrNull { it.role == "assistant" }?.content
        return Run(
            id = runId,
            status = chat.status ?: "FINISHED",
            result = lastAssistant,
        )
    }

    suspend fun cancelRun(id: String, runId: String) {
        chatStore.updateStatus(id, "CANCELLED", runId)
    }

    suspend fun getUsage(id: String, runId: String? = null): AgentUsageResponse = AgentUsageResponse()

    suspend fun listArtifacts(id: String): ArtifactListResponse = ArtifactListResponse()

    suspend fun getArtifactDownloadUrl(id: String, path: String): ArtifactDownloadResponse =
        ArtifactDownloadResponse(url = "")

    suspend fun archiveAgent(id: String) = Unit

    suspend fun unarchiveAgent(id: String) = Unit

    suspend fun deleteAgent(id: String) {
        chatStore.delete(id)
    }

    private suspend fun completeChat(chat: Chat): String {
        val model = chat.model ?: DEFAULT_MODELS.first().id
        val messages = chat.messages.map { ChatMessageWire(role = it.role, content = it.content) }
        val response = service.chatCompletions(
            ChatCompletionRequest(
                model = model,
                messages = messages,
                stream = false,
            ),
        )
        val text = response.choices.firstOrNull()?.message?.content.orEmpty()
        if (text.isNotBlank()) {
            chatStore.appendMessage(
                chat.id,
                ChatMessage(role = "assistant", content = text),
                status = "FINISHED",
            )
        } else {
            chatStore.updateStatus(chat.id, "FINISHED")
        }
        return text
    }

    private fun Chat.toAgent(): Agent = Agent(
        id = id,
        name = title,
        status = status,
        latestRunId = latestRunId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        archived = false,
        description = messages.lastOrNull { it.role == "user" }?.content,
        url = null,
    )

    companion object {
        val DEFAULT_MODELS = listOf(
            ModelInfo(id = "grok-4", displayName = "Grok 4"),
            ModelInfo(id = "grok-3", displayName = "Grok 3"),
            ModelInfo(id = "grok-3-mini", displayName = "Grok 3 Mini"),
        )
    }
}
