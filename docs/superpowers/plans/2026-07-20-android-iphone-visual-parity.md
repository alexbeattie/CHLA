# Android iPhone Visual Parity Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the native Android presentation so onboarding, Home, Map, Regions, List, Chat, More, and Settings match the current live iPhone product while preserving the verified Android data and state foundation.

**Architecture:** Add a small role-based Compose design system and a shared bundled regional-center boundary source, then rebuild each screen as a presentation layer over the existing ViewModels and stores. The app shell owns floating navigation and the modal Chat sheet; Map and List continue to share `DiscoveryStore`, and all regional-center map surfaces share one cached GeoJSON parser/source.

**Tech Stack:** Kotlin 1.9.22, Jetpack Compose BOM 2024.04.00, Material 3, Navigation Compose 2.7.6, Google Maps Compose 4.3.0, Hilt 2.50, Gson, JUnit 4, Compose UI instrumentation tests, Pixel 8 API 36 emulator.

## Global Constraints

- Work only in `/Users/alexbeattie/Developer/CHLA-android-persistent-discovery` on `feature/android-persistent-discovery`; preserve the dirty `release/1.4.1` checkout.
- The current SwiftUI implementation and fresh iPhone 16 Pro captures under `/tmp/kindd-iphone-reference-nr5Y7O` are the visual source of truth.
- Use native Compose and Google Maps; no Python or backend changes.
- Preserve the existing profile, `DiscoveryStore`, latest-wins requests, API decoding, privacy, localization, and accessibility behavior.
- New user-facing strings use `KiNDD`, never introduce `CHLA`, and must exist in English and Spanish.
- Keep 48dp semantic targets, TalkBack labels, large-text usability, light/dark contrast, reduced-motion behavior, and solid fallbacks for translucent chrome.
- Do not invent regional-center provider counts, resident counts, taglines, or service catalogs that are not present in production data.
- Each task begins with a failing focused test, ends with its focused suite passing, and is committed independently.

## File Map

- `ui/theme/Color.kt`, `Theme.kt`, `Type.kt`, `KiNDDTokens.kt`: role-based palette, adaptive canvas, typography, geometry, motion.
- `ui/design/KiNDDSurfaces.kt`: top wash, neutral/frosted cards, gradients, press feedback.
- `ui/design/KiNDDControls.kt`: capsules, icon actions, segmented choice, service tiles/tags.
- `ui/design/KiNDDSearchOverlay.kt`: compact shared Map/List search surface.
- `ui/navigation/KiNDDFloatingNavBar.kt`: six-action floating app chrome.
- `data/servicearea/*`: deterministic bundled boundary model, parser, and cached source.
- `ui/map/RegionalCenterMapSurface.kt`: reusable Google Maps polygon rendering for Home, onboarding, and Regions.
- Existing screen files remain route/state adapters; large visual sections move into focused files beside the owning screen.

---

### Task 1: KiNDD visual tokens and reusable controls

**Files:**
- Create: `chla-android/app/src/main/java/com/chla/kindd/ui/theme/KiNDDTokens.kt`
- Modify: `chla-android/app/src/main/java/com/chla/kindd/ui/theme/Color.kt`
- Modify: `chla-android/app/src/main/java/com/chla/kindd/ui/theme/Theme.kt`
- Modify: `chla-android/app/src/main/java/com/chla/kindd/ui/theme/Type.kt`
- Create: `chla-android/app/src/main/java/com/chla/kindd/ui/design/KiNDDSurfaces.kt`
- Create: `chla-android/app/src/main/java/com/chla/kindd/ui/design/KiNDDControls.kt`
- Create: `chla-android/app/src/androidTest/java/com/chla/kindd/ui/design/KiNDDDesignSystemTest.kt`
- Copy: `chla-ios/CHLA-iOS/Resources/Assets.xcassets/KiNDDLogo.imageset/kindd-logo.png` to `chla-android/app/src/main/res/drawable/kindd_logo.png`

**Interfaces:**
- Produces: `KiNDDSpacing`, `KiNDDShapes`, `KiNDDMotion`, `Brush.kinddPrimaryGradient()`, `Brush.kinddAiGradient()`, `Modifier.kinddPressable()`, `KiNDDTopWash`, `KiNDDCard`, `KiNDDPrimaryCapsule`, `KiNDDIconAction`, `KiNDDServiceTile`, `KiNDDServiceTag`, `KiNDDSegmentedChoice`.
- Consumes: existing `KINDDTheme` and canonical colors in `Color.kt`.

- [ ] **Step 1: Write failing component-contract tests**

  Add tests that compose each primitive and assert: logo exists; primary and AI capsules expose 48dp semantics; standard cards use a 20dp silhouette; service tiles retain distinct ABA/Speech/OT/PT content descriptions; dark theme uses an adaptive neutral canvas rather than a broad lavender surface.

- [ ] **Step 2: Run the focused test and verify failure**

  Run: `cd chla-android && ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chla.kindd.ui.design.KiNDDDesignSystemTest`

  Expected: compilation fails because `ui.design` primitives and `kindd_logo` do not exist.

- [ ] **Step 3: Implement tokens and primitives**

  Define exact public contracts:

  ```kotlin
  object KiNDDSpacing { val page = 18.dp; val compact = 10.dp; val section = 22.dp }
  object KiNDDShapes { val selection = RoundedCornerShape(14.dp); val card = RoundedCornerShape(20.dp); val hero = RoundedCornerShape(26.dp); val sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp) }
  object KiNDDMotion { const val PressedControlScale = 0.97f; const val PressedCardScale = 0.985f }
  enum class KiNDDCardStyle { Neutral, Hero, Matched, ListRow, Destructive, Frosted }
  enum class KiNDDServiceRole { ABA, SPEECH, OCCUPATIONAL, PHYSICAL, OTHER }
  ```

  Build gradients with `Brush.linearGradient(listOf(Indigo, Violet))` and `Brush.linearGradient(listOf(Violet, Pink))`. Use layered semi-opaque surfaces and hairlines rather than adding a blur dependency. Copy the exact logo asset byte-for-byte.

- [ ] **Step 4: Run design-system and accessibility tests**

  Run: `cd chla-android && ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chla.kindd.ui.design.KiNDDDesignSystemTest,com.chla.kindd.ui.accessibility.TouchedSurfaceAccessibilityTest`

  Expected: all selected instrumentation tests pass.

- [ ] **Step 5: Commit**

  ```bash
  git add chla-android/app/src/main/java/com/chla/kindd/ui/theme chla-android/app/src/main/java/com/chla/kindd/ui/design chla-android/app/src/main/res/drawable/kindd_logo.png chla-android/app/src/androidTest/java/com/chla/kindd/ui/design
  git commit -m "feat(android): add KiNDD visual system"
  ```

### Task 2: Floating navigation, More destination, and modal Chat routing

**Files:**
- Create: `chla-android/app/src/main/java/com/chla/kindd/ui/navigation/KiNDDFloatingNavBar.kt`
- Modify: `chla-android/app/src/main/java/com/chla/kindd/ui/navigation/KINDDNavHost.kt`
- Modify: `chla-android/app/src/main/java/com/chla/kindd/ui/navigation/MainDestinationContent.kt`
- Create: `chla-android/app/src/main/java/com/chla/kindd/ui/screens/MoreScreen.kt`
- Modify: `chla-android/app/src/main/res/values/strings.xml`
- Modify: `chla-android/app/src/main/res/values-es/strings.xml`
- Modify: `chla-android/app/src/androidTest/java/com/chla/kindd/ui/navigation/AppEntryNavigationTest.kt`

**Interfaces:**
- Consumes: Task 1 controls and existing `MainNavActions.navigateToChat(ChatLaunchPrompt?)`.
- Produces: `Screen.More`, `MainDestinationContent.more(actions)`, `KiNDDFloatingNavBar(selectedRoute, onHome, onMap, onAsk, onRegions, onList, onMore)` and modal `ChatScreen` presentation without a Chat route.

- [ ] **Step 1: Finalize failing shell tests**

  Ensure navigation tests expect six localized icon-only actions with tags `bottom_nav_home`, `bottom_nav_map`, `bottom_nav_ask`, `bottom_nav_regions`, `bottom_nav_list`, `bottom_nav_more`; More must land on `Screen.More.route`, not Settings. Ask must open `chat_sheet` over the current route, deliver typed prompts once, and dismiss with Back without changing the selected destination.

- [ ] **Step 2: Run the focused navigation test and verify failure**

  Run: `cd chla-android && ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chla.kindd.ui.navigation.AppEntryNavigationTest`

  Expected: compilation/assertion failures for floating-nav constants, `Screen.More`, and `chat_sheet`.

- [ ] **Step 3: Implement the app shell**

  Remove `NavigationBar`, `NavigationBarItem`, and `Screen.Chat` from the graph. Keep `showChatSheet` plus a typed `pendingChatPrompt` in `KINDDMainNavHost`; `navigateToChat` sets both. Render content edge-to-edge with bottom content inset and overlay `KiNDDFloatingNavBar`. Use `ModalBottomSheet(shape = KiNDDShapes.sheet)` for Chat. Add a real More screen with Settings, About, FAQ, Regions, privacy, and terms actions; privacy/terms must open real content or clearly state unavailable.

- [ ] **Step 4: Run navigation, app-entry, and adaptive tests**

  Run: `cd chla-android && ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chla.kindd.ui.navigation.AppEntryNavigationTest,com.chla.kindd.ui.app.AppEntryContentTest,com.chla.kindd.ui.accessibility.AdaptiveAppBarTest`

  Expected: all selected tests pass in English, Spanish, and night-mode cases already covered by the suite.

- [ ] **Step 5: Commit**

  ```bash
  git add chla-android/app/src/main/java/com/chla/kindd/ui/navigation chla-android/app/src/main/java/com/chla/kindd/ui/screens/MoreScreen.kt chla-android/app/src/main/res/values*/strings.xml chla-android/app/src/androidTest/java/com/chla/kindd/ui/navigation/AppEntryNavigationTest.kt
  git commit -m "feat(android): match iPhone floating app shell"
  ```

### Task 3: Bundled regional-center boundaries and shared map surface

**Files:**
- Copy: `chla-ios/CHLA-iOS/Resources/la_regional_centers.geojson` to `chla-android/app/src/main/res/raw/la_regional_centers.geojson`
- Create: `chla-android/app/src/main/java/com/chla/kindd/data/servicearea/ServiceAreaModels.kt`
- Create: `chla-android/app/src/main/java/com/chla/kindd/data/servicearea/ServiceAreaGeoJsonParser.kt`
- Create: `chla-android/app/src/main/java/com/chla/kindd/data/servicearea/BundledServiceAreaDataSource.kt`
- Create: `chla-android/app/src/main/java/com/chla/kindd/ui/map/RegionalCenterMapSurface.kt`
- Test: `chla-android/app/src/test/java/com/chla/kindd/data/servicearea/ServiceAreaGeoJsonParserTest.kt`
- Create: `chla-android/app/src/androidTest/java/com/chla/kindd/ui/map/RegionalCenterMapSurfaceTest.kt`

**Interfaces:**
- Produces:

  ```kotlin
  data class ServiceAreaCoordinate(val latitude: Double, val longitude: Double)
  data class ServiceAreaFeature(val id: Int, val name: String, val acronym: String, val description: String, val polygons: List<List<ServiceAreaCoordinate>>)
  interface ServiceAreaDataSource { suspend fun getServiceAreas(): Result<List<ServiceAreaFeature>> }
  @Composable fun RegionalCenterMapSurface(areas: List<ServiceAreaFeature>, highlightedAcronym: String?, interactive: Boolean, onAreaClick: (String) -> Unit, modifier: Modifier = Modifier)
  ```

- [ ] **Step 1: Preserve and complete the failing parser tests**

  Retain tests for longitude/latitude conversion, Polygon and MultiPolygon outer rings, malformed features, cancellation propagation, successful caching, exact asset SHA-256 `27bcaa63cb143e55abe9cdfccbf52b86f02522f28da3f280a2d8a001bd28070b`, and the seven expected acronyms.

- [ ] **Step 2: Run the parser test and verify failure**

  Run: `cd chla-android && ./gradlew testDebugUnitTest --tests com.chla.kindd.data.servicearea.ServiceAreaGeoJsonParserTest`

  Expected: compilation fails because parser/source/model classes and asset do not exist.

- [ ] **Step 3: Implement parser, cached source, and map surface**

  Parse Gson `JsonObject` defensively. Keep only the outer ring of each polygon, require at least three valid finite coordinate pairs, return immutable lists, rethrow `CancellationException`, and cache only successful loads. Render each polygon with canonical region color, 0.15 alpha normally and 0.34 highlighted, with 1.5/3dp strokes. Hide default Google controls in the reusable surface.

- [ ] **Step 4: Run parser and map-surface tests**

  Run: `cd chla-android && ./gradlew testDebugUnitTest --tests com.chla.kindd.data.servicearea.ServiceAreaGeoJsonParserTest connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chla.kindd.ui.map.RegionalCenterMapSurfaceTest`

  Expected: unit and instrumentation tests pass; checksum and seven-feature assertions pass.

- [ ] **Step 5: Commit**

  ```bash
  git add chla-android/app/src/main/res/raw chla-android/app/src/main/java/com/chla/kindd/data/servicearea chla-android/app/src/main/java/com/chla/kindd/ui/map chla-android/app/src/test/java/com/chla/kindd/data/servicearea chla-android/app/src/androidTest/java/com/chla/kindd/ui/map
  git commit -m "feat(android): add regional center boundary maps"
  ```

### Task 4: Rebuild Home around the live iPhone hierarchy

**Files:**
- Modify: `chla-android/app/src/main/java/com/chla/kindd/ui/screens/HomeScreen.kt`
- Create: `chla-android/app/src/main/java/com/chla/kindd/ui/home/HomeHero.kt`
- Create: `chla-android/app/src/main/java/com/chla/kindd/ui/home/HomeServiceTiles.kt`
- Create: `chla-android/app/src/main/java/com/chla/kindd/ui/home/HomeJourneyContent.kt`
- Modify: `chla-android/app/src/main/java/com/chla/kindd/ui/navigation/MainDestinationContent.kt`
- Modify: `chla-android/app/src/main/res/values/strings.xml`
- Modify: `chla-android/app/src/main/res/values-es/strings.xml`
- Modify: `chla-android/app/src/androidTest/java/com/chla/kindd/ui/home/HomeContentTest.kt`

**Interfaces:**
- Consumes: existing `HomeUiState`, `HomeViewModel`, profile-authority callbacks, Task 1 components, and Task 3 map surface.
- Produces: live-iPhone order: logo/county/menu, 340dp map hero + matched/ZIP overlay, four service tiles, next-step card, guided questions, footer, floating Ask capsule.

- [ ] **Step 1: Add failing Home composition tests**

  Extend `HomeContentTest` to assert the new order/tags, matched center highlight, Explore/Details/phone callbacks, four therapy roles, journey CTA, three guided questions, FAQ/About actions, and floating Ask capsule. Retain every existing profile replacement, stale-lookup, localization, and 48dp assertion.

- [ ] **Step 2: Run the Home tests and verify failure**

  Run: `cd chla-android && ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chla.kindd.ui.home.HomeContentTest`

  Expected: new hero/header/service/question tags are absent.

- [ ] **Step 3: Implement the Home vertical slice**

  Keep `HomeScreen` as state/event wiring. Move visual sections to the three focused files. Load boundaries once through the shared source, highlight `profile.regionalCenter.shortName`, preserve ZIP editing behavior, and route menu/footer actions through added UI-only callbacks. Match the live iPhone geometry: 18dp gutter, 22dp rhythm, 340dp hero, 26dp clip, neutral cards, top wash, and bottom insets for both Ask capsule and floating navigation.

- [ ] **Step 4: Run Home, ViewModel, and accessibility tests**

  Run: `cd chla-android && ./gradlew testDebugUnitTest --tests com.chla.kindd.ui.home.HomeViewModelTest connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chla.kindd.ui.home.HomeContentTest,com.chla.kindd.ui.accessibility.TouchedSurfaceAccessibilityTest`

  Expected: all selected tests pass.

- [ ] **Step 5: Build, install, and capture Home checkpoint**

  Run: `cd chla-android && ./gradlew assembleDebug && adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk && adb -s emulator-5554 shell am force-stop com.chla.kindd && adb -s emulator-5554 shell monkey -p com.chla.kindd 1 && adb -s emulator-5554 exec-out screencap -p > /tmp/kindd-android-home-parity.png`

  Expected: installed Home visibly matches `/tmp/kindd-iphone-reference-nr5Y7O/02-home.png` in hierarchy and product identity.

- [ ] **Step 6: Commit**

  ```bash
  git add chla-android/app/src/main/java/com/chla/kindd/ui/home chla-android/app/src/main/java/com/chla/kindd/ui/screens/HomeScreen.kt chla-android/app/src/main/java/com/chla/kindd/ui/navigation/MainDestinationContent.kt chla-android/app/src/main/res/values*/strings.xml chla-android/app/src/androidTest/java/com/chla/kindd/ui/home/HomeContentTest.kt
  git commit -m "feat(android): match iPhone Home composition"
  ```

### Task 5: Make Map full-bleed with floating discovery chrome

**Files:**
- Modify: `chla-android/app/src/main/java/com/chla/kindd/ui/screens/MapScreen.kt`
- Create: `chla-android/app/src/main/java/com/chla/kindd/ui/discovery/KiNDDSearchOverlay.kt`
- Modify: `chla-android/app/src/main/java/com/chla/kindd/ui/discovery/ActiveFilterChips.kt`
- Modify: `chla-android/app/src/main/java/com/chla/kindd/ui/discovery/DiscoveryStateContent.kt`
- Modify: `chla-android/app/src/androidTest/java/com/chla/kindd/ui/discovery/DiscoveryControlsTest.kt`
- Modify: `chla-android/app/src/androidTest/java/com/chla/kindd/ui/discovery/MapListParityTest.kt`

**Interfaces:**
- Consumes: existing `DiscoveryState`, `DiscoveryUiActions`, `MapLocationState`, valid marker filtering, and Task 1 controls.
- Produces: persistent full-screen map plus compact search/filter overlay, right-side controls, resource/region badges, branded markers, and nonblocking progress/error overlays.

- [ ] **Step 1: Add failing Map hierarchy tests**

  Assert that `provider_map` remains composed during initial loading, empty, and recoverable error states; search/filter/location/refresh controls overlay it; no `TopAppBar` title stack exists; default zoom controls are disabled; invalid coordinates remain excluded; retry and filters still call the existing actions.

- [ ] **Step 2: Run focused discovery tests and verify failure**

  Run: `cd chla-android && ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chla.kindd.ui.discovery.DiscoveryControlsTest,com.chla.kindd.ui.discovery.MapListParityTest`

  Expected: map-retention and overlay assertions fail against the current Scaffold/Column implementation.

- [ ] **Step 3: Implement full-bleed Map**

  Render `GoogleMap` first in a `Box(fillMaxSize())`; place compact chrome with aligned overlays. Keep stale results/markers during refresh. Convert full-screen loading/empty/error replacement into overlay/banner variants for Map only. Use canonical marker hues and hide default zoom/location buttons in favor of labeled 48dp controls.

- [ ] **Step 4: Run Map/List parity and ViewModel tests**

  Run: `cd chla-android && ./gradlew testDebugUnitTest --tests com.chla.kindd.ui.screens.MapViewModelTest --tests com.chla.kindd.ui.screens.ProviderListViewModelTest connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chla.kindd.ui.discovery.DiscoveryControlsTest,com.chla.kindd.ui.discovery.MapListParityTest`

  Expected: all selected tests pass and shared criteria/results remain identical across Map and List.

- [ ] **Step 5: Install and capture Map checkpoint**

  Use the installed debug APK, navigate to Map, wait for tiles, and capture `/tmp/kindd-android-map-parity.png`. Compare it side-by-side with `/tmp/kindd-iphone-reference-nr5Y7O/03-map-first.png`.

- [ ] **Step 6: Commit**

  ```bash
  git add chla-android/app/src/main/java/com/chla/kindd/ui/screens/MapScreen.kt chla-android/app/src/main/java/com/chla/kindd/ui/discovery chla-android/app/src/androidTest/java/com/chla/kindd/ui/discovery
  git commit -m "feat(android): make discovery map immersive"
  ```

### Task 6: Match iPhone onboarding and Regions

**Files:**
- Modify: `chla-android/app/src/main/java/com/chla/kindd/ui/onboarding/OnboardingRoute.kt`
- Modify: `chla-android/app/src/main/java/com/chla/kindd/ui/onboarding/AudienceStep.kt`
- Modify: `chla-android/app/src/main/java/com/chla/kindd/ui/onboarding/ZipStep.kt`
- Modify: `chla-android/app/src/main/java/com/chla/kindd/ui/onboarding/RegionalCenterStep.kt`
- Modify: `chla-android/app/src/main/java/com/chla/kindd/ui/onboarding/JourneyStep.kt`
- Modify: `chla-android/app/src/main/java/com/chla/kindd/ui/onboarding/AgeGroupStep.kt`
- Modify: `chla-android/app/src/main/java/com/chla/kindd/ui/screens/RegionalCentersScreen.kt`
- Modify: `chla-android/app/src/androidTest/java/com/chla/kindd/ui/onboarding/OnboardingContentTest.kt`
- Create: `chla-android/app/src/androidTest/java/com/chla/kindd/ui/regions/RegionalCentersContentTest.kt`

**Interfaces:**
- Consumes: existing onboarding ViewModel state/actions, existing RegionalCentersViewModel, Task 1 selection controls, and Task 3 map surface.
- Produces: five centered branded onboarding pages and a first-class Regions Map/List destination with selected-center sheet.

- [ ] **Step 1: Write failing onboarding/Regions visual-contract tests**

  Assert capsule progress, logo, segmented audience choice, compact ZIP control, matched polygon hero, icon-led journey/age selections, gradient Continue, default Regions map mode, Map/List toggle, seven boundaries, ZIP result, and selected-center details. Retain existing persistence, offline, edit-mode, Spanish, large-text, and hit-target assertions.

- [ ] **Step 2: Run focused tests and verify failure**

  Run: `cd chla-android && ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chla.kindd.ui.onboarding.OnboardingContentTest,com.chla.kindd.ui.regions.RegionalCentersContentTest`

  Expected: new branded progress/map/toggle tags are absent.

- [ ] **Step 3: Implement onboarding and Regions presentation**

  Preserve all ViewModel transitions. Replace linear progress/form rows with the approved components and use the shared boundary surface for matched-center and Regions map. Regions list remains available behind a compact segmented toggle. Use only real center API fields in the detail sheet.

- [ ] **Step 4: Run onboarding/Regions unit and device suites**

  Run: `cd chla-android && ./gradlew testDebugUnitTest --tests com.chla.kindd.ui.onboarding.OnboardingViewModelTest --tests com.chla.kindd.ui.screens.RegionalCentersViewModelTest connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chla.kindd.ui.onboarding.OnboardingContentTest,com.chla.kindd.ui.regions.RegionalCentersContentTest`

  Expected: all selected tests pass.

- [ ] **Step 5: Commit**

  ```bash
  git add chla-android/app/src/main/java/com/chla/kindd/ui/onboarding chla-android/app/src/main/java/com/chla/kindd/ui/screens/RegionalCentersScreen.kt chla-android/app/src/androidTest/java/com/chla/kindd/ui/onboarding chla-android/app/src/androidTest/java/com/chla/kindd/ui/regions
  git commit -m "feat(android): match iPhone onboarding and regions"
  ```

### Task 7: Rebuild provider List cards and normalize addresses

**Files:**
- Modify: `chla-android/app/src/main/java/com/chla/kindd/data/models/Provider.kt`
- Create: `chla-android/app/src/main/java/com/chla/kindd/data/models/ProviderAddressFormatter.kt`
- Modify: `chla-android/app/src/main/java/com/chla/kindd/ui/screens/ProviderListScreen.kt`
- Create: `chla-android/app/src/main/java/com/chla/kindd/ui/providers/ProviderCard.kt`
- Modify: `chla-android/app/src/test/java/com/chla/kindd/data/models/ProviderJsonTest.kt`
- Create: `chla-android/app/src/test/java/com/chla/kindd/data/models/ProviderAddressFormatterTest.kt`
- Modify: `chla-android/app/src/androidTest/java/com/chla/kindd/ui/discovery/MapListParityTest.kt`

**Interfaces:**
- Produces: `fun ProviderAddressFormatter.format(address: String?, city: String?, state: String?, zipCode: String?): String` and a neutral provider card with type, distance, two-line address, up to three therapy tags, phone, and regional-center badge.

- [ ] **Step 1: Write failing address and card tests**

  Cover plain addresses, JSON-object strings, JSON-array strings, repeated city/state/ZIP fragments, blank values, and malformed-but-recoverable strings. Add UI assertions for provider type, distance, therapy overflow, phone, regional center, and two-line address.

- [ ] **Step 2: Run focused tests and verify failure**

  Run: `cd chla-android && ./gradlew testDebugUnitTest --tests com.chla.kindd.data.models.ProviderAddressFormatterTest --tests com.chla.kindd.data.models.ProviderJsonTest`

  Expected: formatter class/type decoding is absent and JSON-like display cases fail.

- [ ] **Step 3: Implement normalization and compact List composition**

  Decode the API `type` field without breaking existing fixtures. Normalize presentation only; never discard the provider. Move card rendering to `ui/providers/ProviderCard.kt`, use service-role colors, and replace the tall app bar/filter stack with the shared compact List header/search treatment.

- [ ] **Step 4: Run model and Map/List tests**

  Run: `cd chla-android && ./gradlew testDebugUnitTest --tests com.chla.kindd.data.models.ProviderAddressFormatterTest --tests com.chla.kindd.data.models.ProviderJsonTest connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chla.kindd.ui.discovery.MapListParityTest`

  Expected: all selected tests pass and no JSON punctuation appears in display-address fixtures.

- [ ] **Step 5: Commit**

  ```bash
  git add chla-android/app/src/main/java/com/chla/kindd/data/models chla-android/app/src/main/java/com/chla/kindd/ui/screens/ProviderListScreen.kt chla-android/app/src/main/java/com/chla/kindd/ui/providers chla-android/app/src/test/java/com/chla/kindd/data/models chla-android/app/src/androidTest/java/com/chla/kindd/ui/discovery/MapListParityTest.kt
  git commit -m "feat(android): match iPhone provider list cards"
  ```

### Task 8: Match the Chat sheet and compact Settings hierarchy

**Files:**
- Modify: `chla-android/app/src/main/java/com/chla/kindd/ui/screens/ChatScreen.kt`
- Create: `chla-android/app/src/main/java/com/chla/kindd/ui/chat/ChatComponents.kt`
- Modify: `chla-android/app/src/main/java/com/chla/kindd/ui/screens/SettingsScreen.kt`
- Modify: `chla-android/app/src/main/java/com/chla/kindd/ui/screens/MoreScreen.kt`
- Modify: `chla-android/app/src/main/res/values/strings.xml`
- Modify: `chla-android/app/src/main/res/values-es/strings.xml`
- Create: `chla-android/app/src/androidTest/java/com/chla/kindd/ui/chat/ChatContentTest.kt`
- Modify: `chla-android/app/src/androidTest/java/com/chla/kindd/ui/settings/SettingsContentTest.kt`

**Interfaces:**
- Consumes: existing ChatViewModel request/error behavior, Task 1 AI gradient controls, Task 2 modal routing, and existing SettingsViewModel profile actions.
- Produces: branded Chat sheet toolbar/suggestions/bubbles/composer and compact grouped More/Settings rows with no inert actions.

- [ ] **Step 1: Write failing Chat and Settings UI tests**

  Assert sparkle toolbar, no duplicated empty-state title, suggestion capsules, gradient user bubble, neutral assistant card, multiline composer, loading/send state, grouped Settings sections, language/setup/search/location/information hierarchy, and working privacy/terms actions.

- [ ] **Step 2: Run focused tests and verify failure**

  Run: `cd chla-android && ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chla.kindd.ui.chat.ChatContentTest,com.chla.kindd.ui.settings.SettingsContentTest`

  Expected: new sheet component tags and grouped rows are absent.

- [ ] **Step 3: Implement Chat and Settings presentation**

  Keep chat transport unchanged. Replace CHLA blue/gold with the canonical AI/primary roles, use full-width readable assistant content, compact horizontal suggestions, and a rounded composer. Do not add attachment/microphone buttons unless functional. Recompose More/Settings with compact grouped rows and preserve edit/reset confirmations.

- [ ] **Step 4: Run Chat, Settings, localization, and accessibility tests**

  Run: `cd chla-android && ./gradlew testDebugUnitTest --tests com.chla.kindd.ui.screens.ChatViewModelInitialPromptTest --tests com.chla.kindd.ui.settings.SettingsViewModelTest connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.chla.kindd.ui.chat.ChatContentTest,com.chla.kindd.ui.settings.SettingsContentTest,com.chla.kindd.ui.accessibility.TouchedSurfaceAccessibilityTest`

  Expected: all selected tests pass.

- [ ] **Step 5: Commit**

  ```bash
  git add chla-android/app/src/main/java/com/chla/kindd/ui/chat chla-android/app/src/main/java/com/chla/kindd/ui/screens/ChatScreen.kt chla-android/app/src/main/java/com/chla/kindd/ui/screens/SettingsScreen.kt chla-android/app/src/main/java/com/chla/kindd/ui/screens/MoreScreen.kt chla-android/app/src/main/res/values*/strings.xml chla-android/app/src/androidTest/java/com/chla/kindd/ui/chat chla-android/app/src/androidTest/java/com/chla/kindd/ui/settings
  git commit -m "feat(android): match iPhone chat and settings"
  ```

### Task 9: Visual baselines, full verification, and emulator handoff

**Files:**
- Create: `chla-android/app/src/androidTest/java/com/chla/kindd/ui/visual/VisualParityCaptureTest.kt`
- Create: `chla-android/scripts/capture-visual-parity.sh`
- Create: `docs/visual-parity/android-iphone-parity-manifest.md`
- Modify only if failures require it: touched implementation/tests from Tasks 1-8.

**Interfaces:**
- Produces deterministic captures for onboarding, Home, Map, Regions, List, Chat, and Settings with fixed fixtures/locale/theme/font scale plus a manifest linking current iPhone and Android state.

- [ ] **Step 1: Add deterministic capture tests and script**

  Use existing Compose test APIs and `captureToImage()` for fixture-driven components; use `adb exec-out screencap -p` for live Google Maps screens. Fix locale to English, font scale to 1.0, animations to disabled for captures, and document the exact device/view state. Add dark and 1.3x text captures for shared chrome/Home.

- [ ] **Step 2: Run the complete unit and instrumentation suites**

  Run: `cd chla-android && ./gradlew testDebugUnitTest testReleaseUnitTest connectedDebugAndroidTest`

  Expected: 0 failed tests. Report exact debug/release/instrumentation execution counts.

- [ ] **Step 3: Run lint and both builds**

  Run: `cd chla-android && ./gradlew lintDebug lintRelease assembleDebug assembleRelease`

  Expected: 0 lint errors; both APKs exist. Record warnings separately and calculate debug/release SHA-256 values.

- [ ] **Step 4: Install exact APK and exercise live flows**

  Install the just-built debug APK on `emulator-5554`. Verify first-run onboarding, ZIP `91403`, matched regional center, Home persistence, Map tiles/markers, Regions polygon selection, List results/address formatting, Chat sheet open/dismiss/send failure recovery, Settings edit/reset, process restart, and offline recovery.

- [ ] **Step 5: Capture and compare every target screen**

  Capture Android onboarding, Home, Map, Regions, List, Chat, and Settings. Compare each with the matching fresh iPhone capture. Correct any P0 hierarchy, spacing, navigation, surface, iconography, or typography discrepancy before completion; do not accept functional green as visual proof.

- [ ] **Step 6: Run independent code and spec review**

  Dispatch a behavior reviewer and a visual-contract reviewer. Resolve every correctness, accessibility, privacy, and P0/P1 parity finding, then rerun the affected focused and full checks.

- [ ] **Step 7: Commit evidence harness and final corrections**

  ```bash
  git add chla-android/app/src/androidTest/java/com/chla/kindd/ui/visual chla-android/scripts/capture-visual-parity.sh docs/visual-parity chla-android/app/src/main chla-android/app/src/test chla-android/app/src/androidTest
  git commit -m "test(android): lock visual parity evidence"
  ```

- [ ] **Step 8: Leave Android emulator visible on redesigned Home**

  Launch `com.chla.kindd`, navigate to Home with the saved `91403` profile, bring the Android Emulator window to the front, and leave it running for user review.
