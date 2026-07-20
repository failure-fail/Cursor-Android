package fail.failure.grok.network

import org.json.JSONObject
import retrofit2.HttpException

/**
 * cli-chat-proxy returns plain `{"error":"..."}` (or nested message) on auth /
 * validation failures. Prefer that over Retrofit's generic HTTP status text.
 */
fun Throwable.grokApiErrorMessage(): String? {
    val http = this as? HttpException ?: return null
    val body = try {
        http.response()?.errorBody()?.string()
    } catch (_: Exception) {
        null
    } ?: return null
    return try {
        val obj = JSONObject(body)
        when {
            obj.has("error") && obj.opt("error") is String -> obj.getString("error")
            obj.optJSONObject("error")?.has("message") == true ->
                obj.getJSONObject("error").getString("message")
            obj.has("message") -> obj.getString("message")
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

fun Throwable.isUnauthorized(): Boolean {
    val http = this as? HttpException ?: return false
    return http.code() == 401 || http.code() == 403
}
