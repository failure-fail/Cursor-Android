package fail.failure.grok.network

import fail.failure.grok.agents.ChatStore
import fail.failure.grok.auth.TokenStore
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient(
    tokenStore: TokenStore,
    chatStore: ChatStore,
) {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(tokenStore))
        .addInterceptor(
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC },
        )
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .callTimeout(0, TimeUnit.SECONDS)
        .build()

    private val retrofitService: GrokApiService = Retrofit.Builder()
        .baseUrl(GrokEndpoints.API_BASE)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(GrokApiService::class.java)

    val service: GrokBackend = GrokBackend(retrofitService, chatStore)

    val sseClient = SseClient(okHttpClient)

    fun runStreamUrl(agentId: String, runId: String) =
        "${GrokEndpoints.API_BASE}chat/completions"
}
