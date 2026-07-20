package fail.failure.grok.auth

import android.util.Base64
import org.json.JSONObject

/** Minimal JWT claim peek (unverified) for userId / email display. */
object JwtUtil {

    fun subjectOrNull(jwt: String?): String? = claimString(jwt, "sub")

    fun emailOrNull(jwt: String?): String? = claimString(jwt, "email")

    private fun claimString(jwt: String?, claim: String): String? {
        if (jwt.isNullOrBlank()) return null
        val parts = jwt.split('.')
        if (parts.size < 2) return null
        return try {
            val payload = String(
                Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING),
                Charsets.UTF_8,
            )
            JSONObject(payload).optString(claim).takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}
