package fail.failure.grok.auth

import fail.failure.grok.network.GrokEndpoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Grok Build OAuth against `https://auth.x.ai`, transcribed from
 * `xai-org/grok-build` (`xai-grok-shell` device_code + oidc protocol).
 *
 * Primary mobile flow is RFC 8628 device authorization (same as
 * `grok login --device-auth`): show a user code + verification URL, poll
 * `/oauth2/token` until the browser consent completes.
 */
class GrokOAuthClient(private val httpClient: OkHttpClient) {

    private val json = Json { ignoreUnknownKeys = true }

    data class DeviceLogin(
        val deviceCode: String,
        val userCode: String,
        val verificationUri: String,
        val verificationUriComplete: String?,
        val intervalSeconds: Int,
        val expiresInSeconds: Long,
    )

    data class TokenPair(
        val accessToken: String,
        val refreshToken: String?,
        val expiresIn: Long?,
        val idToken: String?,
    )

    @Serializable
    private data class DeviceCodeResponse(
        @SerialName("device_code") val deviceCode: String,
        @SerialName("user_code") val userCode: String,
        @SerialName("verification_uri") val verificationUri: String,
        @SerialName("verification_uri_complete") val verificationUriComplete: String? = null,
        @SerialName("expires_in") val expiresIn: Long,
        val interval: Int? = null,
    )

    @Serializable
    private data class TokenOk(
        @SerialName("access_token") val accessToken: String,
        @SerialName("refresh_token") val refreshToken: String? = null,
        @SerialName("expires_in") val expiresIn: Long? = null,
        @SerialName("id_token") val idToken: String? = null,
        val error: String? = null,
        @SerialName("error_description") val errorDescription: String? = null,
    )

    suspend fun requestDeviceCode(): DeviceLogin = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("client_id", GrokEndpoints.OAUTH_CLIENT_ID)
            .add("scope", SCOPES)
            .add("referrer", "grok-build")
            .build()
        val request = Request.Builder()
            .url("${GrokEndpoints.OAUTH_ISSUER}/oauth2/device/code")
            .header("x-grok-client-version", GrokEndpoints.CLIENT_VERSION)
            .header("x-grok-client-surface", "ui")
            .post(body)
            .build()
        httpClient.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error("Device code request failed (HTTP ${response.code}): $text")
            }
            val parsed = json.decodeFromString(DeviceCodeResponse.serializer(), text)
            require(parsed.userCode.all { it.isLetterOrDigit() || it == '-' }) {
                "Invalid user_code from auth.x.ai"
            }
            DeviceLogin(
                deviceCode = parsed.deviceCode,
                userCode = parsed.userCode,
                verificationUri = parsed.verificationUri,
                verificationUriComplete = parsed.verificationUriComplete,
                intervalSeconds = (parsed.interval ?: 5).coerceAtLeast(1),
                expiresInSeconds = parsed.expiresIn,
            )
        }
    }

    /**
     * Polls until approved, denied, or expired. Returns null on timeout /
     * denial / network exhaustion (caller shows TimedOut).
     */
    suspend fun pollDeviceLogin(login: DeviceLogin): TokenPair? {
        var intervalMs = login.intervalSeconds * 1000L
        val deadline = System.currentTimeMillis() + login.expiresInSeconds * 1000L
        while (System.currentTimeMillis() < deadline) {
            delay(intervalMs)
            val outcome = withContext(Dispatchers.IO) { pollTokenOnce(login.deviceCode) }
            when (outcome) {
                is PollOutcome.Success -> return outcome.tokens
                PollOutcome.Pending -> Unit
                PollOutcome.SlowDown -> intervalMs += 5_000L
                PollOutcome.Denied, PollOutcome.Expired -> return null
                PollOutcome.Error -> {
                    // Transient — keep going until expiry.
                }
            }
        }
        return null
    }

    private sealed interface PollOutcome {
        data class Success(val tokens: TokenPair) : PollOutcome
        data object Pending : PollOutcome
        data object SlowDown : PollOutcome
        data object Denied : PollOutcome
        data object Expired : PollOutcome
        data object Error : PollOutcome
    }

    private fun pollTokenOnce(deviceCode: String): PollOutcome {
        val body = FormBody.Builder()
            .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
            .add("device_code", deviceCode)
            .add("client_id", GrokEndpoints.OAUTH_CLIENT_ID)
            .build()
        val request = Request.Builder()
            .url("${GrokEndpoints.OAUTH_ISSUER}/oauth2/token")
            .header("x-grok-client-version", GrokEndpoints.CLIENT_VERSION)
            .header("x-grok-client-surface", "ui")
            .post(body)
            .build()
        return try {
            httpClient.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                val parsed = try {
                    json.decodeFromString(TokenOk.serializer(), text)
                } catch (_: Exception) {
                    return PollOutcome.Error
                }
                when {
                    response.isSuccessful && parsed.accessToken.isNotBlank() ->
                        PollOutcome.Success(
                            TokenPair(
                                accessToken = parsed.accessToken,
                                refreshToken = parsed.refreshToken,
                                expiresIn = parsed.expiresIn,
                                idToken = parsed.idToken,
                            ),
                        )
                    parsed.error == "authorization_pending" -> PollOutcome.Pending
                    parsed.error == "slow_down" -> PollOutcome.SlowDown
                    parsed.error == "access_denied" -> PollOutcome.Denied
                    parsed.error == "expired_token" -> PollOutcome.Expired
                    else -> PollOutcome.Error
                }
            }
        } catch (_: Exception) {
            PollOutcome.Error
        }
    }

    suspend fun refresh(refreshToken: String): TokenPair? = withContext(Dispatchers.IO) {
        val body = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .add("client_id", GrokEndpoints.OAUTH_CLIENT_ID)
            .build()
        val request = Request.Builder()
            .url("${GrokEndpoints.OAUTH_ISSUER}/oauth2/token")
            .header("x-grok-client-version", GrokEndpoints.CLIENT_VERSION)
            .post(body)
            .build()
        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val text = response.body?.string().orEmpty()
                val parsed = json.decodeFromString(TokenOk.serializer(), text)
                TokenPair(
                    accessToken = parsed.accessToken,
                    refreshToken = parsed.refreshToken,
                    expiresIn = parsed.expiresIn,
                    idToken = parsed.idToken,
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        /** Default scopes from Grok Build's `default_oauth2_scopes()`. */
        const val SCOPES =
            "openid profile email offline_access grok-cli:access api:access " +
                "conversations:read conversations:write workspaces:read workspaces:write"
    }
}
