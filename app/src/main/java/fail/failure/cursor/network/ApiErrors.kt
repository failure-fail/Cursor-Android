package fail.failure.cursor.network

import retrofit2.HttpException

/**
 * `api.cursor.com`'s Background/Cloud Agents endpoints only accept a personal/service API key
 * (Basic or Bearer per the official docs) - they don't recognize the account session token this
 * app's OAuth-style login flow produces, since that's a wholly different, internal credential
 * for `api2.cursor.sh`'s account/dashboard backend. A 401 here almost always means exactly that
 * mismatch rather than "your login expired," so it gets its own check instead of being lumped in
 * with generic errors.
 */
fun Throwable.isUnauthorized(): Boolean = this is HttpException && code() == 401
