package fail.failure.grok.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RepoInput(
    val url: String? = null,
    val startingRef: String? = null,
    val prUrl: String? = null,
)

@Serializable
data class CreateAgentRequest(
    val prompt: PromptInput,
    val model: ModelSelectionInput? = null,
    val name: String? = null,
    val repos: List<RepoInput>? = null,
    @SerialName("autoCreatePR") val autoCreatePr: Boolean? = null,
    @SerialName("workOnCurrentBranch") val workOnCurrentBranch: Boolean? = null,
    val env: EnvInput? = null,
    val envVars: Map<String, String>? = null,
    val mcpServers: List<McpServerInput>? = null,
    val customSubagents: List<CustomSubagentInput>? = null,
)

@Serializable
data class PromptInput(val text: String, val images: List<ImageInput>? = null)

@Serializable
data class ImageInput(
    val data: String,
    val mediaType: String,
)

@Serializable
data class EnvInput(
    val type: String,
    val keepAwake: Boolean? = null,
) {
    companion object {
        const val TYPE_CLOUD = "cloud"
        const val TYPE_MACHINE = "machine"
    }
}

@Serializable
data class ModelSelectionInput(
    val id: String,
    val params: List<ModelParamInput>? = null,
)

@Serializable
data class ModelParamInput(
    val id: String,
    val value: String,
)

@Serializable
data class McpServerInput(
    val name: String,
    val url: String,
)

@Serializable
data class CustomSubagentInput(
    val name: String,
    val prompt: String,
)

@Serializable
data class CreateAgentResponse(
    val agent: Agent,
    val run: Run? = null,
)

/** UI-facing agent row — mapped from a Grok Build sandbox environment. */
@Serializable
data class Agent(
    val id: String,
    val name: String? = null,
    val status: String? = null,
    @SerialName("latestRunId") val latestRunId: String? = null,
    val repos: List<RepoInput>? = null,
    @SerialName("autoCreatePR") val autoCreatePr: Boolean? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
    val archived: Boolean? = null,
    val env: EnvInput? = null,
    val url: String? = null,
    val description: String? = null,
)

@Serializable
data class AgentListResponse(
    @SerialName("items") val agents: List<Agent> = emptyList(),
    @SerialName("nextCursor") val cursor: String? = null,
)

@Serializable
data class GitBranch(
    val repoUrl: String? = null,
    val branch: String? = null,
    val prUrl: String? = null,
)

@Serializable
data class GitInfo(
    val branches: List<GitBranch>? = null,
)

@Serializable
data class Run(
    val id: String,
    val status: String,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("durationMs") val durationMs: Long? = null,
    val result: String? = null,
    val git: GitInfo? = null,
)

@Serializable
data class RunListResponse(
    @SerialName("items") val runs: List<Run> = emptyList(),
    @SerialName("nextCursor") val cursor: String? = null,
)

@Serializable
data class CreateRunResponse(val run: Run)

@Serializable
data class CreateRunRequest(
    val prompt: PromptInput,
    val mode: String? = null,
)

@Serializable
data class RepositoryInfo(
    val url: String,
)

@Serializable
data class RepositoryListResponse(
    @SerialName("items") val repositories: List<RepositoryInfo> = emptyList(),
)

@Serializable
data class ModelParamValue(
    val value: String,
    val displayName: String? = null,
)

@Serializable
data class ModelParameter(
    val id: String,
    val displayName: String? = null,
    val values: List<ModelParamValue>? = null,
)

@Serializable
data class ModelInfo(
    val id: String,
    val displayName: String? = null,
    val aliases: List<String>? = null,
    val parameters: List<ModelParameter>? = null,
)

@Serializable
data class ModelListResponse(
    @SerialName("items") val models: List<ModelInfo> = emptyList(),
)

@Serializable
data class ApiKeyInfo(
    @SerialName("apiKeyName") val name: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    val userId: String? = null,
    @SerialName("userEmail") val email: String? = null,
)

@Serializable
data class UsageTotals(
    @SerialName("inputTokens") val inputTokens: Long? = null,
    @SerialName("outputTokens") val outputTokens: Long? = null,
    @SerialName("totalTokens") val totalTokens: Long? = null,
)

@Serializable
data class RunUsage(
    val runId: String? = null,
    @SerialName("inputTokens") val inputTokens: Long? = null,
    @SerialName("outputTokens") val outputTokens: Long? = null,
    @SerialName("totalTokens") val totalTokens: Long? = null,
)

@Serializable
data class AgentUsageResponse(
    val totalUsage: UsageTotals? = null,
    val runs: List<RunUsage> = emptyList(),
)

@Serializable
data class Artifact(
    val path: String,
    val sizeBytes: Long? = null,
    val updatedAt: String? = null,
)

@Serializable
data class ArtifactListResponse(
    val artifacts: List<Artifact> = emptyList(),
)

@Serializable
data class ArtifactDownloadResponse(
    val url: String,
)

// --- Wire types for cli-chat-proxy sandbox API (camelCase) ---

@Serializable
data class OpenAiModelsResponse(
    val data: List<OpenAiModel> = emptyList(),
)

@Serializable
data class OpenAiModel(
    val id: String,
    @SerialName("owned_by") val ownedBy: String? = null,
)

@Serializable
data class ChatMessageWire(
    val role: String,
    val content: String,
)

@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessageWire>,
    val stream: Boolean? = null,
    val temperature: Double? = null,
)

@Serializable
data class ChatCompletionChoice(
    val index: Int? = null,
    val message: ChatMessageWire? = null,
    val delta: ChatDelta? = null,
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
data class ChatDelta(
    val role: String? = null,
    val content: String? = null,
    @SerialName("reasoning_content") val reasoningContent: String? = null,
)

@Serializable
data class ChatCompletionResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<ChatCompletionChoice> = emptyList(),
)

@Serializable
data class ChatCompletionChunk(
    val id: String? = null,
    val choices: List<ChatCompletionChoice> = emptyList(),
)

