# Android Google Play final review fixes report

## Status

Implementation and required verification are complete on
`feature/android-persistent-discovery` in
`/Users/alexbeattie/Developer/CHLA-android-persistent-discovery`.

Implementation commit: `7da81ab` (`fix(android): address final Play review`).

Documentation review follow-up: committed with this updated report as a focused
follow-up to `7da81ab`.

No push, upload, merge, deployment, signing, bundle task, or secret access was
performed. The existing `app-release.aab` was not used as verification and was
not rebuilt or overwritten.

## Changes

### Coarse-only foreground location

- Removed `ACCESS_FINE_LOCATION` from the Android manifest. The release keeps
  `ACCESS_COARSE_LOCATION` and does not declare background location.
- Preserved the existing optional location-hardware declarations so devices
  without location hardware are not filtered from Play eligibility.
- Added a single `LocationRequestPolicy` used by permission checks, one-shot
  current-location requests, and streaming location updates.
- The policy requires only foreground coarse permission and uses
  `PRIORITY_BALANCED_POWER_ACCURACY`, matching the permission exposed by the UI.
- Replaced the manifest substring assertion with semantic XML checks against
  Gradle's generated merged release manifest.

### Map attribution and external actions

- Preserved the existing runtime path that measures overlay cards and forwards
  resolved `contentPadding` to Google Maps.
- Added a shared pure padding calculation used by the Home and onboarding map
  hosts. It reserves measured overlay height plus 20 dp clearance and returns no
  padding for an absent or separately rendered card.
- Replaced `MapAttributionContractTest` source-string checks with behavior-level
  tests of that production calculation.
- Removed the untracked `ProviderDetailExternalActionContractTest` source-string
  test. The guarded Provider Detail wiring remains on disk unchanged, and the
  existing `ExternalIntentContractTest` continues to cover URI sanitization,
  resolver failure, launch failure, and successful launch behavior.

### Play documentation

- Updated `play-assets/data-safety-draft.md` to state that approximate location
  is collected, device permission is optional, ZIP is required to complete the
  profile, and precise/background location is not collected by this release.
- Updated `PLAY_STORE_RELEASE.md` to distinguish onboarding's on-device
  coarse-fix-to-ZIP conversion from user-initiated nearby provider discovery,
  which is the only flow that sends raw coarse coordinates.
- Reworded the draft data-sale answer as requiring organization confirmation
  because code does not establish that legal answer.
- Follow-up review changed the Google Maps-related `Shared` cells for app
  interactions, device or other IDs, and crash logs/diagnostics from definitive
  `Yes` values to `Confirm`, with explicit Play service-provider confirmation.
- Follow-up review replaced the unsupported statement that the app is not a
  regulated medical device with a privacy-owner/counsel confirmation gate.

## RED/GREEN evidence

All Gradle commands used:

```text
ANDROID_HOME=/Users/alexbeattie/Library/Android/sdk
JAVA_HOME=/Applications/Android Studio.app/Contents/jbr/Contents/Home
```

1. RED: `PlayReleaseManifestContractTest` failed because the release manifest
   still contained `ACCESS_FINE_LOCATION` (2 tests, 1 failed).
2. RED: `LocationRequestPolicyTest` failed compilation because the intended
   policy seam did not exist.
3. GREEN: both permission suites passed after the manifest and request-policy
   changes.
4. RED: `MapAttributionContractTest` failed compilation because the intended
   pure padding seam did not exist.
5. GREEN: the map behavior suite passed after adding and wiring the production
   padding calculation.
6. Artifact regression check: after temporarily restoring fine permission, the
   generated merged-manifest test failed specifically on the coarse-only
   assertion; removing it again restored GREEN.

Focused GREEN command covered:

- `PlayReleaseManifestContractTest`
- `LocationRequestPolicyTest`
- `MapAttributionContractTest`
- `ExternalIntentContractTest`

Result: `BUILD SUCCESSFUL`, 35 actionable tasks.

## Required final verification

```text
./gradlew :app:testReleaseUnitTest :app:lintRelease
BUILD SUCCESSFUL in 24s
42 actionable tasks: 8 executed, 34 up-to-date
```

`git diff --check` exited 0 with no output.

The documentation follow-up was read back with its adjacent disclosure sections,
checked to ensure no definitive Google Maps `Shared: Yes` cells or unsupported
medical-device conclusion remained, and passed `git diff --check`.

The Gradle run emitted only the repository's existing deprecation notice; unit
tests and release lint completed without failures. No AAB-producing Gradle task
was invoked.

## Commit scope and pre-existing dirty work

Only task-related paths were committed. The worktree still contains unrelated
dirty release work that remains unstaged.

The following pre-existing changes are inseparable from this task and are
included in the task commit:

- Optional location-hardware declarations already present in
  `AndroidManifest.xml`.
- The existing Google Maps runtime padding propagation in `HomeMapHero.kt`,
  `RegionalCenterStep.kt`, `RegionalCenterMapSurface.kt`, and
  `RegionalCenterServiceAreaMap.kt`; this task replaced its brittle test and
  wired the shared behavior seam.
- The previously untracked Play release documents; this task commits only
  `PLAY_STORE_RELEASE.md` and `play-assets/data-safety-draft.md` from those
  release materials because both required correction.

The guarded `ProviderDetailScreen.kt` change remains unstaged with the other
pre-existing dirty release work. Its brittle untracked test was removed rather
than committed.

## Remaining handoff

- Controller should review the focused commit and this report.
- Controller should run the signing wrapper and recapture screenshots only
  after review.
- Google Play upload, Console declarations, and publication remain external
  steps and were not attempted here.
