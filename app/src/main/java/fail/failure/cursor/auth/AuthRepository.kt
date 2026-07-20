package fail.failure.cursor.auth

import kotlinx.coroutines.flow.StateFlow

sealed interface LoginStartResult {
    data class AwaitingBrowser(val loginUrl: String, val challenge: PkceUtil.LoginChallenge) : LoginStartResult
}

sealed interface LoginPollResult {
    data object Pending : LoginPollResult
    data object TimedOut : LoginPollResult
    data class Success(val userId: String?) : LoginPollResult
}

/**
 * Single source of truth for "are we signed in, and with what credential".
 * Supports two credential kinds, both usable against api.cursor.com:
 *  - a full account session obtained via [startAccountLogin]/[awaitAccountLogin] (OAuth-style
 *    deep-link flow identical to desktop/CLI)
 *  - a pasted personal/service API key ([signInWithApiKey]), kept as a fallback for users who
 *    prefer scoped keys or whose account session needs one for a specific endpoint.
 */
class AuthRepository(
    private val tokenStore: TokenStore,
    private val deepLinkAuthClient: DeepLinkAuthClient,
) {

    val session: StateFlow<StoredSession> = tokenStore.session

    fun isSignedIn(): Boolean = tokenStore.isSignedIn()

    fun startAccountLogin(): LoginStartResult.AwaitingBrowser {
        val challenge = PkceUtil.generate()
        val url = deepLinkAuthClient.buildLoginUrl(challenge)
        return LoginStartResult.AwaitingBrowser(url, challenge)
    }

    suspend fun awaitAccountLogin(challenge: PkceUtil.LoginChallenge): LoginPollResult {
        val result = deepLinkAuthClient.pollForSession(challenge) ?: return LoginPollResult.TimedOut
        val accessToken = result.accessToken ?: return LoginPollResult.TimedOut
        tokenStore.saveAccountSession(
            accessToken = accessToken,
            refreshToken = result.refreshToken,
            userId = result.authId,
        )
        return LoginPollResult.Success(result.authId)
    }

    fun signInWithApiKey(apiKey: String) {
        tokenStore.saveApiKey(apiKey.trim())
    }

    fun refreshAccountSessionIfNeeded(): Boolean {
        val current = session.value
        val refreshToken = current.refreshToken ?: return false
        val refreshed = deepLinkAuthClient.refresh(refreshToken) ?: return false
        val newAccessToken = refreshed.accessToken ?: return false
        tokenStore.saveAccountSession(
            accessToken = newAccessToken,
            refreshToken = refreshed.refreshToken ?: refreshToken,
            userId = current.userId,
        )
        return true
    }

    fun signOut() = tokenStore.clear()
}
