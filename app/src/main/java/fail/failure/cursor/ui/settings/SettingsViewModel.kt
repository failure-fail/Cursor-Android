package fail.failure.cursor.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fail.failure.cursor.auth.AuthRepository
import fail.failure.cursor.network.ApiClient
import fail.failure.cursor.network.model.ApiKeyInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(val apiKeyInfo: ApiKeyInfo? = null)

/** `/v1/me` only recognizes a real API key (same Basic-auth credential every other
 * `api.cursor.com` call needs), so this is best-effort and only attempted when one is set -
 * fills in a real name/email on top of the raw account ID Settings otherwise shows. */
class SettingsViewModel(
    private val apiClient: ApiClient,
    authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        if (!authRepository.session.value.apiKey.isNullOrBlank()) {
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
