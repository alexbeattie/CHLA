# KiNDD Google Play release

This is the source of truth for the first Google Play release of
KiNDD - NDD Resource Navigator.

## Release identity

| Field | Value |
| --- | --- |
| Play account owner email | `alex@kinddhelp.org` |
| Account type | Organization |
| Developer name | `KiNDD` |
| App title | `KiNDD - NDD Resource Navigator` |
| Package | `com.navigator.kindd` |
| Version | `1.4.1` |
| Version code | `1` for the first upload; confirm unused in Play Console |
| Price | Free |
| Primary language | English (United States) |
| Category | Medical |
| Privacy policy | `https://kinddhelp.org/privacy` |
| Support email | `support@kinddhelp.org` |
| Website | `https://kinddhelp.org` |

The package name becomes permanent after the first upload. Confirm it before
creating the Play app.

## Account prerequisite

Google directs health-app publishers to use an Organization developer account.
Account creation requires a legal organization and matching D-U-N-S record,
official address and phone, an authorized representative, identity documents,
website verification, acceptance of Google's agreements, and the one-time
registration fee. Do not create a Personal account to bypass these checks.

The repository's nonprofit brief currently describes KiNDD as a founder-led
project moving toward incorporation; it does not establish a verified legal
entity or D-U-N-S number. Account creation must wait for those authoritative
details. The account owner must personally complete Google sign-in, identity
verification, payment, and legal acceptance.

## One-time upload key

Create the dedicated upload key on the release Mac:

```bash
cd chla-android
./scripts/create-play-upload-key.sh
```

The private keystore is stored outside the repository at
`~/.android/keys/kindd-upload.jks`. Its password is stored in macOS Keychain.
The public certificate is exported beside it. Back up the keystore in an
approved encrypted location; never commit it or its password.

Google Play App Signing should generate and protect the app-signing key. This
local key is only the upload key.

## Build and verify the signed bundle

```bash
cd chla-android
./scripts/build-play-bundle.sh
```

The script reads the password from Keychain, signs the release bundle, verifies
the signature, rejects a missing or placeholder Google Maps key, and prints its
SHA-256 checksum. The output is:

`app/build/outputs/bundle/release/app-release.aab`

Before upload, run the full unit, lint, and device verification appropriate to
the release and retain the results with the bundle checksum.

## Pre-upload service gate

The Android reporting client and backend must ship together. Before distributing
the Play build:

1. Deploy the backend commit containing the `llm.0001_initial` migration and run
   the migration.
2. Confirm `/api/llm/ask/` returns an opaque `response_fingerprint` and that a
   matching `/api/llm/response-reports/` request creates one admin-reviewable
   report.
3. Confirm the public privacy policy is updated from the checklist in
   `play-assets/privacy-policy-amendment.md` and approved for publication.
4. Reconcile the Play Data safety answers with production AWS Bedrock, Tavily,
   Langfuse, Google Maps, logging, retention, and deletion configuration.

## Play Console setup

1. Create the Organization developer account using `alex@kinddhelp.org` and
   finish verification.
2. Create the app with the title and package above, choose App (not Game), Free,
   and accept Play App Signing.
3. Upload the signed AAB to Internal testing first.
4. Add the English listing from `play-assets/listing/en-US.md` and the required
   icon, feature graphic, and phone screenshots from `play-assets/`.
5. Complete App access (no sign-in), Ads (none), Content rating, Target audience,
   News, Data safety, Health apps, and privacy-policy declarations accurately.
6. Restrict Google Maps to package `com.navigator.kindd`. Authorize the debug or
   upload signer used for direct installs, and authorize the Play App Signing
   signer for Play-delivered builds; the upload certificate is not the
   certificate users receive from Play.
7. Install the Play-delivered internal-test build and repeat map, location,
   provider, phone, website, English/Spanish, and Ask KiNDD checks.
8. Resolve every Play pre-review warning, then promote the verified build to
   production and submit it for review.

## Policy truth that must be reflected in Play

- Foreground coarse location permission is optional. During Regional Center
  onboarding, a coarse location fix is passed to the device's geocoding service
  to derive a ZIP, and that ZIP is sent to the KiNDD API; depending on the device
  implementation, geocoding may use a network service. User-initiated nearby
  provider discovery sends coarse coordinates to the KiNDD API. A ZIP is
  required to complete the profile. This release does not request precise or
  background location.
- ZIP, search text, age group, diagnosis, insurance, and service filters can be
  sent to the KiNDD API.
- Ask KiNDD prompts are sent to the KiNDD API and processed with AWS Bedrock /
  Anthropic. Reported AI responses are sent to KiNDD when the user explicitly
  submits an in-app report. The report endpoint uses a short-lived, answer-bound
  fingerprint and persists only its one-way digest; it does not use a client IP
  or device identifier for throttling.
- Google Maps and Google Play Services process data described by Google's Maps
  SDK disclosure. There are no ads, Firebase Analytics, or standalone crash SDKs
  in the Android app.
- The saved onboarding profile remains local. Chat history is not persisted on
  device. Network traffic uses HTTPS.

The public privacy policy and Play Data safety answers must match the released
code and actual backend/vendor configuration. Have the privacy owner or counsel
review those statements before production submission.
