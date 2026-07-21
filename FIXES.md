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
