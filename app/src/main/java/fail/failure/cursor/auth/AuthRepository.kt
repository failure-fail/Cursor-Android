package fail.failure.cursor.auth

import kotlinx.coroutines.flow.StateFlow

sealed interface LoginStartResult {
    data class AwaitingBrowser(val loginUrl: String, val challenge: PkceUtil.LoginChallenge) : LoginStartResult
}

sealed interface LoginPollResult {
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
        tokenStore.savePendingChallenge(
            uuid = challenge.uuid,
            verifier = challenge.verifier,
            challenge = challenge.challenge,
            createdAtMillis = System.currentTimeMillis(),
        )
        val url = deepLinkAuthClient.buildLoginUrl(challenge)
        return LoginStartResult.AwaitingBrowser(url, challenge)
    }

    /**
     * A login started before the app got backgrounded/killed while the browser had focus - the
     * uuid/verifier survive on disk even though the in-memory poll loop that was chasing them
     * didn't. Returns null (and clears it) once it's past [PENDING_CHALLENGE_TTL_MS], on the
     * assumption a login nobody finished in that long isn't coming back.
     */
    fun resumePendingLogin(): LoginStartResult.AwaitingBrowser? {
        val pending = tokenStore.readPendingChallenge() ?: return null
        if (System.currentTimeMillis() - pending.createdAtMillis > PENDING_CHALLENGE_TTL_MS) {
            tokenStore.clearPendingChallenge()
            return null
        }
        val challenge = PkceUtil.LoginChallenge(
            uuid = pending.uuid,
            verifier = pending.verifier,
            challenge = pending.challenge,
        )
        return LoginStartResult.AwaitingBrowser(deepLinkAuthClient.buildLoginUrl(challenge), challenge)
    }

    fun clearPendingLogin() = tokenStore.clearPendingChallenge()

    suspend fun awaitAccountLogin(challenge: PkceUtil.LoginChallenge): LoginPollResult {
        val result = deepLinkAuthClient.pollForSession(challenge)
        tokenStore.clearPendingChallenge()
        if (result == null) return LoginPollResult.TimedOut
        val accessToken = result.accessToken ?: return LoginPollResult.TimedOut
        val userId = result.authId ?: JwtUtil.subjectOrNull(accessToken)
        tokenStore.saveAccountSession(
            accessToken = accessToken,
            refreshToken = result.refreshToken,
            userId = userId,
        )
        return LoginPollResult.Success(userId)
    }

    private companion object {
        const val PENDING_CHALLENGE_TTL_MS = 10 * 60 * 1000L
    }

    fun signInWithApiKey(apiKey: String) {
        tokenStore.saveApiKey(apiKey.trim())
    }

    suspend fun refreshAccountSessionIfNeeded(): Boolean {
        val current = session.value
        val refreshToken = current.refreshToken ?: return false
        val refreshed = deepLinkAuthClient.refresh(refreshToken) ?: return false
        val newAccessToken = refreshed.accessToken ?: return false
        tokenStore.saveAccountSession(
            accessToken = newAccessToken,
            refreshToken = refreshed.refreshToken ?: refreshToken,
            userId = JwtUtil.subjectOrNull(newAccessToken) ?: current.userId,
        )
        return true
    }

    fun signOut() = tokenStore.clear()
}
