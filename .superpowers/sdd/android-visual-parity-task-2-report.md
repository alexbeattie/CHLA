# Android visual parity Task 2 report

## Status

Literal-parity Home implementation is committed as `e445fcb` on
`agent/home-literal-parity-20260720`. The worktree is
`/tmp/kindd-home-parity.zzx4Tj` and is based on `5e448d4`.

## What changed

- Replaced the generic Material Home with the current iPhone hierarchy: compact exact KiNDD logo header, LA County context pill, 340dp regional-center map hero with physical center/ZIP overlay, four service tiles, contextual next step, guided questions, information footer, and persistent Ask capsule.
- Reused `RegionalCenterMapSurface`; no `ui/map` files were modified.
- Added bundled service-area load state to `HomeViewModel` without changing profile, ZIP lookup, center hydration, CAS, dial, or discovery authority.
- Added fixed nonlocalized Chat launch keys with exact English and natural Spanish prompts for ABA, center funding, and ZIP/center questions.
- Wired About, FAQ, Edit Profile, Settings, Explore, Details, Chat, therapy, ZIP submit, and dial actions to existing navigation/behavior paths.
- Added exact iPhone KiNDD logo asset; SHA-256 is `dbe96295fbd71b75d627d988e40b5db3e5b07e9ba28bff9bd0f1193abe3238c3`.
- Added English and Spanish Home parity resources in separate files.

## TDD evidence

### RED

- Service-area state unit tests failed before production changes with unresolved `ServiceAreaLoadState`, `serviceAreas`, and the missing `HomeViewModel` dependency.
- The literal-hierarchy device test failed against the old Home because `home_compact_logo` and the new hero/card hierarchy did not exist.
- Fixed Chat prompt-key tests failed while the enum still contained only the original three route values.

### GREEN

- `./gradlew :app:testDebugUnitTest --no-daemon`
  - `172` tests, `0` failures, `0` errors.
- `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin :app:compileDebugAndroidTestKotlin --no-daemon`
  - build successful.
- `./gradlew :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:assembleDebug :app:lintDebug --no-daemon`
  - build successful; debug APK assembled; lint completed with no errors.

## Connected-test evidence and device contention

- Exact Home instrumentation APK contained `13` tests.
- A direct exact-APK run executed all 13 and exposed four stale/interaction-sensitive test assertions after the production UI was already rendering. Those assertions were corrected:
  - iPhone-literal uppercase overline expectation;
  - authoritative full center name instead of the removed short-name line;
  - overflow header scrolled into view before menu interaction;
  - semantic click used for next-step callback verification so the persistent Ask overlay does not intercept a synthetic coordinate click.
- The corrected 13-test class was not rerun because the shared emulator was repeatedly replaced or force-stopped by concurrent Settings, Map/List, and Onboarding/Regions connected suites. Logcat proved the last interruption: the external `MapListParityTest` finished and force-stopped `com.chla.kindd` while the Home class was active.
- Per coordinator direction, no further device run was attempted in this worktree. The root agent will run the exact corrected Home class after shared connected work settles.

## Screenshot

No trustworthy screenshot was captured from this isolated branch because shared emulator installs were contending. The root agent will install the integrated APK and capture the final Home state. Any pre-integration Home screenshot would also be provisional until the separately assigned `RegionalCenterMapSurface` palette/stroke fix is integrated.

## Self-review and concerns

- No shared/main worktree files were edited or staged.
- No `ui/map`, Provider, or address files were edited.
- `KINDDNavHost.kt` has one isolated required Home Settings wiring line. `AppEntryNavigationTest.kt` has isolated Home Settings and prompt-resource contract additions; these may conflict with the parallel nav-fix branch and should be resolved by preserving both sets of assertions.
- Final connected Home verification and screenshot remain required after integration. The JVM, compilation, assemble, and lint gates are green.
