package fail.failure.grok.network

import fail.failure.grok.network.model.GrokSettingsResponse
import fail.failure.grok.network.model.OpenAiModelsResponse
import fail.failure.grok.network.model.SandboxCreateEnvironmentRequest
import fail.failure.grok.network.model.SandboxEnvironmentResponse
import fail.failure.grok.network.model.SandboxListEnvironmentsResponse
import fail.failure.grok.network.model.SandboxStartRequest
import fail.failure.grok.network.model.SandboxStartResponse
import fail.failure.grok.network.model.SandboxStatusResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Thin Retrofit surface over Grok Build's cli-chat-proxy
 * (`https://cli-chat-proxy.grok.com/v1/...`), matching paths used by
 * `xai-org/grok-build` (`SandboxClient` + models/settings fetches).
 */
interface GrokApiService {

    @GET("models")
    suspend fun listModels(): OpenAiModelsResponse

    @GET("settings")
    suspend fun settings(): GrokSettingsResponse

    @GET("sandbox/environments")
    suspend fun listEnvironments(
        @Query("page") page: Int? = null,
        @Query("pageSize") pageSize: Int? = null,
    ): SandboxListEnvironmentsResponse

    @POST("sandbox/environments")
    suspend fun createEnvironment(
        @Body request: SandboxCreateEnvironmentRequest,
    ): SandboxEnvironmentResponse

    @GET("sandbox/environments/{id}")
    suspend fun getEnvironment(@Path("id") id: String): SandboxEnvironmentResponse

    @DELETE("sandbox/environments/{id}")
    suspend fun deleteEnvironment(@Path("id") id: String)

    @POST("sandbox/sessions/start")
    suspend fun startSession(@Body request: SandboxStartRequest): SandboxStartResponse

    @GET("sandbox/sessions/{id}/status")
    suspend fun sessionStatus(@Path("id") id: String): SandboxStatusResponse
}
