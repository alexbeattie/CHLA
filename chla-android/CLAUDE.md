# chla-android

KiNDD Android app — Kotlin + Jetpack Compose client for the KiNDD Resource Navigator API.

## Stack

- Kotlin, Jetpack Compose, Hilt DI, Gradle (Android Gradle Plugin 8.2.2, Kotlin 1.9.22)
- compileSdk 34, minSdk 26, targetSdk 34, JVM target 17
- Package `com.chla.kindd`; API base `https://api.kinddhelp.com/api` (see `local.properties` / `secrets.properties` for local overrides)

## Commands

| Task | Command |
| --- | --- |
| Build (debug) | `./gradlew assembleDebug` |
| Build (release) | `./gradlew assembleRelease` |
| Unit tests | `./gradlew test` |
| Instrumented tests | `./gradlew connectedAndroidTest` |

Defer to the repo root `AGENTS.md` for working agreement; log changes in root `FIXES.md`.
