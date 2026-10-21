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

### 2026-10-21 — Android: onboarding intro screen and feedback survey
- **Branch:** feat/survey-intro-android
- **Files:** chla-android/app/src/main/java/com/chla/kindd/ui/intro/*, chla-android/app/src/main/java/com/chla/kindd/ui/survey/*, chla-android/app/src/main/java/com/chla/kindd/data/survey/*, chla-android/app/src/main/java/com/chla/kindd/data/api/KINDDApi.kt, chla-android/app/src/main/java/com/chla/kindd/ui/onboarding/*, chla-android/app/src/main/java/com/chla/kindd/ui/navigation/*, chla-android/app/src/main/java/com/chla/kindd/ui/screens/SettingsScreen.kt, chla-android/app/src/main/java/com/chla/kindd/ui/screens/AboutScreen.kt, chla-android/app/src/main/java/com/chla/kindd/platform/ExternalIntents.kt, chla-android/app/src/main/AndroidManifest.xml, chla-android/app/src/main/res/values*/strings.xml
- **Problem:** First-run onboarding opened straight on the audience question with no explanation of what KiNDD does, its privacy terms, or where to go in an emergency, and the app had no way to collect user feedback.
- **Fix:** New `INTRO` onboarding step ahead of the questions (first run only; editing a profile still starts at audience) with the shared English/Spanish intro copy, a tinted urgent-concerns card, heading semantics, and mailto/Privacy Policy links; reopenable as "Welcome guide" from Settings and About with a Done button. New "Share feedback" survey screen posts the 11 optional answers plus `platform`/`locale`/`app_version` to `/api/surveys/responses/`, with empty, sending, success, 429, and error states. The survey DTO lives in `data.api` so the existing R8 keep rule covers its Gson field names. `OnboardingContentTest.completedSession_firstReusedRouteFrameIsReset_andSavedEventIsDelivered` already failed on main because it called `finish()` from `JOURNEY` (it never stepped past `HOW_TO`); it now walks to `AGE` first.

---

### 2026-10-21 — Quick Setup age chips were clipped in the label column
- **Branch:** feat/survey-intro
- **Files:** map-frontend/src/components/OnboardingFlow.vue
- **Problem:** The age row put a hint and the chip grid into a 2-column `.form-row` (`150px` + the rest). The chips wrapped into the 150px track, and `text-overflow: ellipsis` cut them to "0-5 ye..." and "I have more than ...".
- **Fix:** Hint and chips sit in one `.age-field` in the wide column, same track as the audience buttons and the diagnosis select.

---

### 2026-10-21 — Onboarding: intro folded into the carousel steps
- **Branch:** feat/survey-intro
- **Files:** map-frontend/src/components/{OnboardingFlow,IntroContent}.vue, map-frontend/src/i18n/locales/{en,es}.json
- **Problem:** The intro was a separate wall of ten sections ahead of step 1, ending on a red emergency box right above Get started, so it read as a warning. Step 1 then repeated the welcome.
- **Fix:** Step 1 carries the intro title, lead, and three short cards (how it works, services, audience) plus a language switcher. Step 2 adds what happens next, a privacy line with the policy link, and a neutral Good to know note (not medical advice, no endorsement, 911) linking the full text at /intro. Steps 1-2 and the nav buttons are translated (en/es). /intro's emergency section is no longer red. Step content no longer clips the top of tall steps (`justify-content: center` replaced with auto margins). Above 768px the dialog is a fixed `min(780px, 92vh)` tall so step 1 fits a laptop screen without scrolling and the box doesn't resize between steps; phones keep the content-sized layout. `/survey` is prerendered so https://kinddhelp.org/survey returns 200 from CloudFront instead of the 404 fallback.

---

### 2026-10-21 — Survey page: Clear is a row action, form reads as a grouped list
- **Branch:** feat/survey-intro
- **Files:** map-frontend/src/views/SurveyView.vue
- **Problem:** Clear used `btn btn-link`, and the global map stylesheet paints `.btn-link` as a 12px filled blue pill (`#0d6efd`). It sat under the radios, left edge on the circles, so the word didn't line up with the answers.
- **Fix:** Dropped the shared button classes. Choices are a grouped list (44px rows, hairline separators, filled radio). Clear is the last row of that group, tint text, same inset as the answer labels. Fields, title, and submit follow the same grouped-form treatment. Submit is the only filled button.

---

### 2026-10-21 — Web onboarding intro screen and anonymous feedback survey
- **Branch:** feat/survey-intro
- **Files:** maplocation/surveys/*, maplocation/maplocation/{settings,urls}.py, maplocation/pytest.ini, map-frontend/src/components/{IntroContent,OnboardingFlow,LanguageSwitcher,AppNavBar}.vue, map-frontend/src/views/{IntroView,SurveyView,MapView,AboutView,PrivacyPolicyView}.vue, map-frontend/src/{composables/useSurvey,services/surveyService}.ts, map-frontend/src/i18n/locales/{en,es}.json, map-frontend/src/seo/siteConfig.js, map-frontend/vite.config.js, map-frontend/public/sitemap.xml, .env.example
- **Problem:** Families went straight to the location question with no explanation of the tool, its limits, privacy, or emergencies; the research team had no way to collect family feedback.
- **Fix:** Intro screen (en/es) is now the first onboarding screen and lives at /intro. /survey posts to new `POST /api/surveys/responses/` (anonymous, global rate limit), stored in `surveys.SurveyResponse`, viewable and downloadable as CSV in the client portal, and emailed through SES when `SURVEY_NOTIFY_EMAILS` and `SURVEY_FROM_EMAIL` are set. Privacy policy covers the survey. LanguageSwitcher closes based on its own element, so two switchers on one page no longer fight.

---

### 2026-10-09 — release.sh: pass the App Store Connect key path to xcodebuild
- **Branch:** chore/ios-144-release
- **Files:** chla-ios/scripts/release.sh, chla-ios/CHLA-iOS/Resources/Info.plist
- **Problem:** 1.4.4 export failed with "No signing certificate iOS Distribution found" and "Your session has expired" for the Apple ID. xcodebuild only uses the API key when -authenticationKeyPath is given; without it the cloud-managed distribution cert needs a live Xcode Apple ID session.
- **Fix:** Archive and export both pass -authenticationKeyPath with the key under ~/.appstoreconnect/private_keys; script exits early if the key file is missing. Info.plist bumped to 1.4.4 (release.sh reads the version from there, not the pbxproj).

---

### 2026-10-09 — App Store search metadata: subtitle, keywords, Spanish listing
- **Branch:** chore/aso-keywords
- **Files:** chla-ios/AppStoreMetadata.md, chla-ios/CHLA-iOS.xcodeproj/project.pbxproj
- **Problem:** Users searching "kindd" on an iPhone get autocorrected to "kind"/"kinky" results; the live keyword field repeated subtitle words and had no misspelling or domain terms; promotional text was blank on 1.4.3; no Spanish store listing despite a bilingual app.
- **Fix:** Promo text restored on live 1.4.3. Version 1.4.4 created in App Store Connect via API with subtitle "Autism & Regional Center Help", single-word keywords including `kind` and `kinddhelp`, and a full es-MX listing. `MARKETING_VERSION` bumped to 1.4.4 so the next release.sh build attaches to it.

---

### 2026-09-30 — State that kinddhelp.org belongs to KINDD on the About page
- **Branch:** fix/about-domains
- **Files:** map-frontend/src/views/AboutView.vue, map-frontend/src/i18n/locales/en.json, map-frontend/src/i18n/locales/es.json
- **Problem:** Google Workspace for Nonprofits activation for kinddhelp.org was refused because the official site on the application is kinddhelp.com and nothing there connects the two domains.
- **Fix:** Legal entity block now says KINDD owns and operates kinddhelp.com and kinddhelp.org and that staff email uses kinddhelp.org. English and Spanish.

---

### 2026-09-16 — EIN line missing from the About page
- **Branch:** fix/about-ein-pipe
- **Files:** map-frontend/src/i18n/locales/en.json, map-frontend/src/i18n/locales/es.json
- **Problem:** `about.legalEntityEin` was "KINDD | EIN 42-3052958"; vue-i18n reads `|` as the plural separator, so the rendered line was blank after "KINDD".
- **Fix:** Comma instead of pipe. Do not put a bare `|` in locale strings.

---

### 2026-09-16 — Legal entity block on the About page
- **Branch:** feat/about-legal-entity
- **Files:** map-frontend/src/views/AboutView.vue, map-frontend/src/i18n/locales/en.json, map-frontend/src/i18n/locales/es.json
- **Problem:** Goodstack, TechSoup, and Google for Nonprofits cross-check the website against the application and kinddhelp.com showed no legal name, EIN, or exempt status. IRS 501(c)(3) determination (Letter 947, 2026-09-01) now allows the claim.
- **Fix:** About page states the operator is KINDD, a California nonprofit public benefit corporation and 501(c)(3) public charity, EIN 42-3052958, contributions deductible to the extent allowed by law. English and Spanish.

---

### 2026-09-01 — Bump iOS and Android to 1.4.3
- **Branch:** feat/onboarding-howto
- **Files:** chla-ios/CHLA-iOS/Resources/Info.plist, chla-ios/CHLA-iOS.xcodeproj/project.pbxproj, chla-android/app/build.gradle.kts
- **Problem:** 1.4.2 is already tagged and uploaded to App Store Connect; Android on main was still versionCode 1 / 1.4.1 while another Play bundle used versionCode 3.
- **Fix:** Marketing version 1.4.3 (iOS build 1) and Android versionCode 4 so store uploads are unique.

---

### 2026-09-01 — Keep Mapbox token when switching frontend env
- **Branch:** feat/onboarding-howto
- **Files:** map-frontend/switch-env.sh
- **Problem:** `./switch-env.sh dev` rewrote `.env` from shell vars only, so a worktree (or a shell with no `VITE_MAPBOX_TOKEN`) ended up with a blank token and Mapbox never painted.
- **Fix:** Read the existing token from `.env` / `.env.production` before rewriting, and warn if none is found.

---

### 2026-09-01 — How-to step, multi-child ages, insurance buckets, disclaimers
- **Branch:** feat/onboarding-howto
- **Files:** chla-ios/CHLA-iOS/Views/OnboardingView.swift, chla-ios/CHLA-iOS/Views/MapContainerView.swift, chla-ios/CHLA-iOS/App/CHLA_iOSApp.swift, map-frontend/src/components/OnboardingFlow.vue, map-frontend/src/components/map/FilterPanel.vue, map-frontend/src/constants/filters.js, chla-android/app/src/main/java/com/chla/kindd/ui/onboarding/OnboardingUiState.kt, chla-android/app/src/main/java/com/chla/kindd/ui/onboarding/OnboardingViewModel.kt, chla-android/app/src/main/java/com/chla/kindd/ui/onboarding/HowToGuideContent.kt, chla-android/app/src/main/java/com/chla/kindd/ui/discovery/ActiveFilterChips.kt, chla-android/app/src/main/java/com/chla/kindd/data/discovery/DiscoveryCatalog.kt, chla-android/app/src/main/java/com/chla/kindd/data/profile/ProfileModels.kt
- **Problem:** After the Regional Center match, families were dropped into optional questions with no explanation of Filters; age was single-select; insurance listed dozens of carriers; there was no in-flow disclaimer that KiNDD does not keep medical information. Android still had the old flow after iOS and web shipped the new one.
- **Fix:** Added a how-to step after Regional Center, multi-select ages plus "I have more than 1 child", a visible Filters/Reset/kid-toggle bar, three insurance buckets (Medi-Cal, Private Insurance, Private Pay), and a navigation-tool disclaimer on onboarding, filters, About, and FAQ. Android now matches iOS: optional journey/age, Settings how-to, and a first-map Filters coach.

---

### 2026-08-13 — release.sh tags each upload
- **Branch:** chore/release-tagging
- **Files:** chla-ios/scripts/release.sh
- **Problem:** Releases were never pinned in git (1.4.0 and 1.4.1 shipped untagged; v1.4.1/v1.4.2 were back-tagged by hand today), so the archived source of an upload was not recoverable from the repo.
- **Fix:** After a successful upload, release.sh creates an annotated v{VERSION} tag and pushes it - only when the working tree is clean, since a dirty tree would not describe the archived source. Existing tags are never moved.

### 2026-08-13 — TestFlight uploads broadcast to Slack; 1.4.2 version bump
- **Branch:** feat/onboarding-diagnosis
- **Files:** chla-ios/scripts/release.sh, chla-ios/CHLA-iOS/Resources/Info.plist, chla-ios/CHLA-iOS.xcodeproj/project.pbxproj
- **Problem:** Nothing announced new TestFlight builds; testers had to be pinged by hand. Marketing version needed to exceed the released 1.4.1 for the next beta-reviewable build (external testers are stuck NOT_INVITED behind a closed version).
- **Fix:** release.sh posts to the KiNDD Slack (#general by default, KINDD_SLACK_CHANNEL to override) after a successful upload, using SLACK_BOT_TOKEN or the stored bot token; notify failure never fails the release. Version bumped to 1.4.2 (build 1) in Info.plist and all four pbxproj MARKETING_VERSION slots.

### 2026-08-13 — Diagnosis step in iOS onboarding plus provider card match badge
- **Branch:** feat/onboarding-diagnosis
- **Files:** chla-ios/CHLA-iOS/Views/OnboardingView.swift, chla-ios/CHLA-iOS/Views/ProviderListView.swift, chla-ios/CHLA-iOS/Views/MapContainerView.swift, chla-ios/CHLA-iOS/App/CHLA_iOSApp.swift
- **Problem:** The Aug 11 dataset import populated diagnoses_treated on all 307 providers, but nothing in the app asked families which diagnoses they were navigating: onboarding collected ZIP, journey stage, and age only, and provider cards gave no signal that a provider treats the user's diagnosis. The SearchFilters.diagnoses vocabulary also listed Intellectual Disability (0 providers) while omitting ADHD (258) and Sensory Processing Disorder (155).
- **Fix:** Onboarding gains an optional multi-select diagnosis step (step 5 of 6); the first non-Other pick becomes searchFilters.diagnosis (the API filter is single-valued) and all picks are stored in UserMemory for chat context. Provider cards show a purple "Treats X" capsule when a provider's diagnoses_treated matches the active filter or a remembered diagnosis. Diagnosis vocabulary aligned with live data.

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
