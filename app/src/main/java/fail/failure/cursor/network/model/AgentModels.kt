package fail.failure.cursor.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Matches the documented `repos[]` shape exactly (cursor.com/docs/cloud-agent/api/endpoints):
 * `{ url, startingRef?, prUrl? }` - there's no `owner`/`repo` pair in the real API, only a full
 * repository URL. An earlier version guessed an `owner`/`repo` shape that doesn't exist in the
 * documented schema at all, so every agent was created with no repository actually attached. */
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
    /** Base64-encoded image bytes (no data: URI prefix). */
    val data: String,
    val mediaType: String,
)

/**
 * Where the agent's run actually executes. "machine" is Cursor's Remote Control target: an
 * always-on desktop session, the same way the official mobile app hands off to your own
 * computer instead of a cloud sandbox. The exact wire shape here (particularly `keepAwake`)
 * isn't in the public API reference, so this mirrors the documented `env` field name and the
 * "keep my computer awake" toggle described in Cursor's own mobile-app announcement, but may
 * need adjusting once/if Cursor publishes the full schema.
 */
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
    /** `params[]` per the documented schema - a list of `{id, value}` pairs, not a JSON object. */
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

@Serializable
data class Agent(
    val id: String,
    val name: String? = null,
    val status: String? = null,
    @SerialName("latestRunId") val latestRunId: String? = null,
    val repos: List<RepoInput>? = null,
    @SerialName("autoCreatePR") val autoCreatePr: Boolean? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    val archived: Boolean? = null,
    val env: EnvInput? = null,
)

@Serializable
data class AgentListResponse(
    val agents: List<Agent> = emptyList(),
    @SerialName("nextCursor") val cursor: String? = null,
)

@Serializable
data class GitInfo(
    val branches: List<String>? = null,
    @SerialName("prUrls") val prUrls: List<String>? = null,
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
    val runs: List<Run> = emptyList(),
    val cursor: String? = null,
)

@Serializable
data class CreateRunRequest(
    val prompt: PromptInput,
    val mode: String? = null,
)

@Serializable
data class RepositoryInfo(
    val owner: String,
    val repo: String,
    @SerialName("fullName") val fullName: String? = null,
    val private2: Boolean? = null,
)

@Serializable
data class RepositoryListResponse(
    val repositories: List<RepositoryInfo> = emptyList(),
)

@Serializable
data class ModelParameter(
    val name: String? = null,
    val values: List<String>? = null,
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
    val models: List<ModelInfo> = emptyList(),
)

@Serializable
data class ApiKeyInfo(
    val name: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    val userId: String? = null,
    val email: String? = null,
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
