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

### 2026-08-13 — Skeleton loading state for the iOS Resources list
- **Branch:** fix/spinner-loading-skeleton
- **Files:** chla-ios/CHLA-iOS/Views/ProviderListView.swift
- **Problem:** The Resources tab attaches its full-screen gradient background to the content Group; while loading, the Group collapsed to the intrinsic size of the spinner VStack, so the 300pt indigo gradient painted as a floating lavender slab around the spinner. Every re-search also replaced the visible list with that spinner.
- **Fix:** Content now fills the screen so the background paints edge-to-edge; the bare spinner is replaced with pulsing skeleton provider cards (static under Reduce Motion, VoiceOver announces the loading label); skeletons show only until the first results arrive so refreshes keep the current list; the loading-to-list swap crossfades over 200ms.

### 2026-08-10 — Header-aware provider CSV import plus cleaned dataset
- **Branch:** fix/csv-import-absent-columns
- **Files:** maplocation/locations/management/commands/import_csv_providers.py, maplocation/locations/tests/test_import_csv_providers.py, docs/data/providers_complete_export_cleaned.csv
- **Problem:** import_csv_providers wrote every model field unconditionally, so a partial CSV (like the hand-cleaned providers sheet, which drops description/type/coordinates) would blank those fields on all 360 matched providers. The cleaned sheet also carried 6 mojibake cells and one lost provider name.
- **Fix:** The importer now assigns only fields whose columns exist in the CSV header (and reads utf-8-sig for BOM safety). Committed the repaired cleaned dataset for reference. Production turned out to be already enriched (Dec/Jul imports), so instead of a bulk import, applied a guarded fill-only pass to prod: 70 website fills, 8 mojibake name fixes, 9 junk `{}` insurance values cleared; 277 sheet-vs-prod conflicts exported for manual review rather than overwritten.

### 2026-07-31 — Add explicit privacy data-retention practices
- **Branch:** fix/privacy-retention-play
- **Files:** map-frontend/src/views/PrivacyPolicyView.vue
- **Problem:** Google Play rejected the privacy-policy declaration because it did not explain how long or under what conditions KiNDD retains and deletes user data.
- **Fix:** Added category-specific retention and deletion practices for on-device data, searches and location, Ask KiNDD, response reports, support requests, backups, and deletion requests.

### 2026-07-22 — Invalidate both .com and .org CloudFront on deploy
- **Branch:** fix/cloudfront-org-invalidation
- **Files:** .github/workflows/deploy-production.yml, map-frontend/deploy.sh
- **Problem:** Production deploy only invalidated the kinddhelp.com CloudFront dist; kinddhelp.com redirects to kinddhelp.org, which kept serving stale HTML until a manual invalidation.
- **Fix:** Invalidate both E2W6EECHUV4LMM (.com) and E2Z6DZAF6O77HY (.org) after S3 sync, and verify the canonical .org URL.

### 2026-07-22 — Privacy policy covers Android, Google Maps, and AI
- **Branch:** chore/privacy-policy-android
- **Files:** map-frontend/src/views/PrivacyPolicyView.vue, map-frontend/src/views/TermsOfServiceView.vue, map-frontend/src/seo/siteConfig.js
- **Problem:** Public privacy page still described Apple Maps/Mapbox only, omitted AI/data disclosures needed for Play submission, and used privacy@ instead of support@.
- **Fix:** Rewrote the /privacy page for iOS + Android + web, added Google Maps and AWS Bedrock/Ask KiNDD disclosures, and set contact email to support@kinddhelp.org.

---
