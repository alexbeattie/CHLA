# Task 7B Report: Literal-parity Provider List UI

## Status

Implementation and focused verification complete.

## Files

- `chla-android/app/src/main/java/com/chla/kindd/ui/screens/ProviderListScreen.kt`
- `chla-android/app/src/main/java/com/chla/kindd/ui/providers/ProviderCard.kt`
- `chla-android/app/src/main/java/com/chla/kindd/ui/discovery/KiNDDSearchOverlay.kt`
- `chla-android/app/src/main/java/com/chla/kindd/ui/discovery/ActiveFilterChips.kt`
- `chla-android/app/src/androidTest/java/com/chla/kindd/ui/discovery/MapListParityTest.kt`
- `chla-android/app/src/main/res/values/strings.xml`
- `chla-android/app/src/main/res/values-es/strings.xml`

## RED evidence

Seven focused `MapListParityTest` tests were compiled into the instrumentation APK and run directly against the pre-change target APK. Four new List contract tests failed because the compact header/actions, rich provider metadata, and List clearance item did not exist. The existing Map/List shared-state tests remained intact.

The first Gradle connected attempt also exposed a shared-emulator package-manager race (`Unable to find instrumentation target package`); direct serial APK installation was used afterward so product assertions and emulator transport failures remained distinguishable.

## GREEN evidence

- `:app:testDebugUnitTest --tests com.chla.kindd.ui.screens.ProviderListViewModelTest`: PASS
- `:app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:assembleDebug`: PASS
- `git diff --check`: PASS
- `:app:assembleDebug :app:assembleDebugAndroidTest`: PASS
- Direct serial instrumentation on `emulator-5554`: `MapListParityTest` PASS, 7/7 tests.

## Implementation

- Replaced the solid Material top app bar and vertical form stack with the grouped neutral canvas, restrained indigo wash, compact Resources heading, and 48dp sort/filter actions.
- Added a reusable 16dp compact discovery search surface.
- Converted active filters to one horizontal, role-colored row.
- Added neutral 20dp cards with two-line names, real provider type, compact distance, safe two-line normalized address, three localized therapy tags plus overflow, a real phone action, and a real regional-center badge.
- Preserved `DiscoveryState`, `DiscoveryUiActions`, sorting, filters, stale results during refresh, and provider identity.
- Added a scrollable bottom clearance item that composes with the shared floating-navigation content inset.

## Self-review

- No repositories, provider models/formatters, navigation, theme, Home, onboarding, Regions, Chat, or Settings production code was changed by this task.
- No regional center is inferred.
- Blank phone/type/center values are omitted.
- Icon-only actions retain localized descriptions and 48dp targets.
- New copy exists in English and Spanish.
- The existing broad `TouchedSurfaceAccessibilityTest.providerList_spanishLargeTextWrapsAndLocalizesEveryCanonicalTherapy` still expects six visible therapy tags; the approved List contract now deliberately renders three plus overflow, so that legacy assertion must be updated in the final integration test wave.

## Screenshot

Not captured in this slice because the checked-in live API state currently returns the same empty-list composition shown by the iPhone reference. Final deterministic populated-state and live empty-state screenshots belong to Task 9 acceptance.

## Commit

`feat(android): match iPhone provider list` (this report is committed with the implementation).
