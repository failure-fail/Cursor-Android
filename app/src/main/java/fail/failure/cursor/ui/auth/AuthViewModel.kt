package fail.failure.cursor.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fail.failure.cursor.auth.AuthRepository
import fail.failure.cursor.auth.LoginPollResult
import fail.failure.cursor.auth.PkceUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LoginUiState {
    data object SignedOut : LoginUiState
    data class AwaitingBrowser(val url: String) : LoginUiState
    data class Polling(val url: String) : LoginUiState
    data object TimedOut : LoginUiState
    data class BrowserLaunchFailed(val url: String) : LoginUiState
    data object SignedIn : LoginUiState
}

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(
        if (authRepository.isSignedIn()) LoginUiState.SignedIn else LoginUiState.SignedOut,
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var pendingChallenge: PkceUtil.LoginChallenge? = null

    /** Kicks off the same browser-based account login the desktop app uses. */
    fun beginAccountLogin() {
        val result = authRepository.startAccountLogin()
        pendingChallenge = result.challenge
        _uiState.value = LoginUiState.AwaitingBrowser(result.loginUrl)
    }

    /** Call once a browser has actually been launched for [url], to start polling for completion. */
    fun onBrowserLaunched(url: String) {
        val challenge = pendingChallenge
        _uiState.value = LoginUiState.Polling(url)
        if (challenge == null) return
        // Guard against double-polling if this is called again (e.g. "open again" after the
        // first launch already kicked off polling) - only start a new poll loop once.
        if (pollStarted) return
        pollStarted = true
        viewModelScope.launch {
            when (authRepository.awaitAccountLogin(challenge)) {
                is LoginPollResult.Success -> _uiState.value = LoginUiState.SignedIn
                LoginPollResult.TimedOut -> _uiState.value = LoginUiState.TimedOut
            }
        }
    }

    /** The Custom Tab and the plain-browser fallback both failed to launch (no browser app?). */
    fun onBrowserLaunchFailed(url: String) {
        _uiState.value = LoginUiState.BrowserLaunchFailed(url)
    }

    private var pollStarted = false

    fun retry() {
        pendingChallenge = null
        pollStarted = false
        _uiState.value = LoginUiState.SignedOut
    }

    fun signInWithApiKey(apiKey: String) {
        authRepository.signInWithApiKey(apiKey)
        _uiState.value = LoginUiState.SignedIn
    }

    fun signOut() {
        authRepository.signOut()
        _uiState.value = LoginUiState.SignedOut
    }
}
