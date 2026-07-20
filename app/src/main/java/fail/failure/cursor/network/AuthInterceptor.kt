package fail.failure.cursor.network

import fail.failure.cursor.auth.TokenStore
import okhttp3.Interceptor
import okhttp3.Response
import java.net.URLEncoder

/**
 * Attaches whichever credential we have. Precedence: pasted API key first (it's the
 * officially supported way to call api.cursor.com), falling back to the account session
 * token obtained from the desktop-style login flow, sent both as a bearer token and as the
 * WorkosCursorSessionToken cookie the web dashboard / api2 host expect.
 */
class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val session = tokenStore.session.value
        val builder = chain.request().newBuilder()

        val apiKey = session.apiKey
        val accessToken = session.accessToken

        when {
            !apiKey.isNullOrBlank() -> {
                builder.header("Authorization", "Bearer $apiKey")
            }
            !accessToken.isNullOrBlank() -> {
                builder.header("Authorization", "Bearer $accessToken")
                val userId = session.userId
                if (!userId.isNullOrBlank()) {
                    val cookieValue = URLEncoder.encode("$userId::$accessToken", "UTF-8")
                    builder.addHeader("Cookie", "WorkosCursorSessionToken=$cookieValue")
                }
            }
        }

        return chain.proceed(builder.build())
    }
}
