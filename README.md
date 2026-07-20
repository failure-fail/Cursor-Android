# Cursor for Android (unofficial)

An unofficial native Android client for [Cursor](https://cursor.com), built to mirror how the
official desktop app and iOS app work rather than inventing a new flow: sign in with your real
Cursor account through a browser, then launch and monitor background coding agents from your
phone, with home-screen widgets for a glance at status.

This is a community client, not built or endorsed by Anysphere/Cursor. It talks to Cursor's own
servers using your own credentials; it doesn't proxy through, store, or share your data anywhere
else.

## How sign-in works

Cursor's desktop app and CLI (`cursor-agent login`) don't use a typical in-app OAuth form - they
open your browser to `cursor.com/loginDeepControl` with a PKCE challenge + a uuid, let you log in
on Cursor's own login page (including SSO/2FA), and then poll `api2.cursor.sh/auth/poll` until
that browser session hands back an access/refresh token pair. This app does exactly that
(`auth/PkceUtil.kt`, `auth/DeepLinkAuthClient.kt`, `auth/AuthRepository.kt`):

1. Generate a uuid + PKCE verifier/challenge.
2. Open `https://www.cursor.com/loginDeepControl?challenge=...&uuid=...&mode=login` in a Chrome
   Custom Tab.
3. Poll `https://api2.cursor.sh/auth/poll` until the browser step completes.
4. Store the resulting session in `EncryptedSharedPreferences` and attach it to every API call as
   a bearer token (and, for endpoints that expect it, the `WorkosCursorSessionToken` cookie the
   web dashboard uses).

A "sign in with an API key" fallback is also available in Settings for anyone who'd rather use a
scoped personal/service key from the Cursor dashboard instead of a full account session.

**Caveat, stated plainly:** the `/auth/poll` and `/oauth/token` request/response shapes come from
community reverse-engineering of the desktop app's own traffic (there's no public spec for them),
so they can break if Cursor changes that internal protocol. The rest of the app - everything under
`v1/agents/...` - is Cursor's officially documented, versioned Background/Cloud Agents API
(`cursor.com/docs/api`), which is what the official iOS app itself is built on for exactly this
"drive an agent from your phone" use case.

## Features

- Launch a background agent against a GitHub repo with a prompt + model choice
- Live-streamed transcript (assistant text, thinking, tool calls) over the run's SSE endpoint
- Follow-up messages to a running/finished agent
- Agent list with status, usage, and cancel
- Two home-screen widgets (Jetpack Glance): recent agent statuses, and a one-tap "new agent" tile
- Background polling (WorkManager) + local notifications when an agent finishes or needs input

## Project layout

```
app/src/main/java/fail/failure/cursor/
  auth/        PKCE challenge generation, the login/poll/refresh network calls, token storage
  network/     Retrofit service + hand-rolled SSE client for api.cursor.com
  ui/          Compose screens (login, agent list, new agent, agent detail, settings)
  widget/      Glance app widgets
  notification/ WorkManager poller + notification channel
```

## Building

Requires JDK 17+ and the Android SDK (compileSdk 34). Standard Gradle project:

```
./gradlew assembleDebug
```
