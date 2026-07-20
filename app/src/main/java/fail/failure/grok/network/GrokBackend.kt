package fail.failure.grok.network

import fail.failure.grok.network.model.Agent
import fail.failure.grok.network.model.AgentListResponse
import fail.failure.grok.network.model.AgentUsageResponse
import fail.failure.grok.network.model.ApiKeyInfo
import fail.failure.grok.network.model.ArtifactDownloadResponse
import fail.failure.grok.network.model.ArtifactListResponse
import fail.failure.grok.network.model.CreateAgentRequest
import fail.failure.grok.network.model.CreateAgentResponse
import fail.failure.grok.network.model.CreateRunRequest
import fail.failure.grok.network.model.CreateRunResponse
import fail.failure.grok.network.model.ModelInfo
import fail.failure.grok.network.model.ModelListResponse
import fail.failure.grok.network.model.RepositoryListResponse
import fail.failure.grok.network.model.Run
import fail.failure.grok.network.model.RunListResponse
import fail.failure.grok.network.model.SandboxCreateEnvironmentRequest
import fail.failure.grok.network.model.SandboxStartRequest
import fail.failure.grok.network.model.toAgent

/**
 * Compatibility facade: keeps the former Grok Agents ViewModel call shape
 * while talking to Grok Build sandbox + models endpoints.
 */
class GrokBackend(private val service: GrokApiService) {

    suspend fun me(): ApiKeyInfo {
        val settings = runCatching { service.settings() }.getOrNull()
        return ApiKeyInfo(
            apiKeyName = "Grok Build",
            userEmail = settings?.email,
        )
    }

    suspend fun models(): ModelListResponse {
        val response = service.listModels()
        return ModelListResponse(
            models = response.data.map { ModelInfo(id = it.id, displayName = it.id) },
        )
    }

    suspend fun repositories(): RepositoryListResponse = RepositoryListResponse()

    suspend fun listAgents(limit: Int = 50, cursor: String? = null, includeArchived: Boolean = false): AgentListResponse {
        val page = cursor?.toIntOrNull()
        val response = service.listEnvironments(page = page, pageSize = limit)
        val agents = response.environments.mapNotNull { it.toAgent() }
        val next = if (response.hasMore == true) {
            ((response.page ?: 0) + 1).toString()
        } else {
            null
        }
        return AgentListResponse(agents = agents, cursor = next)
    }

    suspend fun createAgent(request: CreateAgentRequest): CreateAgentResponse {
        val repoUrl = request.repos?.firstOrNull()?.url
        val created = service.createEnvironment(
            SandboxCreateEnvironmentRequest(
                name = request.name ?: request.prompt.text.take(48).ifBlank { "Sandbox" },
                description = request.prompt.text,
                repository = repoUrl,
                defaultBranch = request.repos?.firstOrNull()?.startingRef,
            ),
        )
        val agent = created.environment?.toAgent()
            ?: error("Sandbox create returned no environment")
        val run = runCatching {
            val started = service.startSession(
                SandboxStartRequest(
                    environmentId = agent.id,
                    repository = repoUrl,
                    branch = request.repos?.firstOrNull()?.startingRef,
                ),
            )
            Run(id = started.sessionId, status = "STARTING", createdAt = null)
        }.getOrNull()
        return CreateAgentResponse(agent = agent, run = run)
    }

    suspend fun getAgent(id: String): Agent {
        return service.getEnvironment(id).environment?.toAgent()
            ?: error("Environment $id not found")
    }

    suspend fun createRun(id: String, request: CreateRunRequest): CreateRunResponse {
        val started = service.startSession(
            SandboxStartRequest(
                environmentId = id,
                mode = "SANDBOX_MODE_AGENT",
            ),
        )
        return CreateRunResponse(
            run = Run(
                id = started.sessionId.ifBlank { started.sandboxId },
                status = "STARTING",
            ),
        )
    }

    suspend fun listRuns(id: String, limit: Int = 50): RunListResponse {
        // Sandbox API has no run history list; surface the latest session id on the agent if any.
        return RunListResponse()
    }

    suspend fun getRun(id: String, runId: String): Run {
        val status = service.sessionStatus(runId)
        return Run(
            id = runId,
            status = status.status.ifBlank { "UNKNOWN" },
            result = status.message.takeIf { it.isNotBlank() },
        )
    }

    suspend fun cancelRun(id: String, runId: String) {
        // No direct cancel endpoint exposed here; hibernate/terminate live on other paths.
    }

    suspend fun getUsage(id: String, runId: String? = null): AgentUsageResponse = AgentUsageResponse()

    suspend fun listArtifacts(id: String): ArtifactListResponse = ArtifactListResponse()

    suspend fun getArtifactDownloadUrl(id: String, path: String): ArtifactDownloadResponse =
        ArtifactDownloadResponse()

    suspend fun archiveAgent(id: String) {
        // No archive concept — delete is the destructive path; keep as no-op for UI affordance.
    }

    suspend fun unarchiveAgent(id: String) = Unit

    suspend fun deleteAgent(id: String) {
        service.deleteEnvironment(id)
    }
}
