# Task 2A Report: Bundled LA Regional Center Geometry

## Outcome

Android now has the exact iPhone LA Regional Center GeoJSON as a bundled raw resource, together with Android-neutral models, a directly testable parser, and an injectable cached data source. The parser supports `Polygon` and `MultiPolygon`, preserves every renderable outer ring, and intentionally ignores interior rings for rendering.

## Files

- `chla-android/app/src/main/res/raw/la_regional_centers.geojson`
- `chla-android/app/src/main/java/com/chla/kindd/data/servicearea/ServiceAreaModels.kt`
- `chla-android/app/src/main/java/com/chla/kindd/data/servicearea/ServiceAreaGeoJsonParser.kt`
- `chla-android/app/src/main/java/com/chla/kindd/data/servicearea/BundledServiceAreaDataSource.kt`
- `chla-android/app/src/main/java/com/chla/kindd/di/RepositoryModule.kt`
- `chla-android/app/src/test/java/com/chla/kindd/data/servicearea/ServiceAreaGeoJsonParserTest.kt`

## TDD Evidence

RED was recorded before production edits.

1. The first focused command was run before any production edit and was blocked during Gradle configuration because this checkout did not have an Android SDK path configured:

   ```bash
   ./gradlew :app:testDebugUnitTest --tests com.chla.kindd.data.servicearea.ServiceAreaGeoJsonParserTest
   ```

2. The same focused command was rerun with the local SDK and failed as expected because the new service-area model, parser, and data source did not exist yet:

   ```bash
   ANDROID_HOME=/Users/alexbeattie/Library/Android/sdk ./gradlew :app:compileDebugUnitTestKotlin --rerun-tasks
   ```

   Expected RED: unresolved references to `ServiceAreaCoordinate`, `ServiceAreaGeoJsonParser`, and `BundledServiceAreaDataSource`.

GREEN command:

```bash
ANDROID_HOME=/Users/alexbeattie/Library/Android/sdk ./gradlew :app:testDebugUnitTest --tests com.chla.kindd.data.servicearea.ServiceAreaGeoJsonParserTest
```

Result: `BUILD SUCCESSFUL`; 5 tests, 0 failures, 0 errors. The test report verifies longitude/latitude conversion, MultiPolygon outer-ring extraction, interior-ring exclusion, malformed/unsupported feature skipping, checksum and seven-feature content, cancellation propagation, and successful-result caching/immutability.

## Asset Integrity

```text
27bcaa63cb143e55abe9cdfccbf52b86f02522f28da3f280a2d8a001bd28070b
```

The checksum matches `chla-ios/CHLA-iOS/Resources/la_regional_centers.geojson` exactly. `assembleDebug` also confirmed the resource is packaged at `res/raw/la_regional_centers.geojson`.

## Verification

```bash
ANDROID_HOME=/Users/alexbeattie/Library/Android/sdk ./gradlew :app:assembleDebug
```

Result: `BUILD SUCCESSFUL`.

No instrumentation resource test was added: the raw-file integrity test is a local JVM test, while the debug assembly verifies Android resource packaging.

## Self-review

- The data models contain no Google Maps or Android types, so parser tests run on the local JVM.
- GeoJSON pairs are deliberately converted from `[longitude, latitude]` to explicitly named `latitude` and `longitude` fields.
- `Polygon` yields its sole outer ring; `MultiPolygon` yields the first ring of every member polygon. Interior rings cannot become another center polygon.
- Invalid or unsupported individual features are skipped; an invalid collection or one with no usable feature returns `Result.failure`.
- The loader uses the injected IO dispatcher, caches only a successful immutable result, and rethrows `CancellationException`.
- `git diff --check` passed. Scope review excluded the concurrent navigation-test edit.

## Commit

This report is included in the single focused Task 2A commit; its final hash is supplied in the handoff because a Git commit cannot contain its own final content hash.

Implementation commit: `abe8c9e6dc8e69404882a178b2864b0fd4a5ec11`

## Concerns

The focused test and debug build passed. Existing unrelated Kotlin/deprecation warnings remain in the project build output; this task adds none. The first test invocation required an explicit `ANDROID_HOME` because this checkout lacks `chla-android/local.properties`.
