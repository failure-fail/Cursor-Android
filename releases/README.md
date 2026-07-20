# Releases

GitHub Actions isn't currently able to run for this repository (workflow dispatches end in
`startup_failure` - no runner available at the org level), so `.github/workflows/release.yml`
can't auto-publish a GitHub Release yet. Until that's sorted out, compiled release APKs are
committed directly here instead, so they're still downloadable from the repo.

| File | Notes |
| --- | --- |
| `cursor-android-v0.1.0.apk` | Signed with the local debug key (no CI keystore was involved - see `app/build.gradle.kts`'s `signingConfigs`). Minified release build. |

Once Actions has a runner available, dispatching `.github/workflows/release.yml` (or pushing a
`v*` tag on a repo where tag pushes are allowed) will build and publish real GitHub Releases
instead, and this folder can go away.
