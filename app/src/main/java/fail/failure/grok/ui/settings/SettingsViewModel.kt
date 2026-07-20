package fail.failure.grok.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fail.failure.grok.auth.AuthRepository
import fail.failure.grok.network.ApiClient
import fail.failure.grok.network.model.ApiKeyInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(val apiKeyInfo: ApiKeyInfo? = null)

/** Loads `/v1/settings` when signed in so Settings can show email when available. */
class SettingsViewModel(
    private val apiClient: ApiClient,
    authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        if (authRepository.isSignedIn()) {
            viewModelScope.launch {
                try {
                    _uiState.value = SettingsUiState(apiKeyInfo = apiClient.service.me())
                } catch (_: Exception) {
                    // Best-effort - Settings already has a sensible fallback display.
                }
            }
        }
    }
}
