package fail.failure.grok.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds whichever credential the user signed in with:
 *  - a Grok Build OAuth session (access/refresh from auth.x.ai device login), or
 *  - an `xai-...` API key from console.x.ai (same fallback as `XAI_API_KEY` in the CLI).
 */
data class StoredSession(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val userId: String? = null,
    val email: String? = null,
    val apiKey: String? = null,
    val expiresAtMillis: Long? = null,
)

/**
 * In-flight device-code login. Persisted so Android reclaiming the process while
 * the user is in the browser does not lose the device_code / poll interval.
 */
data class PendingDeviceLogin(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val verificationUriComplete: String?,
    val intervalSeconds: Int,
    val expiresAtMillis: Long,
    val createdAtMillis: Long,
)

class TokenStore(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "grok_auth_secure",
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
        email = prefs.getString(KEY_EMAIL, null),
        apiKey = prefs.getString(KEY_API_KEY, null),
        expiresAtMillis = prefs.getLong(KEY_EXPIRES_AT, 0L).takeIf { it > 0L },
    )

    fun saveAccountSession(
        accessToken: String,
        refreshToken: String?,
        userId: String?,
        email: String? = null,
        expiresInSeconds: Long? = null,
    ) {
        val expiresAt = expiresInSeconds?.let { System.currentTimeMillis() + it * 1000L }
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_EMAIL, email)
            .apply {
                if (expiresAt != null) putLong(KEY_EXPIRES_AT, expiresAt)
                else remove(KEY_EXPIRES_AT)
            }
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

    fun savePendingDeviceLogin(login: PendingDeviceLogin) {
        prefs.edit()
            .putString(KEY_PENDING_DEVICE_CODE, login.deviceCode)
            .putString(KEY_PENDING_USER_CODE, login.userCode)
            .putString(KEY_PENDING_VERIFY_URI, login.verificationUri)
            .putString(KEY_PENDING_VERIFY_URI_COMPLETE, login.verificationUriComplete)
            .putInt(KEY_PENDING_INTERVAL, login.intervalSeconds)
            .putLong(KEY_PENDING_EXPIRES_AT, login.expiresAtMillis)
            .putLong(KEY_PENDING_CREATED_AT, login.createdAtMillis)
            .apply()
    }

    fun readPendingDeviceLogin(): PendingDeviceLogin? {
        val deviceCode = prefs.getString(KEY_PENDING_DEVICE_CODE, null) ?: return null
        val userCode = prefs.getString(KEY_PENDING_USER_CODE, null) ?: return null
        val verificationUri = prefs.getString(KEY_PENDING_VERIFY_URI, null) ?: return null
        return PendingDeviceLogin(
            deviceCode = deviceCode,
            userCode = userCode,
            verificationUri = verificationUri,
            verificationUriComplete = prefs.getString(KEY_PENDING_VERIFY_URI_COMPLETE, null),
            intervalSeconds = prefs.getInt(KEY_PENDING_INTERVAL, 5),
            expiresAtMillis = prefs.getLong(KEY_PENDING_EXPIRES_AT, 0L),
            createdAtMillis = prefs.getLong(KEY_PENDING_CREATED_AT, 0L),
        )
    }

    fun clearPendingDeviceLogin() {
        prefs.edit()
            .remove(KEY_PENDING_DEVICE_CODE)
            .remove(KEY_PENDING_USER_CODE)
            .remove(KEY_PENDING_VERIFY_URI)
            .remove(KEY_PENDING_VERIFY_URI_COMPLETE)
            .remove(KEY_PENDING_INTERVAL)
            .remove(KEY_PENDING_EXPIRES_AT)
            .remove(KEY_PENDING_CREATED_AT)
            .apply()
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EMAIL = "email"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_PENDING_DEVICE_CODE = "pending_device_code"
        private const val KEY_PENDING_USER_CODE = "pending_user_code"
        private const val KEY_PENDING_VERIFY_URI = "pending_verify_uri"
        private const val KEY_PENDING_VERIFY_URI_COMPLETE = "pending_verify_uri_complete"
        private const val KEY_PENDING_INTERVAL = "pending_interval"
        private const val KEY_PENDING_EXPIRES_AT = "pending_expires_at"
        private const val KEY_PENDING_CREATED_AT = "pending_created_at"
    }
}
