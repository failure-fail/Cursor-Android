package fail.failure.cursor.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

/**
 * `api.cursor.com`'s Background/Cloud Agents endpoints only accept a personal/service API key
 * (HTTP Basic, key as username/blank password - `curl -u KEY:` per the official docs) - they
 * don't recognize the account session token this app's OAuth-style login flow produces, since
 * that's a wholly different, internal credential for `api2.cursor.sh`'s account/dashboard
 * backend. Both 401 (no/invalid credential) and 403 (a key that's present but rejected) point at
 * the same prompt, but they're not always the same *reason* - a 403 can mean a perfectly valid
 * key on a plan that doesn't include Cloud Agents (confirmed live: `{"error":{"code":
 * "plan_required","message":"Cloud Agent is not available for free users. Please upgrade to
 * Pro."}}`), which "double-check it and try again" would actively mislead someone about. Surface
 * the server's own message when there is one instead of guessing.
 */
fun Throwable.isUnauthorized(): Boolean = this is HttpException && (code() == 401 || code() == 403)

fun Throwable.cursorApiErrorMessage(): String? {
    if (this !is HttpException) return null
    val body = response()?.errorBody()?.string() ?: return null
    return try {
        Json.parseToJsonElement(body).jsonObject["error"]
            ?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
    } catch (_: Exception) {
        null
    }
}
