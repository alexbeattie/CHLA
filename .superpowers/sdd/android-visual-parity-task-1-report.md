# Task 1 Report: KiNDD visual foundation and floating navigation

Status: complete

Implementation commit: `d2beb797da57774543067b43a2368adf4782a0c9`

Subject: `feat(android): add floating KiNDD navigation`

## RED

The focused navigation contract was added to
`AppEntryNavigationTest.kt` before any production file was changed.

Exact valid RED command:

```bash
ANDROID_HOME="/Users/alexbeattie/Library/Android/sdk" \
ANDROID_SERIAL=emulator-5554 \
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.chla.kindd.ui.navigation.AppEntryNavigationTest \
  --no-daemon
```

Expected RED observed on `Pixel_8(AVD) - 16`: 15 tests ran; 10 existing
tests passed and five new contract tests failed because the old implementation
had no Regions/Ask/More actions, no floating/root overlay tags, and no Chat
modal sheet. Representative failures were missing `bottom_nav_regions`,
`bottom_nav_ask`, `chat_modal_sheet`, and `main_nav_root`; the old Home item also
failed the new icon-only accessible-description assertion.

An earlier invocation without `ANDROID_HOME` stopped before compilation with
the local SDK-location error. It was not accepted as RED evidence; the command
above compiled the tests, installed both APKs, ran the class, and produced the
five expected assertion failures.

## Implementation

- `chla-android/app/src/main/java/com/chla/kindd/ui/theme/KiNDDVisualTokens.kt`
  adds exact brand gradients, adaptive top wash, geometry, page inset, and
  section-rhythm tokens.
- `chla-android/app/src/main/java/com/chla/kindd/ui/theme/KiNDDComponents.kt`
  adds reusable frosted surfaces, critically damped press feedback with a
  no-spatial-motion fallback, gradient/secondary capsules, compact icon
  actions, and card surfaces.
- `chla-android/app/src/main/java/com/chla/kindd/ui/theme/Theme.kt` keeps dynamic
  color disabled and maps Material shapes to the KiNDD geometry roles.
- `chla-android/app/src/main/java/com/chla/kindd/ui/theme/Type.kt` provides the
  requested system-font 34/28/22/20/17/15/12sp hierarchy with size-specific
  tracking.
- `chla-android/app/src/main/java/com/chla/kindd/ui/navigation/KiNDDFloatingNavigation.kt`
  implements the icon-only Home, Map, Ask, Regions, List, More capsule with
  48dp targets, localized descriptions, selected treatment, AI gradient, glass
  highlight, and paired depth.
- `chla-android/app/src/main/java/com/chla/kindd/ui/navigation/KINDDNavHost.kt`
  overlays the capsule over a full-size NavHost, applies navigation-bar and
  horizontal safe insets, makes Regions first class and More route to Settings,
  and presents Chat in a dismissible 28dp modal sheet over the current route.
  Prompt-aware internal launches pass the typed fixed key directly to sheet
  content; the old Chat route remains as a compatibility bridge and never puts
  localized prompt text in navigation state.
- `chla-android/app/src/main/res/values/strings.xml` and
  `chla-android/app/src/main/res/values-es/strings.xml` add English and Spanish
  accessible names for Ask, Regions, and More.
- `chla-android/app/src/androidTest/java/com/chla/kindd/ui/navigation/AppEntryNavigationTest.kt`
  covers all six targets, descriptions, icon-only semantics, 48dp bounds,
  capsule width/overlay placement, route/state behavior, sheet dismissal, and
  exactly-once typed-prompt delivery.

## GREEN and build evidence

Focused connected navigation gate, using the exact RED command above:

- Result: `BUILD SUCCESSFUL` in 41 seconds.
- Device: `emulator-5554`, reported as `Pixel_8(AVD) - 16`.
- Result XML: 15 tests, 0 failures, 0 errors, 0 skipped.

Production and instrumentation source compilation:

```bash
ANDROID_HOME="/Users/alexbeattie/Library/Android/sdk" \
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
./gradlew :app:compileDebugKotlin \
  :app:compileDebugAndroidTestKotlin \
  --no-daemon
```

Result: `BUILD SUCCESSFUL`.

Fresh combined unit/source/APK/lint gate:

```bash
ANDROID_HOME="/Users/alexbeattie/Library/Android/sdk" \
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
./gradlew :app:testDebugUnitTest \
  :app:compileDebugUnitTestSources \
  :app:compileDebugAndroidTestSources \
  :app:assembleDebug \
  :app:lintDebug \
  --no-daemon
```

Result: `BUILD SUCCESSFUL` in 37 seconds. The unit result contained 163 tests,
0 failures, 0 errors, and 0 skipped. Debug unit and instrumentation sources
compiled, the debug APK assembled, and lint completed with 0 errors and 87
existing warnings. No lint issue references the touched navigation or theme
Kotlin files.

## Self-review

- Re-read the binding brief and iPhone Home, Map, and Chat captures after the
  GREEN run. The capsule order, icon-only presentation, selected indigo,
  dedicated AI center action, glass/depth treatment, overlay behavior, and
  near-full-height rounded Chat sheet match the Task 1 contract.
- `git diff --cached --check` passed before the implementation commit.
- The implementation commit contains exactly nine Task 1 files. Concurrent
  `ProviderJsonTest.kt`, `ui/map/`, and `ProviderAddressFormatterTest.kt` work
  was not staged or modified.
- The stable legacy Home/Map/List test tags were retained; Ask, Regions, More,
  capsule, root, and sheet tags were added without exposing visible labels.
- Internal Chat launches no longer mutate the navigation destination. Back
  dismisses the sheet first and the underlying destination remains selected.

## Concerns and follow-up

- No Task 1 functional blocker remains. The SDK toolchain still prints the
  pre-existing SDK XML version warning; it did not affect compilation, tests,
  assembly, lint, or device execution.
- This task establishes reusable primitives and navigation. Individual Home,
  Map, Regions, List, More, and Chat screen-body literal visual parity remains
  intentionally outside this first foundation slice.
- The connected Compose suite proves sizing, semantics, overlay geometry, and
  behavior. A separate end-to-end screenshot-diff gate was not introduced in
  this task.

## Review remediation

Review remediation implementation: this commit, based on `5e448d4`.

The four follow-up findings were reproduced with tests before production
changes:

- The focused unit RED failed at `compileDebugUnitTestKotlin` because the
  required `KiNDDPressVisual` contract did not exist. The existing
  ViewModel-lifetime prompt gate also rejected a second identical typed prompt.
- The focused connected RED ran 19 tests and reported three expected failures:
  the Home/list-style bottom control overlapped the floating capsule, reopening
  an identical typed prompt never issued the second request, and the Ask target
  exposed a filled color circle instead of a neutral glass container.
- A later integration-contract RED proved Home was being host-shrunk: its
  destination bottom was `1995px` while the root bottom was `2205px`. Home owns
  its explicit 190dp scroll and 94dp Ask offsets, so the host now leaves Home
  and the full-bleed Map at full height while reserving navigation-bar-aware
  clearance for List, Regions, Settings, and other non-fullscreen destinations.
- The final pressed-surface test was also mutation-checked: temporarily moving
  press feedback back after the fill reproduced the defect on the primary
  capsule (`press_primary` edge pixel remained `-10131727`), and restoring the
  press layer ahead of the surface returned the test to GREEN.

The remediation makes prompt dispatch presentation-scoped in `ChatScreen`
instead of Activity/ViewModel-scoped, moves the press layer before every shared
control's shadow/fill/border, keeps selected icon fills inside the shared icon
control, and renders the Ask sparkle as a violet-to-pink `SrcIn` mask in an
offscreen compositing layer over glass. Connected pixel assertions cover both
the neutral Ask container/colored glyph and the drawn edges of the primary,
secondary, icon, and card surfaces while pressed.

Fresh GREEN evidence:

```bash
ANDROID_HOME="/Users/alexbeattie/Library/Android/sdk" \
ANDROID_SERIAL=emulator-5554 \
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.chla.kindd.ui.navigation.AppEntryNavigationTest,com.chla.kindd.ui.screens.ChatScreenInitialPromptTest,com.chla.kindd.ui.theme.KiNDDPressSurfaceTest \
  --no-daemon --console=plain
```

Result: `BUILD SUCCESSFUL` in 55 seconds on `Pixel_8(AVD) - 16`; 19 tests,
0 failures, 0 errors, 0 skipped. One earlier combined attempt was externally
interrupted when another worktree installed the same package and Android killed
the instrumentation PID with signal 9; the clean uncontended rerun above is the
accepted result.

```bash
ANDROID_HOME="/Users/alexbeattie/Library/Android/sdk" \
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug \
  --no-daemon --console=plain
```

Result: `BUILD SUCCESSFUL` in 52 seconds; 171 unit tests, 0 failures and
0 errors; the 18MB debug APK assembled; lint completed with 0 errors and 87
pre-existing warnings.
