package fail.failure.cursor.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds whichever credential the user signed in with:
 *  - a full account session (accessToken/refreshToken) from the browser login flow, or
 *  - a personal/service API key pasted in Settings (crsr_...), used as a fallback and
 *    for the officially documented api.cursor.com endpoints.
 */
data class StoredSession(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val userId: String? = null,
    val apiKey: String? = null,
)

class TokenStore(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "cursor_auth_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val _session = MutableStateFlow(readSession())
    val session: StateFlow<StoredSession> = _session.asStateFlow()

    private fun readSession() = StoredSession(
        accessToken = prefs.getString(KEY_ACCESS_TOKEN, null),
        refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null),
        userId = prefs.getString(KEY_USER_ID, null),
        apiKey = prefs.getString(KEY_API_KEY, null),
    )

    fun saveAccountSession(accessToken: String, refreshToken: String?, userId: String?) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_USER_ID, userId)
            .apply()
        _session.value = readSession()
    }

    fun saveApiKey(apiKey: String) {
        prefs.edit().putString(KEY_API_KEY, apiKey).apply()
        _session.value = readSession()
    }

    fun clear() {
        prefs.edit().clear().apply()
        _session.value = readSession()
    }

    fun isSignedIn(): Boolean {
        val s = _session.value
        return !s.accessToken.isNullOrBlank() || !s.apiKey.isNullOrBlank()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_API_KEY = "api_key"
    }
}
