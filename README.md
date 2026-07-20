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
