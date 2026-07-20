package fail.failure.cursor.auth

import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Talks to the same two endpoints the Cursor desktop app and `cursor-agent login` CLI use
 * for a full account sign-in (as opposed to a scoped API key):
 *
 *  1. The user is sent to https://www.cursor.com/loginDeepControl?challenge=...&uuid=...&mode=login
 *     in a browser/Custom Tab and completes Cursor's normal login/2FA/SSO there.
 *  2. Meanwhile this client long-polls https://api2.cursor.sh/auth/poll?uuid=...&verifier=...
 *     which returns 404/202 until the browser step finishes, then returns the session tokens.
 *
 * This mirrors the desktop flow exactly rather than inventing a new one, per the ask: sign-in
 * looks and behaves like it does on desktop, and the resulting session is a real Cursor account
 * session (not an API key).
 */
class DeepLinkAuthClient(private val httpClient: OkHttpClient) {

    @Serializable
    data class PollResponse(
        @SerialName("accessToken") val accessToken: String? = null,
        @SerialName("refreshToken") val refreshToken: String? = null,
        @SerialName("authId") val authId: String? = null,
    )

    @Serializable
    data class RefreshResponse(
        @SerialName("access_token") val accessToken: String? = null,
        @SerialName("refresh_token") val refreshToken: String? = null,
    )

    private val json = Json { ignoreUnknownKeys = true }

    fun buildLoginUrl(challenge: PkceUtil.LoginChallenge): String {
        return "https://www.cursor.com/loginDeepControl" +
            "?challenge=${challenge.challenge}" +
            "&uuid=${challenge.uuid}" +
            "&mode=login"
    }

    /**
     * Suspends, polling every [intervalMs], until the browser login completes or [timeoutMs]
     * elapses. Returns null on timeout so the caller can show a "still waiting / cancel" UI.
     */
    suspend fun pollForSession(
        challenge: PkceUtil.LoginChallenge,
        intervalMs: Long = 2000,
        timeoutMs: Long = 5 * 60 * 1000,
    ): PollResponse? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val result = pollOnce(challenge)
            if (result != null) return result
            delay(intervalMs)
        }
        return null
    }

    private fun pollOnce(challenge: PkceUtil.LoginChallenge): PollResponse? {
        val url = "https://api2.cursor.sh/auth/poll" +
            "?uuid=${challenge.uuid}&verifier=${challenge.verifier}"
        val request = Request.Builder().url(url).get().build()
        return try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return null
                val parsed = json.decodeFromString<PollResponse>(body)
                if (parsed.accessToken.isNullOrBlank()) null else parsed
            }
        } catch (_: IOException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Refreshes an expired access token. NOTE: the desktop client's OAuth client_id is not
     * publicly documented; this uses the value observed in community reverse-engineering of
     * the desktop app's traffic. If Cursor rotates it, update [CURSOR_OAUTH_CLIENT_ID].
     */
    fun refresh(refreshToken: String): RefreshResponse? {
        val body = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("client_id", CURSOR_OAUTH_CLIENT_ID)
            .add("refresh_token", refreshToken)
            .build()
        val request = Request.Builder()
            .url("https://api2.cursor.sh/oauth/token")
            .post(body)
            .build()
        return try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val responseBody = response.body?.string().orEmpty()
                if (responseBody.isBlank()) return null
                json.decodeFromString<RefreshResponse>(responseBody)
            }
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        const val CURSOR_OAUTH_CLIENT_ID = "cursor-ide-client"
    }
}
