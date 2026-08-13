# Android Navigator Identity Design

**Status:** Approved by explicit user direction on 2026-07-21

## Goal

Rebuild the KiNDD Android app under the permanent Google Play identity
`com.navigator.kindd`, with no legacy institutional branding in the production
Android source, packaged artifacts, or Play-facing content.

## Chosen approach

Perform a complete Android namespace migration. The Gradle namespace and
application ID, Kotlin packages and directory trees, imports, generated-symbol
references, ProGuard rules, tests, resource identifiers, and release guidance
move together. A Gradle-only application ID change is rejected because the
relative manifest classes, Hilt output, generated `R`/`BuildConfig` symbols,
and source namespaces must remain consistent.

The existing repository and cross-platform infrastructure directory names are
not part of the Android artifact and remain unchanged. Renaming those paths
would expand this release into unrelated backend, deployment, and iOS work.

## Release behavior

- `com.navigator.kindd` is a new Android application with a new sandbox.
- Existing installs do not migrate local profile data or permissions.
- The existing upload key may sign the first bundle; it is not regenerated.
- Version `1.4.1` and version code `1` remain valid for the first upload of the
  new Play application.
- Google Maps must authorize the new package with the debug/upload signer for
  local testing and the Play App Signing certificate for Play-delivered builds.
- The Play Console record must be created only with `com.navigator.kindd`.

## Validation design

1. Add an identity contract test and observe it fail against the legacy tree.
2. Migrate production, unit-test, and instrumentation-test namespaces and make
   that test pass.
3. Run unit tests, lint, debug build, release build, and launcher parity checks.
4. Build a fresh signed AAB and verify its manifest package, version, signing,
   Maps metadata presence, and absence of legacy branding.
5. Install the renamed APK on the connected Android phone and verify launch,
   map rendering, location behavior, language selection, and core navigation.
6. Create the Play app with the exact package, upload to Internal testing, add
   the Play signing fingerprint to Maps restrictions, and test the
   Play-delivered build before any production submission.

## Failure boundaries

- If Google reports the target package unavailable, stop before creating the
  Play app and do not substitute another identifier without another rebuild.
- If Maps fails only after the rename, treat package/certificate restriction as
  the first hypothesis; do not expose or replace the API key in source.
- A locally signed AAB, a successful device install, and a Play upload remain
  separate evidence gates.
