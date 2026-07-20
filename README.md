# Cursor for Android (unofficial)

An unofficial native Android client for [Cursor](https://cursor.com), built to mirror how the
official desktop app and iOS app work rather than inventing a new flow: sign in with your real
Cursor account through a browser, then launch and monitor background coding agents from your
phone, with home-screen widgets for a glance at status.

This is a community client, not built or endorsed by Anysphere/Cursor. It talks to Cursor's own
servers using your own credentials; it doesn't proxy through, store, or share your data anywhere
else.

## How sign-in works

Cursor's desktop app doesn't use a typical in-app OAuth form - it opens your browser to
`cursor.com/loginDeepControl` with a PKCE challenge + a uuid, lets you log in on Cursor's own login
page (including SSO/2FA), and then polls `api2.cursor.sh/auth/poll` until that browser session
hands back an access/refresh token pair. This app does exactly that (`auth/PkceUtil.kt`,
`auth/DeepLinkAuthClient.kt`, `auth/AuthRepository.kt`):

1. Generate a uuid + PKCE verifier/challenge (32 random bytes, base64url, no padding).
2. Open `https://cursor.com/loginDeepControl?challenge=...&uuid=...&mode=login&supportsSelectedTeamLogin=true`
   in a Chrome Custom Tab.
3. Poll `https://api2.cursor.sh/auth/poll?uuid=...&verifier=...` (with the same
   `x-ghost-mode` / `x-new-onboarding-completed` / `x-cursor-client-type` / `traceparent` headers
   the desktop app sends) until the browser step completes.
4. Store the resulting session in `EncryptedSharedPreferences` and attach it to every API call as
   a bearer token.

A "sign in with an API key" fallback is also available in Settings for anyone who'd rather use a
scoped personal/service key from the Cursor dashboard instead of a full account session.

**Caveat, stated plainly:** none of this is a published API - there's no spec for it, so it can
break if Cursor changes their internal protocol. It's also had a rockier history than the rest of
this app: an earlier version copied a third-party reimplementation's request shape
(`redirectTarget=cli`, no `supportsSelectedTeamLogin`), which loaded the login page fine but left
it stuck on a permanent "Logging in..." screen without ever reaching a sign-in form - not a
network or account issue, just a subtly wrong request. To get the actual ground truth rather than
guess further, the current version was rewritten directly against the **real desktop app's own
code**: downloaded the official Linux build via `cursor.com/api/download?platform=linux-x64`,
unpacked the `.deb`, and read the unminified-enough `loginLink`/poll implementation straight out of
`resources/app/out/vs/workbench/workbench.desktop.main.js`. That's where the exact query params,
poll headers, and response field names (`authId`, `accessToken`, `refreshToken`,
`selectedTeamId`) above come from - transcribed, not inferred. If sign-in still doesn't complete
after this, the next real diagnostic step is inspecting the login page's own network requests live
(`chrome://inspect` with the phone connected over USB), since at that point the discrepancy is
inside the page itself rather than in what this app sends. The rest of the app - everything under
`v1/agents/...` - is Cursor's officially documented, versioned Background/Cloud Agents API
(`cursor.com/docs/api`), which is what the official iOS app itself is built on for exactly this
"drive an agent from your phone" use case.

## Features

- Launch a background agent against a GitHub repo, with a searchable repo picker, model +
  per-model parameter choice, up to 5 image attachments, and advanced env vars / MCP server config
- **Remote Control**: target a new agent at "My machine" instead of the cloud, with a keep-awake
  toggle, the same handoff the official iOS app calls Remote Control (see the caveat below - the
  exact `env` wire shape isn't publicly documented, so this is a best-effort mirror of the
  behavior described in Cursor's own mobile-app announcement)
- Live-streamed transcript (assistant text, thinking, tool calls) over the run's SSE endpoint, plus
  a git/PR info card once a run finishes
- Follow-up messages to a running/finished agent; cancel, archive/unarchive, delete
- Usage (token counts) and artifacts screens per agent, with presigned-URL downloads
- Agent list with pull-to-refresh and status badges
- Two home-screen widgets (Jetpack Glance): recent agent statuses, and a one-tap "new agent" tile
- An ongoing, updating notification while a run is active (closest Android equivalent to iOS's
  lock-screen Live Activity), backed by WorkManager polling so finished/needs-input agents still
  notify you when the app isn't open
- One-time onboarding screen, haptic feedback on key actions, animated screen transitions

## What's still missing vs. the official iOS app

This is a community project built in one sitting, not a 1:1 port. Known gaps: no inline code-diff
viewer (PRs open in the browser instead), no MCP servers beyond simple name+URL pairs, no custom
subagents UI, and Android has no true Live Activity equivalent (API 36's Live Updates could
replace the ongoing-notification approach once compileSdk moves to 36).

## Project layout

```
app/src/main/java/fail/failure/cursor/
  auth/          PKCE challenge generation, the login/poll/refresh network calls, token storage
  network/       Retrofit service + hand-rolled SSE client for api.cursor.com
  ui/            Compose screens (onboarding, login, agent list, new agent, agent detail,
                 usage/artifacts, settings)
  widget/        Glance app widgets
  notification/  WorkManager poller + ongoing/terminal notification helpers
  onboarding/    One-time intro-seen flag
```

## Building

Requires JDK 17+ and the Android SDK (compileSdk 34). Standard Gradle project:

```
./gradlew assembleDebug
```
