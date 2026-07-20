package fail.failure.cursor.auth

import android.util.Base64
import org.json.JSONObject

/** Minimal, best-effort JWT payload reader - used only to show an account identifier in
 * Settings, never for anything security-relevant (the server is the source of truth there). */
object JwtUtil {

    fun subjectOrNull(jwt: String): String? {
        return try {
            val payloadSegment = jwt.split(".").getOrNull(1) ?: return null
            val decoded = Base64.decode(payloadSegment, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            val json = JSONObject(String(decoded, Charsets.UTF_8))
            json.optString("sub").takeIf { it.isNotBlank() }
                ?: json.optString("email").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }
}
