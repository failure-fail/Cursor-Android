# Grok for Android (unofficial)

An unofficial native Android client for [Grok Build](https://docs.x.ai/build/overview),
rebased from the Cursor-Android community client and rewired to SpaceXAI's own
OAuth + cli-chat-proxy backend (as implemented in
[`xai-org/grok-build`](https://github.com/xai-org/grok-build)).

This is a community client, not built or endorsed by xAI. It talks to xAI's
servers using your own credentials.

## How sign-in works

Grok Build's CLI defaults to SpaceXAI OAuth at `auth.x.ai`. On a phone the
closest match is the same RFC 8628 **device-code** flow the CLI exposes as
`grok login --device-auth` (`auth/GrokOAuthClient.kt`, `auth/AuthRepository.kt`):

1. `POST https://auth.x.ai/oauth2/device/code` with the public Grok Build
   client id (`b1a00492-073a-47ea-816f-4c329264a828`) and CLI scopes
   (`openid profile email offline_access grok-cli:access api:access …`).
2. Show the `user_code` and open `verification_uri_complete` in a Custom Tab
   (`accounts.x.ai/oauth2/device?...`).
3. Poll `POST https://auth.x.ai/oauth2/token` with
   `grant_type=urn:ietf:params:oauth:grant-type:device_code` until approved.
4. Store the access/refresh token pair in `EncryptedSharedPreferences` and
   attach it to every cli-chat-proxy call as
   `Authorization: Bearer …` + `X-XAI-Token-Auth: xai-grok-cli`
   (same headers the Grok Build CLI sends).

You can also paste an `xai-...` API key from [console.x.ai](https://console.x.ai)
as a fallback (parity with `XAI_API_KEY` in the CLI).

## Backend

| Concern | Endpoint |
|---------|----------|
| OAuth issuer | `https://auth.x.ai` |
| Inference / sandbox proxy | `https://cli-chat-proxy.grok.com/v1/` |
| Models | `GET /v1/models` |
| Settings | `GET /v1/settings` |
| Sandbox environments | `GET/POST /v1/sandbox/environments` |
| Start session | `POST /v1/sandbox/sessions/start` |

The agent list / create UI maps onto **sandbox environments** (Grok Build's
cloud-agent surface), not Cursor's Background Agents API.

## Project layout

```
app/src/main/java/fail/failure/grok/
  auth/          Device-code OAuth (auth.x.ai), token storage, refresh
  network/       Retrofit + facade over cli-chat-proxy sandbox/models
  ui/            Compose screens
  widget/        Glance app widgets
  notification/  WorkManager poller + notifications
```

## Building

Requires JDK 17+ and the Android SDK (compileSdk 34):

```bash
./gradlew :app:assembleDebug
```

## Branch note

Developed on `cursor/grok-build-oauth-backend-5cac` against the former
Cursor-Android codebase; package id is now `fail.failure.grok`.
