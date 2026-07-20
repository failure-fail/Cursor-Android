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
import java.security.SecureRandom

/**
 * Talks to the same two endpoints the real Cursor desktop app uses for a full account sign-in
 * (as opposed to a scoped API key).
 *
 * This was previously modeled after a third-party reimplementation (schultzp2020/pi-extensions'
 * pi-cursor package), which turned out to build a subtly different request than what Cursor's
 * own desktop app sends (notably an extraneous `redirectTarget` param and a missing
 * `supportsSelectedTeamLogin` one) - close enough to load the login page, but apparently not
 * close enough for it to ever get past its "Logging in..." holding screen. To get the actual
 * ground truth, this was rewritten directly against the real desktop app's own code: downloaded
 * the official Linux build (`cursor_3.12.17_amd64.deb` from
 * `cursor.com/api/download?platform=linux-x64`), unpacked it, and read the unminified-enough
 * `loginLink`/poll implementation straight out of
 * `resources/app/out/vs/workbench/workbench.desktop.main.js`. The request shape below - URL
 * params, poll headers, and response fields - is transcribed from that source, not inferred.
 */
class DeepLinkAuthClient(private val httpClient: OkHttpClient) {

    @Serializable
    data class PollResponse(
        @SerialName("authId") val authId: String? = null,
        @SerialName("accessToken") val accessToken: String? = null,
        @SerialName("refreshToken") val refreshToken: String? = null,
        @SerialName("selectedTeamId") val selectedTeamId: Long? = null,
    )

    @Serializable
    data class RefreshResponse(
        @SerialName("accessToken") val accessToken: String? = null,
        @SerialName("refreshToken") val refreshToken: String? = null,
    )

    private val json = Json { ignoreUnknownKeys = true }

    /** Matches the real app's `loginLink(mode = "login")`: no `redirectTarget` param exists. */
    fun buildLoginUrl(challenge: PkceUtil.LoginChallenge): String {
        return "https://cursor.com/loginDeepControl" +
            "?challenge=${challenge.challenge}" +
            "&uuid=${challenge.uuid}" +
            "&mode=login" +
            "&supportsSelectedTeamLogin=true"
    }

    /**
     * Polls every 500ms, same cadence as the real desktop app, but for up to 5 minutes rather
     * than the desktop app's ~15s (30 attempts) - reasonable for a status-bar quick-login on
     * desktop, too short for someone unlocking their phone, switching to a browser, and typing
     * a password. 404 means "not yet"; anything else non-2xx counts as an error, 3 in a row
     * aborts early rather than polling a dead session for the full 5 minutes.
     */
    suspend fun pollForSession(challenge: PkceUtil.LoginChallenge): PollResponse? {
        var consecutiveErrors = 0

        repeat(POLL_MAX_ATTEMPTS) {
            delay(POLL_INTERVAL_MS)
            val outcome = withContext(Dispatchers.IO) { pollOnce(challenge) }
            when (outcome) {
                is PollOutcome.Success -> return outcome.response
                PollOutcome.Pending -> consecutiveErrors = 0
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
        val request = Request.Builder()
            .url(url)
            // Same four headers the real desktop app sends on every poll request (see
            // getRawAuthFetchHeaders / the loginLink poll call in workbench.desktop.main.js).
            // Privacy mode and onboarding state aren't tracked here, so these use the same
            // values the app sends before it has fetched that info itself (undefined ->
            // "implicit-false", and onboarding-completed false).
            .header("x-ghost-mode", "implicit-false")
            .header("x-new-onboarding-completed", "false")
            .header("x-cursor-client-type", "ide")
            .header("traceparent", randomTraceparent())
            .get()
            .build()
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

    /** W3C Trace Context header: version-traceId-spanId-flags, all hex. */
    private fun randomTraceparent(): String {
        val random = SecureRandom()
        val traceId = ByteArray(16).also { random.nextBytes(it) }.joinToString("") { "%02x".format(it) }
        val spanId = ByteArray(8).also { random.nextBytes(it) }.joinToString("") { "%02x".format(it) }
        return "00-$traceId-$spanId-01"
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
        const val POLL_MAX_ATTEMPTS = 600
        const val POLL_INTERVAL_MS = 500L
    }
}
