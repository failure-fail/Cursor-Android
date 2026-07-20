package fail.failure.grok.network

import fail.failure.grok.auth.TokenStore
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches Grok Build credentials the same way the CLI does against
 * cli-chat-proxy: `Authorization: Bearer <token>` plus
 * `X-XAI-Token-Auth: xai-grok-cli` for OAuth sessions. API keys (`xai-...`)
 * are sent as Bearer without the token-auth header (parity with `XAI_API_KEY`).
 */
class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val session = tokenStore.session.value
        val apiKey = session.apiKey?.takeIf { it.isNotBlank() }
        val accessToken = session.accessToken?.takeIf { it.isNotBlank() }

        val builder = chain.request().newBuilder()
            .header("x-grok-client-version", GrokEndpoints.CLIENT_VERSION)

        when {
            apiKey != null -> {
                builder.header("Authorization", "Bearer $apiKey")
            }
            accessToken != null -> {
                builder.header("Authorization", "Bearer $accessToken")
                builder.header("X-XAI-Token-Auth", GrokEndpoints.TOKEN_AUTH_HEADER)
                session.userId?.takeIf { it.isNotBlank() }?.let {
                    builder.header("x-userid", it)
                }
                session.email?.takeIf { it.isNotBlank() }?.let {
                    builder.header("x-email", it)
                }
            }
        }

        return chain.proceed(builder.build())
    }
}
