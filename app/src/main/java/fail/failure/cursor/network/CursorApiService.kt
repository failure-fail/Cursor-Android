package fail.failure.cursor.network

import fail.failure.cursor.network.model.Agent
import fail.failure.cursor.network.model.AgentListResponse
import fail.failure.cursor.network.model.AgentUsageResponse
import fail.failure.cursor.network.model.ApiKeyInfo
import fail.failure.cursor.network.model.ArtifactListResponse
import fail.failure.cursor.network.model.CreateAgentRequest
import fail.failure.cursor.network.model.CreateAgentResponse
import fail.failure.cursor.network.model.CreateRunRequest
import fail.failure.cursor.network.model.ModelListResponse
import fail.failure.cursor.network.model.RepositoryListResponse
import fail.failure.cursor.network.model.Run
import fail.failure.cursor.network.model.RunListResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Thin wrapper around Cursor's officially documented Background/Cloud Agents API. */
interface CursorApiService {

    @GET("v1/me")
    suspend fun me(): ApiKeyInfo

    @GET("v1/models")
    suspend fun models(): ModelListResponse

    @GET("v1/repositories")
    suspend fun repositories(): RepositoryListResponse

    @GET("v1/agents")
    suspend fun listAgents(
        @Query("limit") limit: Int = 50,
        @Query("cursor") cursor: String? = null,
        @Query("includeArchived") includeArchived: Boolean = false,
    ): AgentListResponse

    @POST("v1/agents")
    suspend fun createAgent(@Body request: CreateAgentRequest): CreateAgentResponse

    @GET("v1/agents/{id}")
    suspend fun getAgent(@Path("id") id: String): Agent

    @POST("v1/agents/{id}/runs")
    suspend fun createRun(@Path("id") id: String, @Body request: CreateRunRequest): Run

    @GET("v1/agents/{id}/runs")
    suspend fun listRuns(@Path("id") id: String, @Query("limit") limit: Int = 50): RunListResponse

    @GET("v1/agents/{id}/runs/{runId}")
    suspend fun getRun(@Path("id") id: String, @Path("runId") runId: String): Run

    @POST("v1/agents/{id}/runs/{runId}/cancel")
    suspend fun cancelRun(@Path("id") id: String, @Path("runId") runId: String)

    @GET("v1/agents/{id}/usage")
    suspend fun getUsage(@Path("id") id: String, @Query("runId") runId: String? = null): AgentUsageResponse

    @GET("v1/agents/{id}/artifacts")
    suspend fun listArtifacts(@Path("id") id: String): ArtifactListResponse

    @POST("v1/agents/{id}/archive")
    suspend fun archiveAgent(@Path("id") id: String)

    @POST("v1/agents/{id}/unarchive")
    suspend fun unarchiveAgent(@Path("id") id: String)

    @DELETE("v1/agents/{id}")
    suspend fun deleteAgent(@Path("id") id: String)
}
