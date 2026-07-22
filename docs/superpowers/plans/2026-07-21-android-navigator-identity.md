# Android Navigator Identity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild KiNDD as `com.navigator.kindd` and produce a verified, signed first-upload bundle with no legacy institutional branding in Android or Play-facing surfaces.

**Architecture:** Move Gradle identity, all Kotlin package trees, imports, generated-symbol references, ProGuard rules, tests, release documentation, and stale resource identifiers as one atomic namespace migration. Preserve repository/backend/iOS infrastructure paths because they are not packaged into Android. Treat local signing, device verification, Maps restrictions, and Play upload as separate release gates.

**Tech Stack:** Kotlin 1.9.22, Jetpack Compose, Hilt, Android Gradle Plugin 8.9.1, Gradle 8.11.1, Android SDK 36, JUnit 4, Google Maps SDK, Play App Signing.

## Global Constraints

- The permanent Android namespace and application ID are exactly `com.navigator.kindd`.
- Production Android source, packaged artifacts, and Play-facing assets contain no legacy institutional branding.
- Product name remains `KiNDD - NDD Resource Navigator`; short form remains `KiNDD`.
- API access remains environment-driven and the production API base remains `https://api.kinddhelp.com/api`.
- Do not print, commit, replace, or copy the Maps key, keystore password, or signing credentials.
- Reuse the existing upload key; Play App Signing owns the distribution signing key.
- Keep version name `1.4.1` and version code `1` for the first upload of this new package.
- Do not create a Play app under any package other than `com.navigator.kindd`.

---

### Task 1: Migrate the complete Android namespace with an identity contract

**Files:**
- Create: `chla-android/app/src/test/java/com/navigator/kindd/PlayReleaseIdentityContractTest.kt` after the package-tree move; initially place the same file in the current unit-test root so Gradle can run it before migration.
- Modify: `chla-android/app/build.gradle.kts`
- Modify: `chla-android/app/proguard-rules.pro`
- Modify and move: all Kotlin files under `chla-android/app/src/{main,test,androidTest}/java/`
- Modify: `chla-android/app/src/main/res/values/colors.xml`
- Modify: `chla-android/CLAUDE.md`
- Modify: `chla-android/README.md`
- Modify: `chla-android/PLAY_STORE_RELEASE.md`
- Test: `chla-android/app/src/test/java/com/navigator/kindd/PlayReleaseIdentityContractTest.kt`

**Interfaces:**
- Consumes: current Gradle identity, Kotlin source roots, production resources, and Play assets.
- Produces: a coherent `com.navigator.kindd` application plus `PlayReleaseIdentityContractTest` as the durable release identity guard.

- [ ] **Step 1: Write the failing identity contract**

```kotlin
package com.navigator.kindd

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayReleaseIdentityContractTest {
    private val targetPackage = "com.navigator.kindd"
    private val legacyMarker = charArrayOf('c', 'h', 'l', 'a').concatToString()
    private val legacyPackage = "com.$legacyMarker.kindd"

    @Test
    fun gradleAndReleaseRulesUseNavigatorIdentity() {
        val gradle = File("build.gradle.kts").readText()
        val proguard = File("proguard-rules.pro").readText()
        val releaseGuide = File("../PLAY_STORE_RELEASE.md").readText()

        assertTrue(gradle.contains("namespace = \"$targetPackage\""))
        assertTrue(gradle.contains("applicationId = \"$targetPackage\""))
        assertTrue(proguard.contains(targetPackage))
        assertTrue(releaseGuide.contains("`$targetPackage`"))
        assertFalse(gradle.contains(legacyPackage))
        assertFalse(proguard.contains(legacyPackage))
        assertFalse(releaseGuide.contains(legacyPackage))
    }

    @Test
    fun sourceTreesAndStoreAssetsContainNoLegacyMarker() {
        val roots = listOf(
            File("src/main"),
            File("src/test"),
            File("src/androidTest"),
            File("../play-assets")
        )
        val offenders = roots
            .flatMap { root -> root.walkTopDown().filter(File::isFile).toList() }
            .filter { file -> file.extension in setOf("kt", "kts", "xml", "md", "svg", "pro") }
            .filter { file -> file.readText().contains(legacyMarker, ignoreCase = true) }

        assertTrue("Legacy branding remains in: $offenders", offenders.isEmpty())
    }

    @Test
    fun sourcePackagesLiveUnderNavigatorPath() {
        val targetPath = "com/navigator/kindd"
        val legacyPath = "com/$legacyMarker/kindd"
        listOf("main", "test", "androidTest").forEach { sourceSet ->
            assertTrue(File("src/$sourceSet/java/$targetPath").isDirectory)
            assertFalse(File("src/$sourceSet/java/$legacyPath").exists())
        }
    }
}
```

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```bash
cd chla-android
ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew \
  :app:testDebugUnitTest \
  --tests com.navigator.kindd.PlayReleaseIdentityContractTest
```

Expected: FAIL because Gradle, source roots, ProGuard, and release guidance still use the legacy identity.

#### Migration implementation

**Files:**
- Modify: `chla-android/app/build.gradle.kts`
- Modify: `chla-android/app/proguard-rules.pro`
- Modify: all Kotlin files under `chla-android/app/src/main/java/`
- Modify: all Kotlin files under `chla-android/app/src/test/java/`
- Modify: all Kotlin files under `chla-android/app/src/androidTest/java/`
- Move: all three Kotlin package roots to `com/navigator/kindd/`
- Modify: `chla-android/app/src/main/res/values/colors.xml`
- Modify: `chla-android/CLAUDE.md`
- Modify: `chla-android/README.md`
- Modify: `chla-android/PLAY_STORE_RELEASE.md`
- Test: `chla-android/app/src/test/java/com/navigator/kindd/PlayReleaseIdentityContractTest.kt`

**Interfaces:**
- Consumes: the failing identity contract from Task 1.
- Produces: a coherent `com.navigator.kindd` application, unit-test namespace, and instrumentation-test namespace.

- [ ] **Step 1: Replace the namespace and move all Kotlin roots**

Use one mechanical replacement across tracked Android Kotlin/build/rule/docs files, move the main/test/androidTest trees to `com/navigator/kindd`, and update all package declarations, imports, fully qualified references, reflective source paths, and generated `R`/`BuildConfig` references.

- [ ] **Step 2: Remove stale resource branding**

Delete the five unused legacy color identifiers from `app/src/main/res/values/colors.xml`. Do not change the current KiNDD visual tokens or artwork.

- [ ] **Step 3: Update release truth**

Set the package in `CLAUDE.md`, `README.md`, and `PLAY_STORE_RELEASE.md` to `com.navigator.kindd`. State that Maps restrictions need the new package with the upload signer for direct installs and the Play App Signing signer for Play delivery.

- [ ] **Step 4: Run the focused test and verify GREEN**

Run the Task 1 command again.

Expected: PASS.

- [ ] **Step 5: Run unit tests**

```bash
ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew :app:testDebugUnitTest
```

Expected: BUILD SUCCESSFUL with zero failed tests.

- [ ] **Step 6: Commit the identity migration**

```bash
git add chla-android docs/superpowers
git commit -m "refactor(android): adopt navigator package identity"
```

### Task 2: Build and prove the renamed release artifacts

**Files:**
- Verify: `chla-android/app/build/outputs/apk/debug/app-debug.apk`
- Verify: `chla-android/app/build/outputs/apk/release/app-release.apk`
- Verify: `chla-android/app/build/outputs/bundle/release/app-release.aab`

**Interfaces:**
- Consumes: the migrated Android namespace and existing upload-key configuration.
- Produces: signed APK/AAB artifacts whose manifest package is `com.navigator.kindd`.

- [ ] **Step 1: Remove stale generated output and run the release ladder**

```bash
cd chla-android
ANDROID_HOME="$HOME/Library/Android/sdk" ./gradlew clean \
  :app:testDebugUnitTest \
  :app:lintDebug \
  :app:lintRelease \
  :app:assembleDebug \
  :app:assembleRelease
./scripts/verify-launcher-icon-parity.sh
```

Expected: BUILD SUCCESSFUL and launcher parity passes.

- [ ] **Step 2: Build the signed Play bundle**

```bash
./scripts/build-play-bundle.sh
```

Expected: signed bundle path and SHA-256 are printed without exposing credentials.

- [ ] **Step 3: Verify manifest identity and artifact branding**

Use `apkanalyzer`/`bundletool` to require `com.navigator.kindd`, version name `1.4.1`, version code `1`, non-debuggable release output, expected signer, and no legacy branding in decompressed release resources or DEX strings.

- [ ] **Step 4: Verify the connected phone**

Install the renamed release APK, launch `com.navigator.kindd/.MainActivity`, verify it is foreground, and smoke-test first-run onboarding, Maps, location, provider navigation, English/Spanish, and Ask KiNDD. Retire the old package only after the new app launches successfully.

### Task 3: Create the exact Play app and upload to Internal testing

**Files:**
- Upload: `chla-android/app/build/outputs/bundle/release/app-release.aab`
- Reuse: `chla-android/play-assets/`

**Interfaces:**
- Consumes: the verified signed AAB and existing Play assets.
- Produces: a Play Console application fixed to `com.navigator.kindd` with an Internal testing release.

- [ ] **Step 1: Create the Play app**

Use title `KiNDD - NDD Resource Navigator`, package `com.navigator.kindd`, primary language English (United States), App, and Free. The account owner must personally affirm policy, Play App Signing, and export-law declarations.

- [ ] **Step 2: Upload the signed AAB to Internal testing**

Verify that Play reports package `com.navigator.kindd`, version `1.4.1`, and version code `1`. Stop on any identity/signing mismatch.

- [ ] **Step 3: Complete Maps signing restrictions**

Add the Play App Signing SHA-1 for `com.navigator.kindd` to the Android-restricted Maps key without printing or replacing the key. Keep only required signer/package pairs.

- [ ] **Step 4: Install the Play-delivered build**

Repeat Maps, location, provider, phone, website, localization, onboarding, and Ask KiNDD checks from the Internal testing delivery before promotion.

- [ ] **Step 5: Keep production submission gated**

Do not promote or submit to production until store listing, App access, Ads, Content rating, Target audience, News, Data safety, Health apps, privacy policy, and all Console warnings are completed and reviewed.
