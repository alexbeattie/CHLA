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

### 2026-07-20 — Restore live Home map and regional-center call action
- **Branch:** feature/android-persistent-discovery
- **Files:** chla-android/app/src/main/java/com/chla/kindd/ui/home/components/HomeMapHero.kt, chla-android/app/src/main/java/com/chla/kindd/ui/home/HomeUiState.kt, chla-android/app/src/main/java/com/chla/kindd/data/models/RegionalCenterContactCatalog.kt
- **Problem:** Home left its purple placeholder visible until service-area overlays loaded and hid matched-center calling while API contact hydration was unavailable.
- **Fix:** Always mount the shared Google map for base tiles, and use an iPhone-compatible seven-center phone catalog when no authoritative API phone is present.
