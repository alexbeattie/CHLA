# Provider List Review Fixes

## Status

Implementation and non-device verification complete on
`fix/android-provider-list-review`, based on `f6d4889`.

The focused connected assertions are compiled into the AndroidTest APK. Their
device execution is intentionally deferred to the single integrated run from
the main worktree because a stale external Gradle/UTP cleanup process repeatedly
removed `com.chla.kindd` immediately after successful installs.

## Changes

- Added a List-specific empty state matching the iPhone hierarchy: building
  icon, exact title and description, and a 48dp Refresh action wired to the
  existing retry callback.
- Changed active filter chips to show only their compact localized labels plus a
  close glyph while exposing the localized removal phrase to TalkBack.
- Matched the iPhone search prompt exactly in English and Spanish.
- Replaced the obsolete six-visible-therapy large-text assertion with the
  approved three-tags-plus-overflow contract.
- Added focused coverage for English empty-state copy/Refresh behavior,
  independent phone action behavior, Spanish 320dp/1.3x layout, localized chip
  semantics, and 48dp sort/filter/phone/Refresh targets.

## RED evidence

The regression assertions were written before production changes and compiled
successfully. Direct serial RED execution could not reach assertions:

1. The first install attempt returned `adb: device offline` before installation.
2. After a cold boot, both APK installs succeeded, but logcat showed an external
   `deletePackageX` removing the target during instrumentation startup, which
   killed the test process.

Per root-agent direction, no further isolated device run was attempted.

## Verification

Fresh command:

```text
ANDROID_HOME=/Users/alexbeattie/Library/Android/sdk ./gradlew \
  :app:testDebugUnitTest \
  :app:compileDebugKotlin \
  :app:compileDebugAndroidTestKotlin \
  :app:assembleDebug \
  :app:assembleDebugAndroidTest \
  :app:lintDebug
```

Result: `BUILD SUCCESSFUL`.

- Debug JVM tests: 187 tests, 0 failures, 0 errors, 0 skipped.
- Android production and AndroidTest Kotlin compilation: PASS.
- Debug app and AndroidTest APK assembly: PASS.
- Debug lint: 0 issues.
- `git diff --check`: PASS.

## Connected follow-up

Run these exact methods once from the integrated main worktree:

- `MapListParityTest#providerList_loadingErrorAndEmptyStatesRetainCompactDiscoveryChrome`
- `MapListParityTest#providerCard_phoneActionIsIndependentAndFiresExactlyOnce`
- `DiscoveryControlsTest#activeChips_areRemovable_hideIneffectiveRadius_andClearOnce`
- `TouchedSurfaceAccessibilityTest#providerList_spanishNarrowLargeTextKeepsChromeMetadataAndActionsUsable`
