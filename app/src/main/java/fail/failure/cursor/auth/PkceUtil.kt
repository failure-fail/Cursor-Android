package fail.failure.cursor.auth

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

/**
 * Generates the uuid + PKCE verifier/challenge pair used by Cursor's browser-based
 * account login (the same flow the desktop app and `cursor-agent login` CLI use):
 * the client picks a uuid and a random verifier, sends sha256(verifier) as the
 * "challenge" to https://cursor.com/loginDeepControl, and later exchanges the
 * verifier for a token by polling api2.cursor.sh/auth/poll?uuid=...&verifier=...
 */
object PkceUtil {

    data class LoginChallenge(val uuid: String, val verifier: String, val challenge: String)

    fun generate(): LoginChallenge {
        val uuid = UUID.randomUUID().toString()
        val verifier = randomUrlSafeString(43)
        val challenge = sha256Base64Url(verifier)
        return LoginChallenge(uuid = uuid, verifier = verifier, challenge = challenge)
    }

    private fun randomUrlSafeString(length: Int): String {
        val random = SecureRandom()
        val bytes = ByteArray(length)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            .take(length)
    }

    private fun sha256Base64Url(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }
}
