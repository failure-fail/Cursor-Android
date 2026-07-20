package fail.failure.cursor.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RepoInput(
    val owner: String? = null,
    val repo: String? = null,
    @SerialName("repositoryUrl") val repositoryUrl: String? = null,
    val ref: String? = null,
)

@Serializable
data class CreateAgentRequest(
    val prompt: PromptInput,
    val model: String? = null,
    val name: String? = null,
    val repos: List<RepoInput>? = null,
    @SerialName("autoCreatePr") val autoCreatePr: Boolean? = null,
    @SerialName("workOnCurrentBranch") val workOnCurrentBranch: Boolean? = null,
)

@Serializable
data class PromptInput(val text: String)

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
    @SerialName("autoCreatePr") val autoCreatePr: Boolean? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    val archived: Boolean? = null,
)

@Serializable
data class AgentListResponse(
    val agents: List<Agent> = emptyList(),
    val cursor: String? = null,
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
data class AgentUsageResponse(
    val totalUsage: UsageTotals? = null,
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
