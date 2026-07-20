package fail.failure.cursor.network

import fail.failure.cursor.auth.TokenStore
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

class ApiClient(tokenStore: TokenStore) {

    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(tokenStore))
        .addInterceptor(
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC },
        )
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        // Long-lived for the SSE run stream; individual calls set their own timeouts as needed.
        .callTimeout(0, TimeUnit.SECONDS)
        .build()

    val service: CursorApiService = Retrofit.Builder()
        .baseUrl(CursorEndpoints.API_BASE)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(CursorApiService::class.java)

    val sseClient = SseClient(okHttpClient)

    fun runStreamUrl(agentId: String, runId: String) =
        "${CursorEndpoints.API_BASE}v1/agents/$agentId/runs/$runId/stream"
}
