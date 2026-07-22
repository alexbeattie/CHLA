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
