---
name: moneytrack
description: Project guide for MoneyTrack, the private Android finance tracker (Kotlin, Compose, Room, Firebase Auth) published to Google Play and GitHub. Use before changing code, building or releasing, editing privacy or store docs, touching the Vercel site, or answering questions about this repo. Keeps work inside the project's privacy promises, release process and conventions.
---

# MoneyTrack project guide

Read this before acting. If a request conflicts with an invariant below, stop and ask the user.

## What it is
A simple, private personal finance tracker for Android. Package `com.moneytrack.app`, GitHub `IOREBIYI/money-track`. Distributed two ways: Google Play (Play App Signing) and a sideload APK on GitHub Releases (signed with the upload key). Current version is in `app/build.gradle.kts` (`versionCode`/`versionName`); never quote a version from memory.

## Invariants (do not break)
1. **Privacy promise:** transactions and budgets live only in the on-device Room DB and are never uploaded. Only email and user ID go to Firebase Auth. No ads, no analytics, no trackers, no new network calls or SDKs without asking. Any change that alters data collection must update `PRIVACY.md`, `ACCOUNT_DELETION.md`, `site/privacy`, `store/play-data-safety.md` and the README in the same change.
2. **Secrets never committed:** `keystore.properties`, `*.jks`, `keystore/`, `app/google-services.json`, `store/production-access-answers.md` are git-ignored. Never print, copy or commit their contents. Never stage with `git add -A` or `git add .` without checking `git status` first.
3. **Release builds need `app/google-services.json`.** The Gradle check at the bottom of `app/build.gradle.kts` deliberately fails without it. Do not weaken it. Without `keystore.properties` the release is unsigned, so never publish that.
4. **Every upload needs a higher `versionCode`** than the last one on Play. Bump both fields together.
5. **Currency is display only.** No conversion logic.
6. **Do not commit, push, tag, publish a release or upload anywhere unless the user asked for that action in this turn.**

## Architecture
MVVM. Kotlin (JVM 17), Jetpack Compose + Material 3, Room 2.7.2 with KSP, Firebase Auth (email/password), coroutines/Flow. minSdk 26, compile/target SDK 36. R8 minify + shrink on release.

```
app/src/main/java/com/moneytrack/app/
  MainActivity.kt, MoneyApp.kt   entry point, application class
  MoneyViewModel.kt              transactions, budgets, reports, settings state
  auth/                          AuthRepository (Firebase), AuthViewModel, AuthModels (validation, error mapping)
  data/                          Database.kt (Room + DAO), Csv.kt (export/restore), Currencies.kt (39 currencies)
  ui/                            AppRoot, AuthScreens, Transactions/Budgets/Reports/Settings screens, Theme
app/src/test/                    JUnit 4: Csv, Currencies, AuthValidation, AuthErrorMapping, PercentLabel
app/src/debug/                   debug-only manifest + network config (Firebase Auth emulator)
site/                            static Vercel site: landing, /privacy, /account-deletion
store/                           Play icon, feature graphic, screenshots, play-data-safety.md
```

Conventions: follow the existing ledger-style theme in `ui/Theme.kt` (light + dark, `values-night`). New logic that can be tested without Android goes in plain Kotlin with a unit test next to the existing ones. Firebase exception classes need `isReturnDefaultValues = true` (already set).

## Commands (run from the repo root; on Windows use `./gradlew.bat` or `./gradlew` in Git Bash)
| Goal | Command |
|---|---|
| Unit tests | `./gradlew testDebugUnitTest` |
| Debug APK | `./gradlew assembleDebug` (runs without Firebase config, shows "Accounts aren't set up") |
| Debug against local auth emulator | `npx firebase-tools emulators:start --only auth --project demo-moneytrack`, then `./gradlew assembleDebug -PauthEmulator=true` |
| Signed Play bundle | `./gradlew bundleRelease` -> `app/build/outputs/bundle/release/app-release.aab` |
| Signed APK | `./gradlew assembleRelease` -> `app/build/outputs/apk/release/app-release.apk` |

Run `testDebugUnitTest` before any release build and after any change to `auth/`, `data/Csv.kt` or `data/Currencies.kt`.

## Release checklist (in order)
1. Working tree clean, on `main`, tests pass.
2. Bump `versionCode` and `versionName` in `app/build.gradle.kts`; commit as `Bump version to X.Y.Z (versionCode N)`.
3. `./gradlew bundleRelease assembleRelease`. Confirm the AAB timestamp is fresh (Gradle can reuse an up-to-date one) and `output-metadata.json` shows the right version.
4. Tag `vX.Y.Z` and push it. Check `git tag -l` and `gh release list` first: tags and releases often already exist; do not recreate or overwrite them, and do not replace a published APK (its SHA-256 is in the release notes and people have downloaded it).
5. GitHub release titled `MoneyTrack X.Y.Z`: attach the APK renamed `MoneyTrack-X.Y.Z.apk`, put its SHA-256 in the notes, keep the uninstall-before-switching-between-Play-and-APK warning (different signing keys), link the `compare/vPREV...vX.Y.Z` changelog.
6. Play Console: upload the AAB to internal/closed track first, add release notes (under 500 chars), re-check Data safety, privacy policy URL, account-deletion URL, and **App access** (reviewers need a working test account because the app requires sign-in). Playbook: README "Releasing to Google Play" and `store/play-data-safety.md`.
7. After going live, update the README Download section (the commented Play link).

## Known inconsistencies to verify, not assume
- The canonical privacy and deletion URLs are the Vercel site: `https://moneytrack-wheat-nine.vercel.app/privacy` and `/account-deletion` (README, `store/play-data-safety.md` and the 1.3.0 release notes now agree). The shipped 1.3.0 app still has `PRIVACY_URL` in `ui/AuthScreens.kt` pointing at the GitHub `PRIVACY.md`; switch it in the next release and confirm with the user which URLs are registered in Play Console.
- The Vercel project has no `vercel.json` or `.vercel` link in the repo, so ask how the site is deployed before changing deployment settings. Content edits live in `site/`; keep `site/privacy` in sync with `PRIVACY.md`.

## Working style for this repo
- Make the smallest change that satisfies the request; do not refactor, rename, upgrade dependencies or change the theme unless asked.
- Check `git status` and `git log` before assuming something is not done. Past work (tags, releases, merged branches) is usually already in place.
- Report results plainly: what was built or changed, what was verified, what was not.
