# FIXES.md — append-only change log (CHLA)

Newest first. Every fix/feature/behavior change gets an entry; refactors and
docs-only changes are exempt. On merge conflict: keep all entries, newest first.

Format:

### YYYY-MM-DD — short title
- **Branch:** feat/slug
- **Files:** path/one, path/two
- **Problem:** one or two lines
- **Fix:** one or two lines

---

### 2026-07-21 — Prepare Android for Google Play release
- **Branch:** feature/android-persistent-discovery
- **Files:** chla-android/app/build.gradle.kts, chla-android/build.gradle.kts, chla-android/gradle/wrapper/gradle-wrapper.properties, chla-android/app/src/main/AndroidManifest.xml, chla-android/app/src/main/java/com/chla/kindd/ui/, chla-android/app/src/main/res/values/, chla-android/app/src/main/res/values-es/, chla-android/app/src/test/, chla-android/app/src/androidTest/java/com/chla/kindd/ui/home/HomeContentTest.kt, chla-android/scripts/, chla-android/PLAY_STORE_RELEASE.md, chla-android/play-assets/
- **Problem:** The Android project targeted an expiring Play API level, produced only unsigned release artifacts, lacked a reproducible signing, policy, asset, and submission workflow, obscured required Maps attribution in two map heroes, directly launched external provider actions, and could unnecessarily exclude devices without location hardware. The full device suite also exposed one off-screen home action tap that could miss the control.
- **Fix:** Target Android 16 with its supported pinned toolchain, normalize the user-facing KiNDD name, add environment-only release signing backed by macOS Keychain and Maps-key validation, reserve Maps attribution space, route provider actions through guarded launchers, declare location hardware optional, make the home call-action test use semantics, and prepare the Organization-account runbook, listing, policy worksheets, and current Play assets.

### 2026-07-21 — Render unexpected chat headings safely
- **Branch:** feature/android-persistent-discovery
- **Files:** chla-android/app/src/main/java/com/chla/kindd/ui/chat/SafeMarkdown.kt, chla-android/app/src/test/java/com/chla/kindd/ui/chat/SafeMarkdownTest.kt
- **Problem:** A live Ask KiNDD response used an unsupported Markdown heading, leaving the raw `#` marker visible in Android chat.
- **Fix:** Degrade level-one through level-six headings to clean bold labels while preserving the renderer's intentionally small Markdown surface.

### 2026-07-21 — Stabilize virtualized Android UI device tests
- **Branch:** feature/android-persistent-discovery
- **Files:** chla-android/app/src/androidTest/java/com/chla/kindd/ui/accessibility/TouchedSurfaceAccessibilityTest.kt, chla-android/app/src/androidTest/java/com/chla/kindd/ui/discovery/DiscoveryControlsTest.kt, chla-android/app/src/androidTest/java/com/chla/kindd/ui/more/MoreContentTest.kt, chla-android/app/src/androidTest/java/com/chla/kindd/ui/settings/SettingsContentTest.kt, chla-android/app/src/main/java/com/chla/kindd/ui/screens/MoreScreen.kt
- **Problem:** Six emulator assertions queried off-screen LazyColumn or LazyRow children before Compose had created their semantics nodes, and one helper tried to scroll fixed list chrome.
- **Fix:** Scroll tagged lazy parents to their target semantics before interaction, keep fixed-chrome assertions non-scrolling, and expose the More list as a stable test seam.

### 2026-07-21 — Bound fresh-device location and external app launches
- **Branch:** feature/android-persistent-discovery
- **Files:** chla-android/app/src/main/java/com/chla/kindd/services/LocationService.kt, chla-android/app/src/main/java/com/chla/kindd/services/CurrentLocationFallback.kt, chla-android/app/src/main/java/com/chla/kindd/platform/ExternalIntents.kt, chla-android/app/src/main/java/com/chla/kindd/ui/screens/HomeScreen.kt, chla-android/app/src/main/java/com/chla/kindd/ui/regions/RegionalCentersContent.kt, chla-android/app/src/main/AndroidManifest.xml
- **Problem:** Fresh installs could fail location immediately when no cached fix existed, and Home or regional-center phone/website actions could crash on malformed destinations or missing handlers.
- **Fix:** Fall back to a cancellable 10-second Play Services current-location request, and route phone/web actions through a shared validated, resolvable, failure-safe launcher.

### 2026-07-21 — Align Android accessibility tests with current semantics
- **Branch:** feature/android-persistent-discovery
- **Files:** chla-android/app/src/androidTest/java/com/chla/kindd/ui/accessibility/TouchedSurfaceAccessibilityTest.kt, chla-android/app/src/androidTest/java/com/chla/kindd/ui/discovery/DiscoveryControlsTest.kt, chla-android/app/src/androidTest/java/com/chla/kindd/ui/settings/SettingsContentTest.kt
- **Problem:** Focused device tests still assumed removed first-viewport controls, a matched regional-center state, parent-owned therapy text semantics, and deleted navigation clearance.
- **Fix:** Assert the current localized/state-specific semantics, scroll lazy controls before interaction, and stop trying to scroll to intentionally absent content.

### 2026-07-20 — Restore live Home map and regional-center call action
- **Branch:** feature/android-persistent-discovery
- **Files:** chla-android/app/src/main/java/com/chla/kindd/ui/home/components/HomeMapHero.kt, chla-android/app/src/main/java/com/chla/kindd/ui/home/HomeUiState.kt, chla-android/app/src/main/java/com/chla/kindd/data/models/RegionalCenterContactCatalog.kt
- **Problem:** Home left its purple placeholder visible until service-area overlays loaded and hid matched-center calling while API contact hydration was unavailable.
- **Fix:** Always mount the shared Google map for base tiles, and use an iPhone-compatible seven-center phone catalog when no authoritative API phone is present.
