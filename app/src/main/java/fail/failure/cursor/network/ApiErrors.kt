package fail.failure.cursor.network

import retrofit2.HttpException

/**
 * `api.cursor.com`'s Background/Cloud Agents endpoints only accept a personal/service API key
 * (HTTP Basic, key as username/blank password - `curl -u KEY:` per the official docs) - they
 * don't recognize the account session token this app's OAuth-style login flow produces, since
 * that's a wholly different, internal credential for `api2.cursor.sh`'s account/dashboard
 * backend. Both 401 (no/invalid credential) and 403 (a key that's present but rejected, e.g. an
 * account-token fallback sent as Bearer instead of a real key) point at the same fix - enter a
 * real API key - so both route to the same prompt instead of a generic error.
 */
fun Throwable.isUnauthorized(): Boolean = this is HttpException && (code() == 401 || code() == 403)
