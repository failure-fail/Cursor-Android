package fail.failure.cursor.network

import fail.failure.cursor.auth.TokenStore
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches whichever credential we have as a bearer token - Cursor's documented API accepts
 * either a personal/service API key or (per the same shape) an account access token this way.
 * An earlier version also sent a guessed `WorkosCursorSessionToken` cookie for extra
 * compatibility; that was unverified and dropped; a malformed cookie is exactly the kind of
 * thing that would make the server reject requests outright, and it wasn't confirmed necessary.
 */
class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val session = tokenStore.session.value
        val token = session.apiKey?.takeIf { it.isNotBlank() } ?: session.accessToken

        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        }

        return chain.proceed(request)
    }
}
