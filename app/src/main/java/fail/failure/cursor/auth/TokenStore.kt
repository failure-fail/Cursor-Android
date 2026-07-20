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

/**
 * A browser login that's been started (uuid/verifier/challenge sent to cursor.com) but not yet
 * confirmed complete. Persisted rather than kept only in the ViewModel because a real login -
 * typing a password, a 2FA code, switching apps to check email - can easily outlast the process:
 * Android may reclaim the backgrounded app while the Custom Tab has focus. Without this, the
 * in-memory poll loop just vanishes and the user comes back to what looks like a reset screen
 * with no sign anything was ever attempted.
 */
data class PendingChallenge(
    val uuid: String,
    val verifier: String,
    val challenge: String,
    val createdAtMillis: Long,
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

    fun savePendingChallenge(uuid: String, verifier: String, challenge: String, createdAtMillis: Long) {
        prefs.edit()
            .putString(KEY_PENDING_UUID, uuid)
            .putString(KEY_PENDING_VERIFIER, verifier)
            .putString(KEY_PENDING_CHALLENGE, challenge)
            .putLong(KEY_PENDING_CREATED_AT, createdAtMillis)
            .apply()
    }

    fun readPendingChallenge(): PendingChallenge? {
        val uuid = prefs.getString(KEY_PENDING_UUID, null) ?: return null
        val verifier = prefs.getString(KEY_PENDING_VERIFIER, null) ?: return null
        val challenge = prefs.getString(KEY_PENDING_CHALLENGE, null) ?: return null
        val createdAt = prefs.getLong(KEY_PENDING_CREATED_AT, 0L)
        return PendingChallenge(uuid, verifier, challenge, createdAt)
    }

    fun clearPendingChallenge() {
        prefs.edit()
            .remove(KEY_PENDING_UUID)
            .remove(KEY_PENDING_VERIFIER)
            .remove(KEY_PENDING_CHALLENGE)
            .remove(KEY_PENDING_CREATED_AT)
            .apply()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_PENDING_UUID = "pending_uuid"
        private const val KEY_PENDING_VERIFIER = "pending_verifier"
        private const val KEY_PENDING_CHALLENGE = "pending_challenge"
        private const val KEY_PENDING_CREATED_AT = "pending_created_at"
    }
}
