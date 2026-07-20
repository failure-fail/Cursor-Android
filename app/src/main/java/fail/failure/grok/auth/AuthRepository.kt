package fail.failure.grok.auth

import kotlinx.coroutines.flow.StateFlow

sealed interface LoginStartResult {
    data class AwaitingDevice(
        val login: GrokOAuthClient.DeviceLogin,
    ) : LoginStartResult
}

sealed interface LoginPollResult {
    data object TimedOut : LoginPollResult
    data class Success(val userId: String?) : LoginPollResult
    data class Failed(val message: String) : LoginPollResult
}

/**
 * Single source of truth for "are we signed in, and with what credential".
 *
 * Supports two credential kinds, both usable against cli-chat-proxy.grok.com:
 *  - a Grok Build OAuth session from [startAccountLogin]/[awaitAccountLogin]
 *    (RFC 8628 device code, same as `grok login --device-auth`)
 *  - a pasted `xai-...` API key ([signInWithApiKey]), matching CLI `XAI_API_KEY`
 */
class AuthRepository(
    private val tokenStore: TokenStore,
    private val oauthClient: GrokOAuthClient,
) {

    val session: StateFlow<StoredSession> = tokenStore.session

    fun isSignedIn(): Boolean = tokenStore.isSignedIn()

    suspend fun startAccountLogin(): LoginStartResult.AwaitingDevice {
        val login = oauthClient.requestDeviceCode()
        tokenStore.savePendingDeviceLogin(
            PendingDeviceLogin(
                deviceCode = login.deviceCode,
                userCode = login.userCode,
                verificationUri = login.verificationUri,
                verificationUriComplete = login.verificationUriComplete,
                intervalSeconds = login.intervalSeconds,
                expiresAtMillis = System.currentTimeMillis() + login.expiresInSeconds * 1000L,
                createdAtMillis = System.currentTimeMillis(),
            ),
        )
        return LoginStartResult.AwaitingDevice(login)
    }

    /**
     * Resume a device login if the process was killed while the user was in the
     * browser. Returns null once past the server-provided expiry.
     */
    fun resumePendingLogin(): LoginStartResult.AwaitingDevice? {
        val pending = tokenStore.readPendingDeviceLogin() ?: return null
        if (System.currentTimeMillis() > pending.expiresAtMillis) {
            tokenStore.clearPendingDeviceLogin()
            return null
        }
        val remaining = ((pending.expiresAtMillis - System.currentTimeMillis()) / 1000L)
            .coerceAtLeast(60L)
        return LoginStartResult.AwaitingDevice(
            GrokOAuthClient.DeviceLogin(
                deviceCode = pending.deviceCode,
                userCode = pending.userCode,
                verificationUri = pending.verificationUri,
                verificationUriComplete = pending.verificationUriComplete,
                intervalSeconds = pending.intervalSeconds,
                expiresInSeconds = remaining,
            ),
        )
    }

    fun clearPendingLogin() = tokenStore.clearPendingDeviceLogin()

    suspend fun awaitAccountLogin(login: GrokOAuthClient.DeviceLogin): LoginPollResult {
        val result = try {
            oauthClient.pollDeviceLogin(login)
        } catch (e: Exception) {
            tokenStore.clearPendingDeviceLogin()
            return LoginPollResult.Failed(e.message ?: "Login failed")
        }
        tokenStore.clearPendingDeviceLogin()
        if (result == null) return LoginPollResult.TimedOut
        val userId = JwtUtil.subjectOrNull(result.idToken ?: result.accessToken)
        val email = JwtUtil.emailOrNull(result.idToken ?: result.accessToken)
        tokenStore.saveAccountSession(
            accessToken = result.accessToken,
            refreshToken = result.refreshToken,
            userId = userId,
            email = email,
            expiresInSeconds = result.expiresIn,
        )
        return LoginPollResult.Success(userId)
    }

    fun signInWithApiKey(apiKey: String) {
        tokenStore.saveApiKey(apiKey.trim())
    }

    suspend fun refreshAccountSessionIfNeeded(): Boolean {
        val current = session.value
        val refreshToken = current.refreshToken ?: return false
        val refreshed = oauthClient.refresh(refreshToken) ?: return false
        tokenStore.saveAccountSession(
            accessToken = refreshed.accessToken,
            refreshToken = refreshed.refreshToken ?: refreshToken,
            userId = JwtUtil.subjectOrNull(refreshed.idToken ?: refreshed.accessToken)
                ?: current.userId,
            email = JwtUtil.emailOrNull(refreshed.idToken ?: refreshed.accessToken) ?: current.email,
            expiresInSeconds = refreshed.expiresIn,
        )
        return true
    }

    fun signOut() = tokenStore.clear()
}
