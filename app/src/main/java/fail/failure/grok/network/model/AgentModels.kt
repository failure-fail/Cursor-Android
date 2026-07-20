package fail.failure.grok.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
data class RepoInput(
    val url: String? = null,
    val startingRef: String? = null,
    val prUrl: String? = null,
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
data class ImageInput(val data: String, val mediaType: String)

@Serializable
data class ModelSelectionInput(
    val id: String,
    val params: List<ModelParamInput>? = null,
)

@Serializable
data class ModelParamInput(val id: String, val value: String)

@Serializable
data class McpServerInput(val name: String, val url: String)

@Serializable
data class CustomSubagentInput(val name: String, val prompt: String)

@Serializable
data class CreateAgentResponse(
    val agent: Agent,
    val run: Run? = null,
)

@Serializable
data class AgentListResponse(
    @SerialName("items") val agents: List<Agent> = emptyList(),
    @SerialName("nextGrok") val cursor: String? = null,
)

@Serializable
data class GitBranch(
    val repoUrl: String? = null,
    val branch: String? = null,
    val prUrl: String? = null,
)

@Serializable
data class GitInfo(
    val branches: List<GitBranch> = emptyList(),
)

@Serializable
data class Run(
    val id: String,
    val status: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
    val result: String? = null,
    val git: GitInfo? = null,
)

@Serializable
data class CreateRunRequest(val prompt: PromptInput)

@Serializable
data class CreateRunResponse(val run: Run)

@Serializable
data class RunListResponse(
    @SerialName("items") val runs: List<Run> = emptyList(),
)

@Serializable
data class ModelInfo(
    val id: String,
    val displayName: String? = null,
    val params: List<ModelParamDef>? = null,
)

@Serializable
data class ModelParamDef(
    val id: String,
    val displayName: String? = null,
    val type: String? = null,
    val values: List<String>? = null,
)

@Serializable
data class ModelListResponse(
    @SerialName("items") val models: List<ModelInfo> = emptyList(),
)

@Serializable
data class RepositoryInfo(
    val url: String? = null,
    val name: String? = null,
)

@Serializable
data class RepositoryListResponse(
    @SerialName("items") val repositories: List<RepositoryInfo> = emptyList(),
)

@Serializable
data class ApiKeyInfo(
    val apiKeyName: String? = null,
    val userEmail: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
)

@Serializable
data class AgentUsageResponse(
    val totalInputTokens: Long? = null,
    val totalOutputTokens: Long? = null,
)

@Serializable
data class Artifact(
    val path: String,
    val sizeBytes: Long? = null,
)

@Serializable
data class ArtifactListResponse(
    @SerialName("items") val artifacts: List<Artifact> = emptyList(),
)

@Serializable
data class ArtifactDownloadResponse(
    val url: String? = null,
)

// --- Wire types for cli-chat-proxy sandbox API (camelCase) ---

@Serializable
data class SandboxEnvironment(
    val environmentId: String? = null,
    val userId: String? = null,
    val teamId: String? = null,
    val name: String? = null,
    val description: String? = null,
    val repository: String? = null,
    val defaultBranch: String? = null,
    val createTime: String? = null,
    val modifyTime: String? = null,
)

@Serializable
data class SandboxEnvironmentWithMetadata(
    val environment: SandboxEnvironment? = null,
    val userRole: String? = null,
)

@Serializable
data class SandboxListEnvironmentsResponse(
    val environments: List<SandboxEnvironmentWithMetadata> = emptyList(),
    val page: Int? = null,
    val pageSize: Int? = null,
    val hasMore: Boolean? = null,
)

@Serializable
data class SandboxCreateEnvironmentRequest(
    val name: String? = null,
    val description: String? = null,
    val repository: String? = null,
    val defaultBranch: String? = null,
)

@Serializable
data class SandboxEnvironmentResponse(
    val environment: SandboxEnvironmentWithMetadata? = null,
)

@Serializable
data class SandboxStartRequest(
    val environmentId: String? = null,
    val repository: String? = null,
    val branch: String? = null,
    val mode: String = "SANDBOX_MODE_AGENT",
)

@Serializable
data class SandboxStartResponse(
    val sandboxId: String = "",
    val sessionId: String = "",
    val websocketUrl: String = "",
    val environment: SandboxEnvironmentWithMetadata? = null,
)

@Serializable
data class SandboxStatusResponse(
    val status: String = "",
    val message: String = "",
)

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
data class GrokSettingsResponse(
    val email: String? = null,
    val userId: String? = null,
)

fun SandboxEnvironmentWithMetadata.toAgent(): Agent? {
    val env = environment ?: return null
    val id = env.environmentId ?: return null
    return Agent(
        id = id,
        name = env.name ?: id,
        status = "READY",
        repos = env.repository?.let { listOf(RepoInput(url = it, startingRef = env.defaultBranch)) },
        createdAt = env.createTime,
        updatedAt = env.modifyTime,
        archived = false,
        env = EnvInput(type = EnvInput.TYPE_CLOUD),
        description = env.description,
        url = "https://grok.com",
    )
}
