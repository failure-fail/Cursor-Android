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
    data object Polling : LoginUiState
    data object TimedOut : LoginUiState
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

    /** Call once the Custom Tab has been launched, to start polling for completion. */
    fun onBrowserLaunched() {
        val challenge = pendingChallenge ?: return
        _uiState.value = LoginUiState.Polling
        viewModelScope.launch {
            when (authRepository.awaitAccountLogin(challenge)) {
                is LoginPollResult.Success -> _uiState.value = LoginUiState.SignedIn
                LoginPollResult.TimedOut -> _uiState.value = LoginUiState.TimedOut
            }
        }
    }

    fun retry() {
        pendingChallenge = null
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
