package fail.failure.grok.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fail.failure.grok.auth.AuthRepository
import fail.failure.grok.auth.GrokOAuthClient
import fail.failure.grok.auth.LoginPollResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LoginUiState {
    data object SignedOut : LoginUiState
    data class AwaitingDevice(
        val userCode: String,
        val verificationUri: String,
        val verificationUriComplete: String?,
    ) : LoginUiState
    data class Polling(
        val userCode: String,
        val verificationUri: String,
        val verificationUriComplete: String?,
    ) : LoginUiState
    data object TimedOut : LoginUiState
    data class Failed(val message: String) : LoginUiState
    data class BrowserLaunchFailed(
        val userCode: String,
        val verificationUri: String,
        val verificationUriComplete: String?,
    ) : LoginUiState
    data object SignedIn : LoginUiState
}

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(
        if (authRepository.isSignedIn()) LoginUiState.SignedIn else LoginUiState.SignedOut,
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var pendingLogin: GrokOAuthClient.DeviceLogin? = null
    private var pollStarted = false

    init {
        if (_uiState.value == LoginUiState.SignedOut) {
            authRepository.resumePendingLogin()?.let { result ->
                pendingLogin = result.login
                _uiState.value = LoginUiState.Polling(
                    userCode = result.login.userCode,
                    verificationUri = result.login.verificationUri,
                    verificationUriComplete = result.login.verificationUriComplete,
                )
                startPolling(result.login)
            }
        }
    }

    /** Starts Grok Build device-code login (`grok login --device-auth`). */
    fun beginAccountLogin() {
        pollStarted = false
        viewModelScope.launch {
            try {
                val result = authRepository.startAccountLogin()
                pendingLogin = result.login
                _uiState.value = LoginUiState.AwaitingDevice(
                    userCode = result.login.userCode,
                    verificationUri = result.login.verificationUri,
                    verificationUriComplete = result.login.verificationUriComplete,
                )
            } catch (e: Exception) {
                _uiState.value = LoginUiState.Failed(e.message ?: "Could not start login")
            }
        }
    }

    fun onBrowserLaunched() {
        val login = pendingLogin ?: return
        _uiState.value = LoginUiState.Polling(
            userCode = login.userCode,
            verificationUri = login.verificationUri,
            verificationUriComplete = login.verificationUriComplete,
        )
        startPolling(login)
    }

    private fun startPolling(login: GrokOAuthClient.DeviceLogin) {
        if (pollStarted) return
        pollStarted = true
        viewModelScope.launch {
            when (val result = authRepository.awaitAccountLogin(login)) {
                is LoginPollResult.Success -> _uiState.value = LoginUiState.SignedIn
                LoginPollResult.TimedOut -> _uiState.value = LoginUiState.TimedOut
                is LoginPollResult.Failed -> _uiState.value = LoginUiState.Failed(result.message)
            }
        }
    }

    fun onBrowserLaunchFailed() {
        val login = pendingLogin ?: return
        _uiState.value = LoginUiState.BrowserLaunchFailed(
            userCode = login.userCode,
            verificationUri = login.verificationUri,
            verificationUriComplete = login.verificationUriComplete,
        )
        // Still poll — user may open the URL on another device.
        startPolling(login)
    }

    fun retry() {
        pendingLogin = null
        pollStarted = false
        authRepository.clearPendingLogin()
        _uiState.value = LoginUiState.SignedOut
    }

    fun signInWithApiKey(apiKey: String) {
        authRepository.signInWithApiKey(apiKey)
        _uiState.value = LoginUiState.SignedIn
    }

    fun signOut() {
        authRepository.signOut()
        authRepository.clearPendingLogin()
        _uiState.value = LoginUiState.SignedOut
    }
}
