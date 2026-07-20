package fail.failure.cursor.auth

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.math.min

/**
 * Talks to the same two endpoints the Cursor desktop app and `cursor-agent login` CLI use
 * for a full account sign-in (as opposed to a scoped API key):
 *
 *  1. The user is sent to https://cursor.com/loginDeepControl?challenge=...&uuid=...&mode=login
 *     &redirectTarget=cli in a browser/Custom Tab and completes Cursor's normal login/2FA/SSO
 *     there.
 *  2. Meanwhile this client polls https://api2.cursor.sh/auth/poll?uuid=...&verifier=...
 *     which returns 404 until the browser step finishes, then returns the session tokens.
 *
 * The exact URL parameters, polling backoff, and refresh endpoint below are verified against
 * schultzp2020/pi-extensions' pi-cursor package (packages/pi-cursor/src/{pkce,auth}.ts), a
 * working open-source implementation of this same flow - not just secondary descriptions of
 * it, since an earlier version of this client had the request shape subtly wrong (missing
 * `redirectTarget`, and calling a refresh endpoint that doesn't exist) and login silently
 * never completed as a result.
 */
class DeepLinkAuthClient(private val httpClient: OkHttpClient) {

    @Serializable
    data class PollResponse(
        @SerialName("accessToken") val accessToken: String? = null,
        @SerialName("refreshToken") val refreshToken: String? = null,
    )

    @Serializable
    data class RefreshResponse(
        @SerialName("accessToken") val accessToken: String? = null,
        @SerialName("refreshToken") val refreshToken: String? = null,
    )

    private val json = Json { ignoreUnknownKeys = true }

    fun buildLoginUrl(challenge: PkceUtil.LoginChallenge): String {
        return "https://cursor.com/loginDeepControl" +
            "?challenge=${challenge.challenge}" +
            "&uuid=${challenge.uuid}" +
            "&mode=login" +
            "&redirectTarget=cli"
    }

    /**
     * Suspends, polling with the same exponential backoff as the reference implementation
     * (1s base delay, x1.2 per attempt, capped at 10s, up to 150 attempts - a bit over 5
     * minutes total) until the browser login completes. A 404 means "not yet" and is expected
     * throughout most of the poll; any other non-2xx counts as an error, and 3 in a row aborts
     * early rather than spinning for the full 150 attempts on a dead session.
     */
    suspend fun pollForSession(challenge: PkceUtil.LoginChallenge): PollResponse? {
        var delayMs = POLL_BASE_DELAY_MS
        var consecutiveErrors = 0

        repeat(POLL_MAX_ATTEMPTS) {
            delay(delayMs)
            val outcome = withContext(Dispatchers.IO) { pollOnce(challenge) }
            when (outcome) {
                is PollOutcome.Success -> return outcome.response
                PollOutcome.Pending -> {
                    consecutiveErrors = 0
                    delayMs = min((delayMs * POLL_BACKOFF_MULTIPLIER).toLong(), POLL_MAX_DELAY_MS)
                }
                PollOutcome.Error -> {
                    consecutiveErrors++
                    if (consecutiveErrors >= 3) return null
                }
            }
        }
        return null
    }

    private sealed interface PollOutcome {
        data class Success(val response: PollResponse) : PollOutcome
        data object Pending : PollOutcome
        data object Error : PollOutcome
    }

    private fun pollOnce(challenge: PkceUtil.LoginChallenge): PollOutcome {
        val url = "https://api2.cursor.sh/auth/poll" +
            "?uuid=${challenge.uuid}&verifier=${challenge.verifier}"
        val request = Request.Builder().url(url).get().build()
        return try {
            httpClient.newCall(request).execute().use { response ->
                when {
                    response.code == 404 -> PollOutcome.Pending
                    response.isSuccessful -> {
                        val body = response.body?.string().orEmpty()
                        val parsed = json.decodeFromString<PollResponse>(body)
                        if (parsed.accessToken.isNullOrBlank()) PollOutcome.Error else PollOutcome.Success(parsed)
                    }
                    else -> PollOutcome.Error
                }
            }
        } catch (_: Exception) {
            PollOutcome.Error
        }
    }

    /** Mirrors auth/exchange_user_api_key: POST with the refresh token as a bearer, empty body. */
    suspend fun refresh(refreshToken: String): RefreshResponse? = withContext(Dispatchers.IO) {
        val body = "{}".toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://api2.cursor.sh/auth/exchange_user_api_key")
            .header("Authorization", "Bearer $refreshToken")
            .post(body)
            .build()
        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val responseBody = response.body?.string().orEmpty()
                if (responseBody.isBlank()) return@withContext null
                json.decodeFromString<RefreshResponse>(responseBody)
            }
        } catch (_: Exception) {
            null
        }
    }

    private companion object {
        const val POLL_MAX_ATTEMPTS = 150
        const val POLL_BASE_DELAY_MS = 1000L
        const val POLL_MAX_DELAY_MS = 10_000L
        const val POLL_BACKOFF_MULTIPLIER = 1.2
    }
}
