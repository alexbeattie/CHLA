# chla-android

KiNDD Android app — Kotlin + Jetpack Compose client for the KiNDD Resource Navigator API.

## Stack

- Kotlin, Jetpack Compose, Hilt DI, Gradle (Android Gradle Plugin 8.9.1, Kotlin 1.9.22)
- compileSdk 36, minSdk 26, targetSdk 36, JVM target 17
- Play package (`applicationId`) `com.navigator.kindd`, Kotlin namespace `com.chla.kindd`; upload key at `~/.android/keys/kindd-upload.jks`, backed up in AWS Secrets Manager `kindd/android/play-upload-key` (restore steps in `PLAY_STORE_RELEASE.md`); API base `https://api.kinddhelp.com/api` (see `local.properties` / `secrets.properties` for local overrides)

## Commands

| Task | Command |
| --- | --- |
| Build (debug) | `./gradlew assembleDebug` |
| Build (Play bundle) | `./scripts/build-play-bundle.sh` |
| Unit tests | `./gradlew test` |
| Instrumented tests | `./gradlew connectedAndroidTest` |

Defer to the repo root `AGENTS.md` for working agreement; log changes in root `FIXES.md`.
