package fail.failure.grok.network

import fail.failure.grok.network.model.ChatCompletionChunk
import fail.failure.grok.network.model.ChatCompletionRequest
import fail.failure.grok.network.model.ChatCompletionResponse
import fail.failure.grok.network.model.OpenAiModelsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

/**
 * cli-chat-proxy / OpenAI-compatible surface used by Grok Build
 * (`POST /v1/chat/completions`, `GET /v1/models`).
 */
interface GrokApiService {
    @GET("models")
    suspend fun listModels(): OpenAiModelsResponse

    @POST("chat/completions")
    suspend fun chatCompletions(@Body body: ChatCompletionRequest): ChatCompletionResponse
}
