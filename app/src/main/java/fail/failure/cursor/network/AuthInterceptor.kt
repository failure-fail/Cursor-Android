package fail.failure.cursor.network

import android.util.Base64
import fail.failure.cursor.auth.TokenStore
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches whichever credential we have. Per Cursor's own Cloud Agents API docs
 * (cursor.com/docs/cloud-agent/api/endpoints), a personal/service API key authenticates with
 * HTTP Basic - `curl -u YOUR_API_KEY:` - the key as the username, empty password, not a bearer
 * token. Sending it as `Authorization: Bearer <key>` (an earlier, unverified guess) got a 403
 * rather than a 401, which in hindsight tracks: the gateway recognized something was there, just
 * not in the scheme it wanted. The account access token isn't officially supported by this API at
 * all - it's kept as a Bearer fallback only because there's nothing better to send when no API
 * key has been entered yet.
 */
class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val session = tokenStore.session.value
        val apiKey = session.apiKey?.takeIf { it.isNotBlank() }

        val request = when {
            apiKey != null -> {
                val credentials = Base64.encodeToString("$apiKey:".toByteArray(), Base64.NO_WRAP)
                chain.request().newBuilder().header("Authorization", "Basic $credentials").build()
            }
            !session.accessToken.isNullOrBlank() -> {
                chain.request().newBuilder().header("Authorization", "Bearer ${session.accessToken}").build()
            }
            else -> chain.request()
        }

        return chain.proceed(request)
    }
}
