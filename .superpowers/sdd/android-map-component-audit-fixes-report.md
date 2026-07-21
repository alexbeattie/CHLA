# Android regional-center map audit fixes

Base: `083747c0f784861b990517c5ac0ae3c25a5bce55`

## Changed

- Replaced the seven regional-center tokens with the approved iPhone palette.
- Encoded polygon stroke widths as `Dp` and converted them with `LocalDensity` only at the Google Maps boundary.
- Applied normal fill/stroke alpha `0.15/0.70` and highlighted fill/stroke alpha `0.34/1.0`.
- Split stable polygon geometry from reactive styling and cached the `LatLng` conversion by geometry identity.
- Added testable interaction and polygon contracts while preserving the public `RegionalCenterMapSurface` signature.

## TDD evidence

- RED: focused model tests failed for the old palette and width/alpha behavior: 2 tests, 2 failures.
- RED: focused model tests then failed to compile on the missing interaction, polygon, and density contracts.
- RED: Android-test compilation failed on the missing `RegionalCenterGoogleMapGeometryCache`.
- GREEN: `RegionalCenterMapModelsTest`: 7 tests, 0 failures.

## Final verification

Command:

```text
ANDROID_HOME=/Users/alexbeattie/Library/Android/sdk ./gradlew :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:assembleDebug :app:lintDebug --no-daemon
```

Result: `BUILD SUCCESSFUL`; 182 unit tests, 0 failures, 0 errors; debug Android-test sources compiled; debug APK assembled; lint reported 0 errors and 87 pre-existing warnings.

No APK was installed or removed and no connected test was run, so the requested post-integration screenshot smoke remains with the integration owner.
