# Android Persistent Discovery Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (- [ ]) syntax for tracking.

**Goal:** Deliver a buildable Android app that persists the same core profile as the iPhone app, gates first launch through onboarding, and gives Home, Map, and List one reliable provider-discovery session against the deployed KiNDD API.

**Architecture:** A Preferences DataStore repository owns the persisted profile. An application-scoped DiscoveryStore owns transient provider criteria and results, delegates a pure request decision to DiscoveryRequestPlanner, and applies latest-request-wins orchestration. A root AppEntryViewModel switches whole onboarding and main graphs only after hydration; focused screen ViewModels translate repository/store state into Compose UI.

**Tech Stack:** Kotlin 1.9.22, Jetpack Compose with the 2024.04.00 BOM, Material 3, Hilt 2.50, coroutines 1.7.3, Preferences DataStore 1.0.0, Retrofit/OkHttp 2.9.0/4.12.0, Google Maps Compose 4.3.0, JUnit 4, MockWebServer, and AndroidX Compose UI tests.

**Approved design:** [Android Persistent Discovery Foundation Design](../specs/2026-07-20-android-persistent-discovery-foundation-design.md)

## Global Constraints

- The repository is broadly dirty. Never use broad staging commands. Before every commit, inspect status and stage only the exact paths named in that task.
- Preserve the already verified Android API repair as its own first commit. Do not overwrite or fold unrelated root, backend, web, or iOS changes into Android commits.
- Use Android Studio's bundled JBR for every Gradle command. Commands may run in separate shells, so use this exact inline assignment every time rather than relying on an exported variable:

~~~bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew ...
~~~

- Run Gradle from chla-android.
- Follow red-green-refactor within every task: add the named failing test, run it and confirm the expected failure, implement only enough production code, rerun the focused test, then run the task's compile/build check.
- Keep user-facing product copy as KiNDD or KiNDD - NDD Resource Navigator. Do not add CHLA or Children's Hospital Los Angeles to strings, copy, docs, or examples. Do not add emojis.
- Persist explicit stable values, never Kotlin enum names or ordinals:
  - Audience: family, clinician
  - Journey: justDiagnosed, waitingIntake, receivingServices, exploring
  - Age: 0-5, 6-12, 13-18, 19+, All Ages
- Keep null age distinct from the real All Ages value.
- Use these exact canonical therapy API values:
  - ABA therapy
  - Speech therapy
  - Occupational therapy
  - Physical therapy
  - Feeding therapy
  - Parent child interaction therapy/parent training behavior management
- Derive only these regional-center short names: NLACRC, WRC, SCLARC, ELARC, HRC, FDLRC, SGPRC. Normalize SG/PRC to SGPRC and map Lanterman to FDLRC.
- Comprehensive search may repeat therapy query parameters. The deployed ZIP endpoint accepts one therapy only, so the ZIP path must omit therapy from the request and apply all selected therapy values locally. Do not change backend behavior in this plan.
- An unmatched or failed ZIP entered from Home must leave the saved profile unchanged. First-run onboarding may continue with a valid five-digit ZIP and no matched center.
- Set HTTP body/URL logging to none before any profile-derived request ships. Do not log URLs, query strings, ZIP codes, coordinates, filter values, profile fields, response bodies, exception messages, or chat text.
- No task is complete with compilation alone. Its focused tests and stated verification command must pass.

## Canonical Provider Request Matrix

| Criteria origin | Remote request | Local work |
| --- | --- | --- |
| Profile ZIP | providers-v2/by_regional_center/ with zip_code, age, diagnosis, insurance; never therapy | Apply all selected therapies with AND semantics, then text-match name, address, city, description, therapies, and insurance |
| Device location | providers-v2/comprehensive_search/ with q, lat, lng, radius, repeated therapy, age, diagnosis, insurance | Compute distance, sort nearest first, cap results |
| Los Angeles plus query/filters | providers-v2/comprehensive_search/ without coordinates, with active q and filters | Cap results |
| Los Angeles with no query/filters | providers-v2/ catalog | Cap results |

The List surface retains every result. Map derives the coordinate-bearing subset only; it never invents zero coordinates.

---

### Task 1: Preserve and Commit the Verified Android API Repair

**Files:**

- Modify: chla-android/app/build.gradle.kts
- Modify: chla-android/app/src/main/java/com/chla/kindd/data/api/KINDDApi.kt
- Modify: chla-android/app/src/main/java/com/chla/kindd/data/models/Provider.kt
- Modify: chla-android/app/src/main/java/com/chla/kindd/data/models/RegionalCenter.kt
- Create: chla-android/app/src/main/java/com/chla/kindd/data/models/StringListJsonAdapter.kt
- Modify: chla-android/app/src/main/java/com/chla/kindd/data/repository/ProviderRepository.kt
- Modify: chla-android/app/src/main/java/com/chla/kindd/data/repository/RegionalCenterRepository.kt
- Create: chla-android/app/src/test/java/com/chla/kindd/data/api/KINDDApiContractTest.kt
- Create: chla-android/app/src/test/java/com/chla/kindd/data/models/ProviderJsonTest.kt
- Create: chla-android/app/src/test/java/com/chla/kindd/data/models/RegionalCenterJsonTest.kt

- [ ] **Step 1: Inventory the tracked and untracked baseline repair**

Run from the repository root:

~~~bash
git status --short
git diff --check -- \
  chla-android/app/build.gradle.kts \
  chla-android/app/src/main/java/com/chla/kindd/data/api/KINDDApi.kt \
  chla-android/app/src/main/java/com/chla/kindd/data/models/Provider.kt \
  chla-android/app/src/main/java/com/chla/kindd/data/models/RegionalCenter.kt \
  chla-android/app/src/main/java/com/chla/kindd/data/repository/ProviderRepository.kt \
  chla-android/app/src/main/java/com/chla/kindd/data/repository/RegionalCenterRepository.kt
for KINDD_BASELINE_FILE in \
  chla-android/app/src/main/java/com/chla/kindd/data/models/StringListJsonAdapter.kt \
  chla-android/app/src/test/java/com/chla/kindd/data/api/KINDDApiContractTest.kt \
  chla-android/app/src/test/java/com/chla/kindd/data/models/ProviderJsonTest.kt \
  chla-android/app/src/test/java/com/chla/kindd/data/models/RegionalCenterJsonTest.kt
do
  sed -n '1,280p' "$KINDD_BASELINE_FILE"
done
~~~

Expected: git status enumerates both tracked and untracked baseline paths, the printed untracked files contain only the adapter/tests described by this task, and git diff --check emits no output. Unrelated dirty files remain untouched.

- [ ] **Step 2: Re-run the focused contract/model suite**

~~~bash
cd chla-android
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest \
  --tests 'com.chla.kindd.data.api.KINDDApiContractTest' \
  --tests 'com.chla.kindd.data.models.ProviderJsonTest' \
  --tests 'com.chla.kindd.data.models.RegionalCenterJsonTest' \
  --no-daemon
~~~

Expected: 12 tests pass, proving flexible string/list JSON decoding, deployed endpoints and envelopes, Los Angeles center filtering, client result caps, and nearest-first ordering.

- [ ] **Step 3: Re-run baseline lint and assembly**

~~~bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:lintDebug :app:assembleDebug --no-daemon
~~~

Expected: BUILD SUCCESSFUL and lint has zero errors. Existing warnings may remain.

- [ ] **Step 4: Commit only the baseline repair**

~~~bash
cd ..
git add \
  chla-android/app/build.gradle.kts \
  chla-android/app/src/main/java/com/chla/kindd/data/api/KINDDApi.kt \
  chla-android/app/src/main/java/com/chla/kindd/data/models/Provider.kt \
  chla-android/app/src/main/java/com/chla/kindd/data/models/RegionalCenter.kt \
  chla-android/app/src/main/java/com/chla/kindd/data/models/StringListJsonAdapter.kt \
  chla-android/app/src/main/java/com/chla/kindd/data/repository/ProviderRepository.kt \
  chla-android/app/src/main/java/com/chla/kindd/data/repository/RegionalCenterRepository.kt \
  chla-android/app/src/test/java/com/chla/kindd/data/api/KINDDApiContractTest.kt \
  chla-android/app/src/test/java/com/chla/kindd/data/models/ProviderJsonTest.kt \
  chla-android/app/src/test/java/com/chla/kindd/data/models/RegionalCenterJsonTest.kt
git diff --cached --check
git diff --cached --stat
git commit -m "fix(android): align deployed API contracts"
~~~

Expected: one Android-only commit; every unrelated worktree change remains unstaged.

---

### Task 2: Add the Deterministic Test Harness and Remove Sensitive Network Logging

**Files:**

- Modify: chla-android/app/build.gradle.kts
- Modify: chla-android/app/src/main/java/com/chla/kindd/di/NetworkModule.kt
- Create: chla-android/app/src/test/java/com/chla/kindd/testing/MainDispatcherRule.kt
- Create: chla-android/app/src/test/java/com/chla/kindd/di/NetworkModuleTest.kt

- [ ] **Step 1: Add a failing network privacy test**

Create NetworkModuleTest with this contract:

~~~kotlin
@Test
fun okHttpClient_hasNoHttpLoggingInterceptor() {
    val client = NetworkModule.provideOkHttpClient()

    assertTrue(client.interceptors.none {
        it.javaClass.name == "okhttp3.logging.HttpLoggingInterceptor"
    })
    assertTrue(client.networkInterceptors.none {
        it.javaClass.name == "okhttp3.logging.HttpLoggingInterceptor"
    })
}
~~~

The assertion intentionally compares a class-name string, so the green test still compiles after the logging-interceptor runtime dependency is removed.

Run:

~~~bash
cd chla-android
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest \
  --tests 'com.chla.kindd.di.NetworkModuleTest' --no-daemon
~~~

Expected red result: the current client contains HttpLoggingInterceptor.

- [ ] **Step 2: Make the minimal dependency and client changes**

In app/build.gradle.kts:

- add implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
- add testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
- add testImplementation("androidx.datastore:datastore-preferences-core:1.0.0")
- add androidTestImplementation("androidx.test:core-ktx:1.5.0")
- add androidTestImplementation("androidx.test:runner:1.5.2")
- add androidTestImplementation("androidx.navigation:navigation-testing:2.7.6")
- remove implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

In NetworkModule, remove HttpLoggingInterceptor and BuildConfig imports related only to logging, remove interceptor construction, and build the client with timeouts only. Do not substitute BASIC logging because it exposes full query-bearing URLs.

- [ ] **Step 3: Add the reusable coroutine rule**

MainDispatcherRule must accept a TestDispatcher, set Dispatchers.Main in starting, and reset it in finished. Use StandardTestDispatcher by default.

- [ ] **Step 4: Run focused and compile checks**

~~~bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew \
  :app:testDebugUnitTest \
  --tests 'com.chla.kindd.di.NetworkModuleTest' \
  --no-daemon
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew \
  :app:compileDebugAndroidTestKotlin \
  --no-daemon
~~~

Expected: the privacy test passes and both source sets compile.

- [ ] **Step 5: Commit**

~~~bash
cd ..
git add \
  chla-android/app/build.gradle.kts \
  chla-android/app/src/main/java/com/chla/kindd/di/NetworkModule.kt \
  chla-android/app/src/test/java/com/chla/kindd/testing/MainDispatcherRule.kt \
  chla-android/app/src/test/java/com/chla/kindd/di/NetworkModuleTest.kt
git diff --cached --check
git commit -m "test(android): add deterministic privacy-safe harness"
~~~

---

### Task 3: Implement the Typed Persisted Profile and Backup Exclusions

**Files:**

- Create: chla-android/app/src/main/java/com/chla/kindd/data/profile/ProfileModels.kt
- Create: chla-android/app/src/main/java/com/chla/kindd/data/profile/UserProfileRepository.kt
- Create: chla-android/app/src/main/java/com/chla/kindd/data/profile/UserProfilePreferences.kt
- Create: chla-android/app/src/main/java/com/chla/kindd/data/profile/DataStoreUserProfileRepository.kt
- Create: chla-android/app/src/main/java/com/chla/kindd/di/ProfileModule.kt
- Modify: chla-android/app/src/main/res/xml/backup_rules.xml
- Modify: chla-android/app/src/main/res/xml/data_extraction_rules.xml
- Create: chla-android/app/src/test/java/com/chla/kindd/data/profile/ProfileModelsTest.kt
- Create: chla-android/app/src/test/java/com/chla/kindd/data/profile/DataStoreUserProfileRepositoryTest.kt
- Create: chla-android/app/src/androidTest/java/com/chla/kindd/data/profile/ProfileBackupRulesTest.kt

- [ ] **Step 1: Write failing stable-value and profile-validity tests**

ProfileModelsTest must prove:

- AudienceType.FAMILY encodes family and CLINICIAN encodes clinician.
- Journey stages encode justDiagnosed, waitingIntake, receivingServices, exploring.
- AgeGroup encodes 0-5, 6-12, 13-18, 19+, All Ages.
- UserProfile.isComplete is true only when onboardingCompleted is true and audience, a five-digit ASCII ZIP, and journey are present.
- RegionalCenterIdentity.from maps the deployed ID/name and canonical short name.
- Lanterman maps to FDLRC; SG/PRC and San Gabriel/Pomona map to SGPRC.

Use explicit fromStorageValue functions that return null for unknown values.

- [ ] **Step 2: Define the domain model**

ProfileModels.kt must contain:

~~~kotlin
enum class AudienceType(val storageValue: String)
enum class JourneyStage(val storageValue: String)
enum class AgeGroup(val apiValue: String)

data class RegionalCenterIdentity(
    val id: Int,
    val name: String,
    val shortName: String
)

data class UserProfile(
    val onboardingCompleted: Boolean = false,
    val audienceType: AudienceType? = null,
    val zipCode: String? = null,
    val regionalCenter: RegionalCenterIdentity? = null,
    val journeyStage: JourneyStage? = null,
    val ageGroup: AgeGroup? = null
) {
    val isComplete: Boolean
}
~~~

Do not derive persisted values with enum name transforms.

- [ ] **Step 3: Write failing DataStore replacement tests**

Use PreferenceDataStoreFactory.create with a TemporaryFolder-backed file and a fresh TestScope per test. Cancel the first DataStore scope before recreating a repository on the same file so two active DataStore instances never target one path. Cover:

1. Empty storage emits UserProfile() and incomplete.
2. replaceProfile writes every field and a newly created repository restores them.
3. Replacing a matched profile with regionalCenter null removes all three old center keys.
4. Unknown audience/journey/age and an incomplete regional-center tuple decode to null.
5. A malformed ZIP makes isComplete false even if the stored completion flag is true.
6. clearProfile removes every key and emits UserProfile().

- [ ] **Step 4: Implement one atomic Preferences DataStore repository**

Use the fixed name and keys:

~~~kotlin
const val PROFILE_DATASTORE_NAME = "user_profile"

onboarding_completed
audience_type
zip_code
regional_center_id
regional_center_name
regional_center_short_name
journey_stage
age_group
~~~

UserProfileRepository exposes:

~~~kotlin
interface UserProfileRepository {
    val profile: Flow<UserProfile>
    suspend fun replaceProfile(profile: UserProfile)
    suspend fun clearProfile()
}
~~~

Create one Context.userProfileDataStore delegate with ReplaceFileCorruptionHandler { emptyPreferences() }. For store.data, emit emptyPreferences only for IOException and rethrow every other failure. In replaceProfile, execute one edit transaction, clear first, then write the complete replacement. clearProfile also uses one edit { clear() } transaction. Do not wrap DataStore work in withContext.

ProfileModule must expose that exact singleton DataStore<Preferences> under a UserProfileStore qualifier and provide one singleton DataStoreUserProfileRepository.

- [ ] **Step 5: Write failing backup-contract instrumentation tests**

ProfileBackupRulesTest must:

- prove context.preferencesDataStoreFile(PROFILE_DATASTORE_NAME), relative to filesDir, is datastore/user_profile.preferences_pb;
- parse R.xml.backup_rules and prove its complete include set is only sharedpref path `.`, with only sharedpref/device.xml excluded;
- parse the cloud-backup section of R.xml.data_extraction_rules and prove its complete include set is only sharedpref path `.`, with only sharedpref/device.xml excluded;
- prove the device-transfer section has no include allowlist and explicitly excludes exactly the file-domain path datastore/user_profile.preferences_pb.

- [ ] **Step 6: Use cloud allowlists and an explicit device-transfer exclusion**

backup_rules.xml and the cloud-backup section of data_extraction_rules.xml must allow only:

~~~xml
<include domain="sharedpref" path="."/>
<exclude domain="sharedpref" path="device.xml"/>
~~~

Because these are include allowlists, the profile DataStore file is not cloud-backup eligible. Do not add a file-domain include or a redundant file-domain exclude to either cloud section.

The device-transfer section has no include allowlist and must explicitly contain:

~~~xml
<exclude domain="file" path="datastore/user_profile.preferences_pb" />
~~~

This preserves the privacy design: the profile DataStore is excluded from both cloud backup and device transfer.

- [ ] **Step 7: Run focused verification**

~~~bash
cd chla-android
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest \
  --tests 'com.chla.kindd.data.profile.ProfileModelsTest' \
  --tests 'com.chla.kindd.data.profile.DataStoreUserProfileRepositoryTest' \
  --no-daemon

JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:compileDebugAndroidTestKotlin --no-daemon
~~~

Expected: profile unit tests pass and backup-contract instrumentation tests compile. Run the device test in Task 13.

- [ ] **Step 8: Commit**

~~~bash
cd ..
git add \
  chla-android/app/src/main/java/com/chla/kindd/data/profile \
  chla-android/app/src/main/java/com/chla/kindd/di/ProfileModule.kt \
  chla-android/app/src/main/res/xml/backup_rules.xml \
  chla-android/app/src/main/res/xml/data_extraction_rules.xml \
  chla-android/app/src/test/java/com/chla/kindd/data/profile \
  chla-android/app/src/androidTest/java/com/chla/kindd/data/profile
git diff --cached --check
git commit -m "feat(android): persist the KiNDD user profile"
~~~

---

### Task 4: Introduce Exact Provider and Regional-Center Data Contracts

**Files:**

- Create: chla-android/app/src/main/java/com/chla/kindd/data/discovery/ProviderRequests.kt
- Create: chla-android/app/src/main/java/com/chla/kindd/data/source/ProviderDiscoveryDataSource.kt
- Create: chla-android/app/src/main/java/com/chla/kindd/data/source/RegionalCenterDataSource.kt
- Create: chla-android/app/src/main/java/com/chla/kindd/di/CoroutineQualifiers.kt
- Create: chla-android/app/src/main/java/com/chla/kindd/di/CoroutineModule.kt
- Modify: chla-android/app/src/main/java/com/chla/kindd/data/api/KINDDApi.kt
- Modify: chla-android/app/src/main/java/com/chla/kindd/data/models/RegionalCenter.kt
- Modify: chla-android/app/src/main/java/com/chla/kindd/data/repository/ProviderRepository.kt
- Modify: chla-android/app/src/main/java/com/chla/kindd/data/repository/RegionalCenterRepository.kt
- Modify: chla-android/app/src/main/java/com/chla/kindd/di/RepositoryModule.kt
- Modify: chla-android/app/src/test/java/com/chla/kindd/data/api/KINDDApiContractTest.kt
- Modify: chla-android/app/src/test/java/com/chla/kindd/data/models/RegionalCenterJsonTest.kt
- Create: chla-android/app/src/test/java/com/chla/kindd/data/repository/RepositoryCancellationTest.kt

- [ ] **Step 1: Add failing comprehensive and ZIP request contract tests**

Extend KINDDApiContractTest with MockWebServer tests that assert decoded query parameters, not fragile full query ordering:

- comprehensive q=ABA;
- paired lat/lng and radius=15;
- LA comprehensive search without coordinates omits lat, lng, and radius;
- therapy appears twice and preserves both canonical values;
- age, diagnosis, and insurance use those exact names;
- ZIP request uses zip_code, age, diagnosis, insurance and the Retrofit method exposes no therapy argument;
- ZIP request contains no therapy parameter when called by the discovery path;
- result caps are enforced client-side after decoding.

Also change the existing nearby/text tests so the result cap is not dependent on a backend honoring limit.

- [ ] **Step 2: Define request/data-source types**

ProviderRequests.kt:

~~~kotlin
data class ComprehensiveProviderRequest(
    val query: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMiles: Int = 15,
    val therapyTypes: List<String> = emptyList(),
    val ageGroup: String? = null,
    val diagnosis: String? = null,
    val insurance: String? = null
) {
    init {
        require((latitude == null) == (longitude == null))
    }
}

data class RegionalCenterProviderRequest(
    val zipCode: String,
    val ageGroup: String? = null,
    val diagnosis: String? = null,
    val insurance: String? = null
)
~~~

ProviderDiscoveryDataSource must expose catalog, comprehensive, and regional-center searches with an explicit client limit. RegionalCenterDataSource must expose center list, nearby list, and:

~~~kotlin
sealed interface RegionalCenterLookup {
    data class Matched(val center: RegionalCenter) : RegionalCenterLookup
    data object Unmatched : RegionalCenterLookup
    data class Unavailable(val reason: LookupFailure) : RegionalCenterLookup
}
~~~

LookupFailure is the sanitized enum NETWORK, TIMEOUT, SERVER, UNKNOWN. It contains no Throwable message.

- [ ] **Step 3: Add coroutine injection**

CoroutineQualifiers.kt defines ApplicationScope, IoDispatcher, and DefaultDispatcher qualifiers. CoroutineModule provides Dispatchers.IO, Dispatchers.Default, and a singleton CoroutineScope(SupervisorJob() + defaultDispatcher).

- [ ] **Step 4: Implement the exact Retrofit/repository surface**

Replace the split comprehensive methods in KINDDApi with one optional-parameter call using q, lat, lng, radius, repeated therapy, age, diagnosis, and insurance. ProviderRepository sends radius only when both coordinates are present and rejects a half-coordinate request. Replace the ZIP API method with zip_code, insurance, age, and diagnosis only; remove its misleading therapy list argument. Retain ProviderRepository.getProviders, getProvidersNearby, searchProviders, and getProvider as compatibility wrappers so the current Map/List/detail callers compile through Task 11; make them delegate to the new implementations rather than duplicate network logic.

ProviderRepository implements ProviderDiscoveryDataSource, injects the IO dispatcher, sorts location results after Haversine calculation, and caps results after decoding. RegionalCenterRepository implements RegionalCenterDataSource, maps HTTP 404 to Unmatched, maps network/server failures to sanitized Unavailable categories, and keeps Los Angeles list filtering. Name the typed method lookupRegionalCenter(zipCode); retain the existing getRegionalCenterByZip(zipCode): Result<RegionalCenter> compatibility wrapper until RegionalCentersViewModel migrates in Task 10, then delete the wrapper.

Every repository catch block must rethrow CancellationException before mapping other exceptions. Do not log exception messages or request data.

- [ ] **Step 5: Lock regional-center normalization**

Fix RegionalCenter.shortName so Lanterman returns FDLRC, all known center names return the seven canonical values, and SG/PRC normalizes to SGPRC. Extend RegionalCenterJsonTest for every mapping.

- [ ] **Step 6: Add cancellation tests**

RepositoryCancellationTest uses a fake KINDDApi that throws CancellationException and proves provider and regional-center repository methods rethrow it instead of returning Result.failure or Unavailable.

- [ ] **Step 7: Wire Hilt and run focused checks**

RepositoryModule provides concrete repositories and binds the same singleton instances to ProviderDiscoveryDataSource and RegionalCenterDataSource. Do not construct duplicate repository objects.

~~~bash
cd chla-android
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest \
  --tests 'com.chla.kindd.data.api.KINDDApiContractTest' \
  --tests 'com.chla.kindd.data.models.RegionalCenterJsonTest' \
  --tests 'com.chla.kindd.data.repository.RepositoryCancellationTest' \
  --no-daemon
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:assembleDebug --no-daemon
~~~

Expected: exact query contracts, canonical short names, cancellation propagation, and assembly pass.

- [ ] **Step 8: Commit**

~~~bash
cd ..
git add \
  chla-android/app/src/main/java/com/chla/kindd/data/api/KINDDApi.kt \
  chla-android/app/src/main/java/com/chla/kindd/data/discovery/ProviderRequests.kt \
  chla-android/app/src/main/java/com/chla/kindd/data/models/RegionalCenter.kt \
  chla-android/app/src/main/java/com/chla/kindd/data/repository/ProviderRepository.kt \
  chla-android/app/src/main/java/com/chla/kindd/data/repository/RegionalCenterRepository.kt \
  chla-android/app/src/main/java/com/chla/kindd/data/source \
  chla-android/app/src/main/java/com/chla/kindd/di/CoroutineModule.kt \
  chla-android/app/src/main/java/com/chla/kindd/di/CoroutineQualifiers.kt \
  chla-android/app/src/main/java/com/chla/kindd/di/RepositoryModule.kt \
  chla-android/app/src/test/java/com/chla/kindd/data/api/KINDDApiContractTest.kt \
  chla-android/app/src/test/java/com/chla/kindd/data/models/RegionalCenterJsonTest.kt \
  chla-android/app/src/test/java/com/chla/kindd/data/repository/RepositoryCancellationTest.kt
git diff --cached --check
git commit -m "refactor(android): type discovery data contracts"
~~~

---

### Task 5: Build the Pure Discovery Model and Request Planner

**Files:**

- Create: chla-android/app/src/main/java/com/chla/kindd/data/discovery/DiscoveryCatalog.kt
- Create: chla-android/app/src/main/java/com/chla/kindd/data/discovery/DiscoveryModels.kt
- Create: chla-android/app/src/main/java/com/chla/kindd/data/discovery/DiscoveryError.kt
- Create: chla-android/app/src/main/java/com/chla/kindd/data/discovery/DiscoveryRequestPlanner.kt
- Create: chla-android/app/src/test/java/com/chla/kindd/data/discovery/DiscoveryCatalogTest.kt
- Create: chla-android/app/src/test/java/com/chla/kindd/data/discovery/DiscoveryRequestPlannerTest.kt

- [ ] **Step 1: Write failing catalog tests**

DiscoveryCatalogTest must assert the exact API values and order for:

~~~text
Age:
0-5
6-12
13-18
19+
All Ages

Diagnosis:
Autism Spectrum Disorder
Global Development Delay
Intellectual Disability
Speech and Language Disorder
Other

Insurance:
Regional Center
Private Pay
Medi-Cal
Medicare
Blue Cross
Blue Shield
Anthem
Aetna
Cigna
Kaiser Permanente
United Healthcare
Health Net
Molina
L.A. Care
Covered California
~~~

TherapyType is a typed enum/value class with the six canonical API values from Global Constraints. Display copy must come from resource IDs, not from apiValue.

- [ ] **Step 2: Define discovery state**

DiscoveryModels.kt must define:

~~~kotlin
sealed interface DiscoveryOrigin {
    data class ProfileZip(val zipCode: String) : DiscoveryOrigin
    data class DeviceLocation(
        val latitude: Double,
        val longitude: Double
    ) : DiscoveryOrigin
    data object LosAngelesCatalog : DiscoveryOrigin
}

data class DiscoveryCriteria(
    val query: String = "",
    val therapyTypes: Set<TherapyType> = emptySet(),
    val ageGroup: AgeGroup? = null,
    val diagnosis: String? = null,
    val insurance: String? = null,
    val radiusMiles: Int = 15,
    val origin: DiscoveryOrigin = DiscoveryOrigin.LosAngelesCatalog
)

data class DiscoveryState(
    val profile: UserProfile = UserProfile(),
    val criteria: DiscoveryCriteria = DiscoveryCriteria(),
    val providers: List<Provider> = emptyList(),
    val isLoading: Boolean = false,
    val error: DiscoveryError? = null,
    val hasLoadedOnce: Boolean = false,
    val lastSuccessfulRequestKey: String? = null
) {
    val mapProviders: List<Provider>
}
~~~

DiscoveryError is a closed, localized category: NETWORK, TIMEOUT, SERVER, UNKNOWN. It never carries a backend body, URL, ZIP, filter, exception string, or stack-oriented detail.

- [ ] **Step 3: Write the failing request-decision table**

DiscoveryRequestPlannerTest must cover:

1. ProfileZip always produces PlannedDiscoveryRequest.ProfileZip.
2. ProfileZip request includes ZIP, age, diagnosis, insurance and excludes therapies.
3. ProfileZip local filter retains providers only when every selected therapy is present.
4. ProfileZip local query is case-insensitive across name, full address, city, description, therapy types, and normalized insurance.
5. DeviceLocation produces comprehensive request with paired coordinates, radius, query, every therapy, age, diagnosis, insurance.
6. LA plus nonblank query produces comprehensive request.
7. LA plus any non-radius filter produces comprehensive request.
8. LA with defaults produces Catalog.
9. Whitespace-only query normalizes to null.
10. Request keys are deterministic when a Set is inserted in a different order.

- [ ] **Step 4: Implement a pure planner**

Define:

~~~kotlin
sealed interface PlannedDiscoveryRequest {
    data class ProfileZip(
        val remote: RegionalCenterProviderRequest,
        val query: String?,
        val therapies: Set<TherapyType>
    ) : PlannedDiscoveryRequest
    data class Comprehensive(
        val remote: ComprehensiveProviderRequest
    ) : PlannedDiscoveryRequest
    data object Catalog : PlannedDiscoveryRequest
}
~~~

DiscoveryRequestPlanner has an @Inject constructor but contains no coroutine, Android, network, or mutable state dependency. DiscoveryStore receives it through Hilt while unit tests instantiate it directly. Keep local ProfileZip filtering in a named applyLocalFilters function that uses AND semantics for multiple therapies.

- [ ] **Step 5: Run and commit**

~~~bash
cd chla-android
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest \
  --tests 'com.chla.kindd.data.discovery.DiscoveryCatalogTest' \
  --tests 'com.chla.kindd.data.discovery.DiscoveryRequestPlannerTest' \
  --no-daemon
cd ..
git add \
  chla-android/app/src/main/java/com/chla/kindd/data/discovery/DiscoveryCatalog.kt \
  chla-android/app/src/main/java/com/chla/kindd/data/discovery/DiscoveryModels.kt \
  chla-android/app/src/main/java/com/chla/kindd/data/discovery/DiscoveryError.kt \
  chla-android/app/src/main/java/com/chla/kindd/data/discovery/DiscoveryRequestPlanner.kt \
  chla-android/app/src/test/java/com/chla/kindd/data/discovery/DiscoveryCatalogTest.kt \
  chla-android/app/src/test/java/com/chla/kindd/data/discovery/DiscoveryRequestPlannerTest.kt
git diff --cached --check
git commit -m "feat(android): define provider discovery planning"
~~~

Expected: the full request matrix is deterministic without touching the network.

---

### Task 6: Implement the Application-Scoped Latest-Wins Discovery Store

**Files:**

- Create: chla-android/app/src/main/java/com/chla/kindd/data/discovery/DiscoveryController.kt
- Create: chla-android/app/src/main/java/com/chla/kindd/data/discovery/DiscoveryStore.kt
- Modify: chla-android/app/src/main/java/com/chla/kindd/di/RepositoryModule.kt
- Create: chla-android/app/src/test/java/com/chla/kindd/data/discovery/FakeProviderDiscoveryDataSource.kt
- Create: chla-android/app/src/test/java/com/chla/kindd/data/profile/FakeUserProfileRepository.kt
- Create: chla-android/app/src/test/java/com/chla/kindd/data/discovery/DiscoveryStoreTest.kt

- [ ] **Step 1: Write failing store behavior tests with fakes**

Use runTest, the test scope as applicationScope, StandardTestDispatcher, and controllable fake responses. Cover:

1. A completed persisted profile seeds ProfileZip origin and profile age, then loads exactly once.
2. An incomplete profile updates state but performs no provider request.
3. A criteria-relevant profile change updates origin/age and refreshes exactly once.
4. An audience/journey-only profile change updates state without a duplicate refresh.
5. setQuery updates criteria synchronously, cancels/supersedes the prior generation immediately, waits 300 ms, then requests.
6. Clearing query requests immediately rather than waiting for debounce.
7. Applying filters, changing location, Home therapy shortcut, refresh, and retry request immediately.
8. Two rapid requests cannot let the slower old result overwrite the new result, even if the fake ignores cancellation.
9. CancellationException never becomes DiscoveryError.
10. Initial failure yields a full error state with no leaked exception text.
11. Refresh failure preserves existing providers and exposes a retryable error.
12. A successful empty result sets hasLoadedOnce and produces an empty state, not an initial loading state.
13. Map provider IDs are the coordinate-bearing subset of the exact List provider IDs.
14. When the profile becomes incomplete, the store immediately advances generation, cancels in-flight work, and resets providers, criteria, origin, loading, errors, and request key.
15. After that reset, completing onboarding with a new profile seeds a clean ProfileZip/age state and cannot accept the old request's late response.
16. Repeated ensureLoaded calls make one attempt for the current key, and a failed attempt repeats only after explicit retry.

- [ ] **Step 2: Define the controller contract**

DiscoveryController exposes StateFlow<DiscoveryState> and explicit methods:

~~~kotlin
fun ensureLoaded()
fun setQuery(query: String)
fun applyFilters(
    therapyTypes: Set<TherapyType>,
    ageGroup: AgeGroup?,
    diagnosis: String?,
    insurance: String?,
    radiusMiles: Int
)
fun setSingleTherapyAndRefresh(therapyType: TherapyType)
fun useDeviceLocation(latitude: Double, longitude: Double)
fun useLosAngelesCatalog()
fun refresh()
fun retry()
fun clearAllFilters()
~~~

Every method that changes criteria updates StateFlow synchronously before it schedules work. This guarantees that Home can set a therapy and only then emit List navigation.

- [ ] **Step 3: Implement orchestration**

DiscoveryStore is @Singleton and injects ProviderDiscoveryDataSource, UserProfileRepository, @ApplicationScope CoroutineScope, @IoDispatcher CoroutineDispatcher, and DiscoveryRequestPlanner.

Required mechanics:

- observe profile for process lifetime;
- seed ProfileZip and profile age for a complete profile;
- refresh only when ZIP or age actually changes;
- never independently double-refresh a Home update;
- track an internal lastAttemptedRequestKey and make ensureLoaded a no-op while that key is active or has already been attempted, including failure; only explicit retry repeats a failed key;
- treat an incomplete profile emission as a privacy reset: advance generation, cancel the active job, and replace all transient discovery fields with defaults before publishing that profile;
- keep one requestJob;
- increment an AtomicLong generation before every scheduled request, including during the debounce window;
- cancel the prior job on every criteria change;
- delay exactly 300 ms only for a nonblank typed query;
- check generation before every loading, success, and error mutation;
- call the pure planner and one ProviderDiscoveryDataSource method;
- apply ProfileZip local query/therapy filtering after remote success;
- preserve old providers during refresh;
- sanitize errors by type;
- rethrow CancellationException.

Use the injected IO dispatcher around provider work and return to synchronized MutableStateFlow updates. lastSuccessfulRequestKey comes from normalized planned request data and never includes raw profile text in logs.

- [ ] **Step 4: Bind one singleton controller**

RepositoryModule must bind the singleton DiscoveryStore as DiscoveryController without constructing a second store.

- [ ] **Step 5: Run focused and full existing unit tests**

~~~bash
cd chla-android
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest \
  --tests 'com.chla.kindd.data.discovery.DiscoveryStoreTest' \
  --no-daemon
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest --no-daemon
~~~

Expected: all store scenarios and the repaired baseline suite pass.

- [ ] **Step 6: Commit**

~~~bash
cd ..
git add \
  chla-android/app/src/main/java/com/chla/kindd/data/discovery/DiscoveryController.kt \
  chla-android/app/src/main/java/com/chla/kindd/data/discovery/DiscoveryStore.kt \
  chla-android/app/src/main/java/com/chla/kindd/di/RepositoryModule.kt \
  chla-android/app/src/test/java/com/chla/kindd/data/discovery/FakeProviderDiscoveryDataSource.kt \
  chla-android/app/src/test/java/com/chla/kindd/data/discovery/DiscoveryStoreTest.kt \
  chla-android/app/src/test/java/com/chla/kindd/data/profile/FakeUserProfileRepository.kt
git diff --cached --check
git commit -m "feat(android): share latest-wins discovery state"
~~~

---

### Task 7: Add the Hydration-Aware Application Entry State

**Files:**

- Create: chla-android/app/src/main/java/com/chla/kindd/ui/app/AppEntryState.kt
- Create: chla-android/app/src/main/java/com/chla/kindd/ui/app/AppEntryViewModel.kt
- Create: chla-android/app/src/main/java/com/chla/kindd/ui/app/LaunchScreen.kt
- Create: chla-android/app/src/test/java/com/chla/kindd/ui/app/AppEntryViewModelTest.kt
- Create: chla-android/app/src/androidTest/java/com/chla/kindd/ui/app/AppEntryContentTest.kt

- [ ] **Step 1: Write failing entry-state tests**

AppEntryViewModelTest uses FakeUserProfileRepository and MainDispatcherRule to prove:

1. state starts as Loading before the repository flow emits;
2. incomplete/default profile maps to NeedsOnboarding(profile);
3. a complete profile maps to Ready(profile);
4. clearing after Ready maps back to NeedsOnboarding;
5. profile-flow failure falls back to NeedsOnboarding(default profile), never an unbounded loading screen.

- [ ] **Step 2: Implement the state machine**

~~~kotlin
sealed interface AppEntryState {
    data object Loading : AppEntryState
    data class NeedsOnboarding(val draft: UserProfile) : AppEntryState
    data class Ready(val profile: UserProfile) : AppEntryState
}
~~~

Map repository.profile with stateIn(viewModelScope, SharingStarted.Eagerly, Loading). Use profile.isComplete rather than the raw completion bit.

LaunchScreen is a neutral, accessible KiNDD surface with no Home or onboarding content. Add stable test tags app_entry_loading, app_entry_onboarding, and app_entry_main to the stateless AppEntryContent switch so the root behavior can be tested without a Hilt instrumentation runner.

- [ ] **Step 3: Write and run the no-Home-flash UI test**

AppEntryContentTest composes each explicit state and asserts:

- Loading shows app_entry_loading and not app_entry_main;
- NeedsOnboarding shows only the onboarding slot;
- Ready shows only the main slot.

~~~bash
cd chla-android
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest \
  --tests 'com.chla.kindd.ui.app.AppEntryViewModelTest' \
  --no-daemon
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:compileDebugAndroidTestKotlin --no-daemon
~~~

- [ ] **Step 4: Commit**

~~~bash
cd ..
git add \
  chla-android/app/src/main/java/com/chla/kindd/ui/app \
  chla-android/app/src/test/java/com/chla/kindd/ui/app \
  chla-android/app/src/androidTest/java/com/chla/kindd/ui/app
git diff --cached --check
git commit -m "feat(android): gate app entry on profile hydration"
~~~

Do not connect MainActivity yet; the current app remains runnable until the real onboarding flow exists.

---

### Task 8: Build the Five-Step Onboarding and Nondestructive Profile Editor

**Files:**

- Create: chla-android/app/src/main/java/com/chla/kindd/data/source/UserLocationSource.kt
- Modify: chla-android/app/src/main/java/com/chla/kindd/services/LocationService.kt
- Modify: chla-android/app/src/main/java/com/chla/kindd/di/RepositoryModule.kt
- Create: chla-android/app/src/main/java/com/chla/kindd/ui/onboarding/OnboardingUiState.kt
- Create: chla-android/app/src/main/java/com/chla/kindd/ui/onboarding/OnboardingViewModel.kt
- Create: chla-android/app/src/main/java/com/chla/kindd/ui/onboarding/OnboardingRoute.kt
- Create: chla-android/app/src/main/java/com/chla/kindd/ui/onboarding/AudienceStep.kt
- Create: chla-android/app/src/main/java/com/chla/kindd/ui/onboarding/ZipStep.kt
- Create: chla-android/app/src/main/java/com/chla/kindd/ui/onboarding/RegionalCenterStep.kt
- Create: chla-android/app/src/main/java/com/chla/kindd/ui/onboarding/JourneyStep.kt
- Create: chla-android/app/src/main/java/com/chla/kindd/ui/onboarding/AgeGroupStep.kt
- Modify: chla-android/app/src/main/res/values/strings.xml
- Modify: chla-android/app/src/main/res/values-es/strings.xml
- Create: chla-android/app/src/test/java/com/chla/kindd/data/source/FakeRegionalCenterDataSource.kt
- Create: chla-android/app/src/test/java/com/chla/kindd/data/source/FakeUserLocationSource.kt
- Create: chla-android/app/src/test/java/com/chla/kindd/ui/onboarding/OnboardingViewModelTest.kt
- Create: chla-android/app/src/androidTest/java/com/chla/kindd/ui/onboarding/OnboardingContentTest.kt

- [ ] **Step 1: Define and test the draft state machine**

OnboardingUiState contains:

- mode: FIRST_RUN or EDIT;
- step: AUDIENCE, ZIP, REGIONAL_CENTER, JOURNEY, AGE;
- a complete local draft;
- center lookup state: IDLE, LOADING, MATCHED, UNMATCHED, UNAVAILABLE;
- location state: IDLE, LOCATING, DENIED, FAILED;
- isSaving;
- canContinue derived per step.

OnboardingViewModelTest must prove:

1. first-run draft defaults audience to FAMILY and every other optional value to null;
2. edit initializes once from the full saved profile;
3. ZIP input retains only ASCII digits and clamps to five;
4. changing ZIP clears only the draft center, never persisted state;
5. a five-digit ZIP lookup advances to a matched center;
6. HTTP no-match advances to UNMATCHED and permits Continue;
7. unavailable/offline offers Retry and permits first-run Continue without a center;
8. device location success supplies coordinates, reverse-geocodes a five-digit ZIP, and performs the same lookup;
9. denial/failure returns control to ZIP entry and does not relaunch permission itself;
10. journey is required and cannot be toggled off;
11. age is optional and tapping the selected age clears it;
12. Get Started writes one full profile with onboardingCompleted=true;
13. edit Cancel writes nothing;
14. edit Save replaces all fields once, including clearing a stale center after a changed unmatched ZIP;
15. a Saved one-shot event is emitted only after replaceProfile succeeds, while a failed save stays on the current screen with a sanitized retry state.

- [ ] **Step 2: Extract a testable location source**

Define:

~~~kotlin
data class UserCoordinates(val latitude: Double, val longitude: Double)

interface UserLocationSource {
    fun hasLocationPermission(): Boolean
    suspend fun currentCoordinates(): UserCoordinates?
    suspend fun zipCodeFor(coordinates: UserCoordinates): String?
}
~~~

Make LocationService implement it while retaining compatibility methods used by current Map code. Hilt must bind the existing singleton LocationService to UserLocationSource, not instantiate another location client.

- [ ] **Step 3: Implement the ViewModel with draft-only writes**

initialize(mode, initialProfile) snapshots the supplied profile exactly once. FIRST_RUN receives the incomplete draft from AppEntryState; EDIT receives the current complete Ready profile. RegionalCenterLookup.Matched stores RegionalCenterIdentity.from(center); Unmatched and allowed Unavailable store null only in the draft.

Only finish() calls replaceProfile. cancel() emits a close event without repository work. Emit Saved only after replaceProfile succeeds; expose a localized save-retry state on failure. Do not expose Throwable or response text to UI.

- [ ] **Step 4: Build five focused Compose steps**

OnboardingRoute owns the activity-result launcher for ACCESS_COARSE_LOCATION. It asks only after a user taps Use my location, calls ViewModel after the result, and never loops on denial. It calls an idempotent viewModel.initialize(mode, initialProfile) from LaunchedEffect; mode is not a Hilt constructor parameter. FIRST_RUN receives AppEntryState.NeedsOnboarding.draft, while EDIT receives the current Ready profile passed into the main graph. The ViewModel snapshots that argument once so later recomposition cannot overwrite an in-progress draft.

Use vertical scrolling, 48dp controls, explicit selected semantics, Back/Continue/Get Started buttons, step progress, and a stable heading. Do not use the existing skippable four-page pager.

Exact English headings/body/actions:

| Key | English |
| --- | --- |
| onboarding_welcome_title | You found the right place. |
| onboarding_welcome_body | KiNDD helps LA County families navigate developmental services - regional centers, therapy providers, and what to do next. |
| onboarding_audience_prompt | I'm here as a |
| onboarding_audience_family | Parent or family |
| onboarding_audience_clinician | Clinician |
| onboarding_zip_title | Where is home? |
| onboarding_zip_body | Every LA County family is assigned a regional center by ZIP code. Yours decides who to call. |
| onboarding_zip_label | ZIP code |
| onboarding_use_location | Use my location instead |
| onboarding_center_title | Your Regional Center |
| onboarding_center_matched | Matched |
| onboarding_center_body | They coordinate evaluations, services, and funding for your family - and they're expecting calls like yours. |
| onboarding_center_unmatched_title | We'll figure it out together |
| onboarding_center_unmatched_body | We couldn't match that ZIP to a regional center. You can still browse everything, and KiNDD can help you find who serves your family. |
| onboarding_journey_title | Where are you in the journey? |
| onboarding_journey_body | KiNDD uses this to suggest your next step - nothing is locked in. |
| onboarding_age_title | How old is your child? |
| onboarding_age_body | Optional - it helps us show age-appropriate services first. |
| action_back | Back |
| action_continue | Continue |
| action_get_started | Get Started |
| action_retry | Retry |
| action_cancel | Cancel |
| action_save | Save |

Add natural Spanish counterparts in values-es in the same change. In particular, translate LA County as condado de Los Ángeles, ZIP code as código postal, regional center as centro regional, and clinician as profesional clínico. Never leave an English fallback for a newly added key.

Age display labels are localized separately from API values:

- 0-5 years (Early Intervention)
- 6-12 years (School Age)
- 13-18 years (Adolescent)
- 19+ years (Adult)
- All Ages

- [ ] **Step 5: Add stateless Compose tests**

OnboardingContentTest must assert:

- each state shows the correct heading and exactly one primary action;
- Continue is disabled for a short ZIP or missing journey;
- unmatched and unavailable center states remain navigable as specified;
- edit mode shows Cancel and prefilled selections;
- location-denied copy is visible and Use my location remains a user-controlled action;
- all clickable controls expose roles/labels and have at least 48dp bounds.

- [ ] **Step 6: Keep the legacy class only until navigation switches**

Do not edit the old ui/screens/OnboardingScreen.kt in this task because KINDDNavHost still references it. The new ui/onboarding flow must compile in isolation. Task 9 removes the legacy destination and file in the same buildable commit. Do not copy Welcome to KINDD, Ask KINDD, or a Skip action into the new flow.

- [ ] **Step 7: Verify and commit**

~~~bash
cd chla-android
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest \
  --tests 'com.chla.kindd.ui.onboarding.OnboardingViewModelTest' \
  --no-daemon
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew \
  :app:compileDebugKotlin \
  :app:compileDebugAndroidTestKotlin \
  --no-daemon
cd ..
git add \
  chla-android/app/src/main/java/com/chla/kindd/data/source/UserLocationSource.kt \
  chla-android/app/src/main/java/com/chla/kindd/services/LocationService.kt \
  chla-android/app/src/main/java/com/chla/kindd/di/RepositoryModule.kt \
  chla-android/app/src/main/java/com/chla/kindd/ui/onboarding \
  chla-android/app/src/main/res/values/strings.xml \
  chla-android/app/src/main/res/values-es/strings.xml \
  chla-android/app/src/test/java/com/chla/kindd/data/source \
  chla-android/app/src/test/java/com/chla/kindd/ui/onboarding \
  chla-android/app/src/androidTest/java/com/chla/kindd/ui/onboarding
git diff --cached --check
git commit -m "feat(android): add profile-aware onboarding"
~~~

---

### Task 9: Switch Whole Navigation Graphs After Hydration

**Files:**

- Modify: chla-android/app/src/main/java/com/chla/kindd/MainActivity.kt
- Create: chla-android/app/src/main/java/com/chla/kindd/ui/navigation/KINDDRoot.kt
- Create: chla-android/app/src/main/java/com/chla/kindd/ui/navigation/MainDestinationContent.kt
- Modify: chla-android/app/src/main/java/com/chla/kindd/ui/navigation/KINDDNavHost.kt
- Delete: chla-android/app/src/main/java/com/chla/kindd/ui/screens/OnboardingScreen.kt
- Create: chla-android/app/src/androidTest/java/com/chla/kindd/ui/navigation/AppEntryNavigationTest.kt

- [ ] **Step 1: Write a failing root graph test**

Test KINDDRootContent using explicit AppEntryState and fake slots, and test the real KINDDMainNavHost with TestNavHostController plus fake MainDestinationContent:

- Loading never composes Home;
- NeedsOnboarding composes onboarding and no bottom bar;
- Ready composes the main graph;
- changing Ready to NeedsOnboarding disposes the main graph;
- changing NeedsOnboarding to Ready starts the main graph at Home.
- the real main NavHost begins at Home, bottom-nav List/Map clicks change the real destination, and back-stack restoration does not construct an onboarding destination.

- [ ] **Step 2: Split the root from the main graph**

KINDDRoot obtains AppEntryViewModel and uses AppEntryContent:

~~~kotlin
when (state) {
    AppEntryState.Loading -> LaunchScreen()
    is AppEntryState.NeedsOnboarding -> OnboardingRoute(
        mode = FIRST_RUN,
        initialProfile = state.draft
    )
    is AppEntryState.Ready -> KINDDMainNavHost(profile = state.profile)
}
~~~

Rename the existing function in KINDDNavHost.kt to KINDDMainNavHost and keep Home as its fixed start destination. It accepts the current Ready profile, an optional NavHostController, and a MainDestinationContent implementation. MainDestinationContent declares @Composable functions for Home, Map, List, Chat, Settings, provider detail, Regions, FAQ, About, and edit-profile, with a MainNavActions value carrying typed navigation callbacks. ProductionMainDestinationContent calls the real screens; tests supply tagged no-Hilt destinations while exercising the real Scaffold, bottom bar, NavHost, routes, and back stack. Remove the old onboarding destination and delete ui/screens/OnboardingScreen.kt in this task. Do not mutate a NavHost startDestination and do not manually navigate after first-run save; the persisted profile emission changes the root branch.

MainActivity renders only KINDDRoot inside KINDDTheme.

- [ ] **Step 3: Verify first-run graph compilation**

~~~bash
cd chla-android
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew \
  :app:testDebugUnitTest \
  :app:compileDebugAndroidTestKotlin \
  :app:assembleDebug \
  --no-daemon
~~~

Expected: unit suite passes, UI tests compile, and an APK assembles with the startup gate connected.

- [ ] **Step 4: Commit**

~~~bash
cd ..
git add \
  chla-android/app/src/main/java/com/chla/kindd/MainActivity.kt \
  chla-android/app/src/main/java/com/chla/kindd/ui/navigation/KINDDRoot.kt \
  chla-android/app/src/main/java/com/chla/kindd/ui/navigation/MainDestinationContent.kt \
  chla-android/app/src/main/java/com/chla/kindd/ui/navigation/KINDDNavHost.kt \
  chla-android/app/src/main/java/com/chla/kindd/ui/screens/OnboardingScreen.kt \
  chla-android/app/src/androidTest/java/com/chla/kindd/ui/navigation/AppEntryNavigationTest.kt
git diff --cached --check
git commit -m "feat(android): route first launch through onboarding"
~~~

---

### Task 10: Connect the Persisted Profile and Canonical Shortcuts to Home and Regions

**Files:**

- Create: chla-android/app/src/main/java/com/chla/kindd/ui/home/HomeUiState.kt
- Create: chla-android/app/src/main/java/com/chla/kindd/ui/chat/ChatLaunchPrompt.kt
- Create: chla-android/app/src/main/java/com/chla/kindd/ui/home/HomeViewModel.kt
- Modify: chla-android/app/src/main/java/com/chla/kindd/ui/screens/HomeScreen.kt
- Modify: chla-android/app/src/main/java/com/chla/kindd/ui/screens/ChatScreen.kt
- Modify: chla-android/app/src/main/java/com/chla/kindd/ui/screens/ChatViewModel.kt
- Modify: chla-android/app/src/main/java/com/chla/kindd/data/repository/RegionalCenterRepository.kt
- Modify: chla-android/app/src/main/java/com/chla/kindd/ui/screens/RegionalCentersViewModel.kt
- Modify: chla-android/app/src/main/java/com/chla/kindd/ui/screens/RegionalCentersScreen.kt
- Modify: chla-android/app/src/main/java/com/chla/kindd/ui/navigation/KINDDNavHost.kt
- Modify: chla-android/app/src/main/java/com/chla/kindd/ui/navigation/MainDestinationContent.kt
- Modify: chla-android/app/src/main/res/values/strings.xml
- Modify: chla-android/app/src/main/res/values-es/strings.xml
- Create: chla-android/app/src/test/java/com/chla/kindd/ui/home/HomeViewModelTest.kt
- Create: chla-android/app/src/test/java/com/chla/kindd/data/discovery/FakeDiscoveryController.kt
- Create: chla-android/app/src/test/java/com/chla/kindd/ui/chat/ChatLaunchPromptTest.kt
- Create: chla-android/app/src/test/java/com/chla/kindd/ui/screens/ChatViewModelInitialPromptTest.kt
- Create: chla-android/app/src/test/java/com/chla/kindd/ui/screens/RegionalCentersViewModelTest.kt
- Create: chla-android/app/src/androidTest/java/com/chla/kindd/ui/home/HomeContentTest.kt

- [ ] **Step 1: Write failing Home behavior tests**

HomeViewModelTest uses fakes and proves:

1. profile flow renders the saved center, ZIP, audience, journey, and age;
2. five ASCII digits are required for lookup;
3. Matched lookup atomically replaces only ZIP/center while retaining audience, journey, age, and completion;
4. Unmatched and Unavailable leave the repository's current profile byte-for-byte unchanged;
5. successful matching stays on Home and makes zero direct discovery refresh calls; DiscoveryStoreTest owns the exactly-once profile-observation assertion;
6. Find button and keyboard search call the same submitZip method;
7. selecting ABA, Speech, Occupational, or Physical synchronously replaces the therapy set with the exact canonical value before NavigateToList is emitted;
8. Details emits NavigateToRegionalCenters;
9. Map and List actions emit their distinct destinations;
10. each journey-chat action emits NavigateToChat with the exact typed prompt key before navigation;
11. Call now emits only the matched center's normalized dial digits and is unavailable without a center/phone;
12. after process recreation, a saved center identity is hydrated from RegionalCenterDataSource.getRegionalCenters by deployed ID, falling back to canonical name/short name, so phone/details return when the list succeeds;
13. hydration failure leaves the identity card usable but omits Call now;
14. changing the persisted identity immediately clears old hydrated details, cancels/supersedes their lookup, and cannot emit an old-center dial action even if that response arrives late;
15. no UI error contains a Throwable message or ZIP.

Use a Channel or SharedFlow for one-shot navigation/dial effects. Do not encode those effects in persistent UiState.

ChatLaunchPrompt contains fixed route keys and @StringRes prompt IDs. The English resources contain these exact request texts, and values-es contains natural Spanish counterparts:

~~~text
JUST_DIAGNOSED
We just got a diagnosis. What do I say when I call my regional center to request an intake evaluation for my child?

WAITING_INTAKE
How do we prepare for our regional center intake appointment? What documents and information should we bring?

RECEIVING_SERVICES
My child already receives regional center services. How do I prepare for an IPP meeting, and what services can I ask for?
~~~

Pass only the fixed key through navigation. Define Chat's base route as chat, destination pattern as chat?prompt={prompt}, and a createRoute(ChatLaunchPrompt?) helper. The bottom bar always navigates to the base route; selected-tab matching compares the current destination pattern to Chat.destinationRoute. Declare prompt as a nullable String navArgument with a null default. MainDestinationContent.Chat receives the decoded ChatLaunchPrompt?. Map it through ChatLaunchPrompt.fromRouteValue, resolve its localized prompt resource in ChatScreen, and call ChatViewModel.sendInitialPrompt(key, resolvedText). That method tracks handled fixed keys in the route-scoped ViewModel and delegates to sendMessage at most once. A normal bottom-nav chat route has no argument. Do not put the full prompt into a route, log, or SavedState key. Move the touched Chat welcome literals to English/Spanish resources and correct Ask KINDD to Ask KiNDD, but do not expand this narrow launch hook into the deferred chat streaming/Markdown work.

- [ ] **Step 2: Implement a profile-backed Home ViewModel**

HomeUiState contains profile, optional hydrated RegionalCenter details, zipDraft, lookupState, and optional sanitized message category. HomeViewModel injects UserProfileRepository, RegionalCenterDataSource, and DiscoveryController. Key the hydration job and details to the full persisted RegionalCenterIdentity. When identity changes, cancel the prior job and clear details synchronously before loading. Load the Los Angeles center list once per identity and match deployed ID first, then canonical name/short name; use an identity generation guard so a late old response cannot repopulate details. Do not persist phone/address fields or block the identity card if hydration fails.

On a matched lookup, replace the full profile once. Do not also call discovery.refresh directly: DiscoveryStore's profile observer owns the one resulting refresh. On a therapy shortcut, call setSingleTherapyAndRefresh first, then emit NavigateToList.

For the narrow journey launch hook, inject @IoDispatcher into ChatViewModel, add sendInitialPrompt(key: String, resolvedText: String) with an in-memory handled-key set, and rethrow CancellationException. Replace its raw exception-message state with a sanitized chat failure category while leaving streaming, Markdown, history, Stop/Retry, and broader chat redesign deferred.

- [ ] **Step 3: Replace local Home ZIP and label-only chips**

Refactor HomeScreen into a route that collects HomeViewModel with collectAsStateWithLifecycle and a stateless HomeContent.

Required touched behavior:

- no matched center: show Who serves your family?, the explanatory ZIP card, Find, and inline no-match/unavailable states;
- matched center: show Your Regional Center, Matched, center name/short name, phone action when present, and Details;
- show separate Map and List discovery actions;
- therapy chips display ABA Therapy, Speech, Occupational, Physical while passing canonical values;
- the Regional Centers Explore action still opens Regions;
- Ask KiNDD remains a Chat action;
- use the saved journey to show:
  - justDiagnosed: Request an intake evaluation; What do I say?; Call now when a center is known;
  - waitingIntake: Get ready for the intake; Help me prepare;
  - receivingServices: Get more from your IPP; What can I ask for?;
  - exploring: no next-step card.

The three journey chat labels launch Chat with their corresponding typed prompt. Call now uses ACTION_DIAL with digits normalized from the matched center phone and is absent when no usable phone is known.

All new copy must be in both string files. No raw strings remain in the touched Home composables.

- [ ] **Step 4: Make Regions use and safely update the same profile**

RegionalCentersViewModel injects UserProfileRepository and RegionalCenterDataSource. Prefill its ZIP draft from the saved profile. On a successful manual match, replace ZIP/center while preserving the rest of the profile. Unmatched or unavailable lookup changes only screen state. After this final caller migrates to lookupRegionalCenter, remove RegionalCenterRepository.getRegionalCenterByZip compatibility wrapper and keep its contract coverage on the typed method.

RegionalCentersScreen must:

- filter ZIP input to five ASCII digits;
- route button and keyboard action to one submit method;
- distinguish no match from network unavailable;
- keep the seven-center list and details behavior;
- remove hard-coded Enter ZIP and move all touched strings to English/Spanish resources.

- [ ] **Step 5: Add Home UI tests**

HomeContentTest verifies matched/unmatched layouts, exact therapy labels, canonical callbacks, journey-card selection, exact journey prompt-key callbacks, Call now presence/digits, keyboard/button equivalence, 48dp click targets, and no CHLA/KINDD user-facing text on the touched Home surface. A focused Chat launch test proves each fixed key is sent once and a normal bottom-nav Chat launch sends no automatic prompt.

- [ ] **Step 6: Run and commit**

~~~bash
cd chla-android
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest \
  --tests 'com.chla.kindd.ui.home.HomeViewModelTest' \
  --tests 'com.chla.kindd.ui.chat.ChatLaunchPromptTest' \
  --tests 'com.chla.kindd.ui.screens.ChatViewModelInitialPromptTest' \
  --tests 'com.chla.kindd.ui.screens.RegionalCentersViewModelTest' \
  --no-daemon
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew \
  :app:compileDebugAndroidTestKotlin \
  :app:assembleDebug \
  --no-daemon
cd ..
git add \
  chla-android/app/src/main/java/com/chla/kindd/ui/home \
  chla-android/app/src/main/java/com/chla/kindd/ui/chat/ChatLaunchPrompt.kt \
  chla-android/app/src/main/java/com/chla/kindd/ui/screens/HomeScreen.kt \
  chla-android/app/src/main/java/com/chla/kindd/ui/screens/ChatScreen.kt \
  chla-android/app/src/main/java/com/chla/kindd/ui/screens/ChatViewModel.kt \
  chla-android/app/src/main/java/com/chla/kindd/data/repository/RegionalCenterRepository.kt \
  chla-android/app/src/main/java/com/chla/kindd/ui/screens/RegionalCentersViewModel.kt \
  chla-android/app/src/main/java/com/chla/kindd/ui/screens/RegionalCentersScreen.kt \
  chla-android/app/src/main/java/com/chla/kindd/ui/navigation/KINDDNavHost.kt \
  chla-android/app/src/main/java/com/chla/kindd/ui/navigation/MainDestinationContent.kt \
  chla-android/app/src/main/res/values/strings.xml \
  chla-android/app/src/main/res/values-es/strings.xml \
  chla-android/app/src/test/java/com/chla/kindd/ui/home \
  chla-android/app/src/test/java/com/chla/kindd/data/discovery/FakeDiscoveryController.kt \
  chla-android/app/src/test/java/com/chla/kindd/ui/chat \
  chla-android/app/src/test/java/com/chla/kindd/ui/screens/ChatViewModelInitialPromptTest.kt \
  chla-android/app/src/test/java/com/chla/kindd/ui/screens/RegionalCentersViewModelTest.kt \
  chla-android/app/src/androidTest/java/com/chla/kindd/ui/home
git diff --cached --check
git commit -m "feat(android): connect Home to profile discovery"
~~~

---

### Task 11: Make Map and List Thin Views over One Discovery Session

**Files:**

- Create: chla-android/app/src/main/java/com/chla/kindd/ui/discovery/DiscoverySearchField.kt
- Create: chla-android/app/src/main/java/com/chla/kindd/ui/discovery/ActiveFilterChips.kt
- Create: chla-android/app/src/main/java/com/chla/kindd/ui/discovery/DiscoveryFilterSheet.kt
- Create: chla-android/app/src/main/java/com/chla/kindd/ui/discovery/DiscoveryStateContent.kt
- Modify: chla-android/app/src/main/java/com/chla/kindd/ui/screens/MapViewModel.kt
- Modify: chla-android/app/src/main/java/com/chla/kindd/ui/screens/MapScreen.kt
- Modify: chla-android/app/src/main/java/com/chla/kindd/ui/screens/ProviderListViewModel.kt
- Modify: chla-android/app/src/main/java/com/chla/kindd/ui/screens/ProviderListScreen.kt
- Modify: chla-android/app/src/main/java/com/chla/kindd/ui/navigation/KINDDNavHost.kt
- Modify: chla-android/app/src/main/res/values/strings.xml
- Modify: chla-android/app/src/main/res/values-es/strings.xml
- Create: chla-android/app/src/test/java/com/chla/kindd/ui/screens/MapViewModelTest.kt
- Create: chla-android/app/src/test/java/com/chla/kindd/ui/screens/ProviderListViewModelTest.kt
- Create: chla-android/app/src/androidTest/java/com/chla/kindd/ui/discovery/DiscoveryControlsTest.kt
- Create: chla-android/app/src/androidTest/java/com/chla/kindd/ui/discovery/MapListParityTest.kt

- [ ] **Step 1: Write failing presenter tests**

Use one FakeDiscoveryController instance in both tests. Prove:

- both presenters expose the same query, criteria, loading, error, and provider IDs;
- Map exposes only state.mapProviders;
- List exposes every state.providers item;
- neither presenter calls an independent repository load in init;
- first appearance calls ensureLoaded, which remains idempotent in the store;
- typing in either presenter calls the same setQuery;
- filter apply/removal/clear delegates to the controller;
- retry delegates to retry;
- Map location success changes origin to DeviceLocation;
- location denial/failure retains current results and returns a localized category.

- [ ] **Step 2: Replace independent ViewModel state**

ProviderListViewModel injects DiscoveryController and exposes its StateFlow plus presentation-only sort state. Name and distance sorting return derived lists without modifying DiscoveryState providers.

MapViewModel injects DiscoveryController and UserLocationSource. It exposes DiscoveryState plus location permission/status. The composable owns the permission launcher; ViewModel owns the post-permission location lookup and controller update.

Delete the old separate provider lists, search jobs, raw String errors, and direct ProviderRepository calls from both ViewModels.

- [ ] **Step 3: Build reusable discovery controls**

DiscoverySearchField binds directly to DiscoveryCriteria.query and exposes clear/filter actions.

ActiveFilterChips renders removable age, diagnosis, insurance, radius only while DeviceLocation is active, and each selected therapy. Clear All invokes one controller action. A radius choice remains in criteria when another origin is active but is not presented as an effective filter until device-location search is selected.

DiscoveryFilterSheet uses a local draft until Apply. The radius section is rendered only for DiscoveryOrigin.DeviceLocation; it is absent for ProfileZip and LosAngelesCatalog so the UI never advertises a no-op radius filter. It contains:

- radius values 5, 10, 15, 25, 50 miles;
- the five typed age values plus Any Age;
- the five exact diagnosis values from DiscoveryCatalog plus Any Diagnosis;
- the 15 exact insurance values plus Any Insurance;
- all six therapy toggles;
- Reset and Apply.

Localized display labels never feed back as API values.

DiscoveryStateContent centralizes:

- initial loading;
- initial full error plus Retry;
- refresh progress over existing results;
- refresh-error banner plus Retry while preserving results;
- explicit no-results state retaining controls;
- result count.

- [ ] **Step 4: Refactor List**

ProviderListScreen collects state with lifecycle, uses the shared search/filter components, labels the surface List, and passes provider IDs unchanged to detail navigation. Keep cards focused and accessible. Do not mutate shared results when changing List sort.

Update the bottom navigation and header string from Resources to List on touched surfaces. Keep API/model terminology unchanged.

- [ ] **Step 5: Refactor Map**

MapScreen uses the same shared controls and state shell above GoogleMap. Render markers only for mapProviders, with the exact provider ID in the click callback. Add an explicit Use my location search action so location changes discovery origin rather than only moving the map camera.

Remove android.util.Log, TAG, provider names, coordinates, and marker debug logging. Keep GoogleMap's my-location layer disabled until permission is confirmed. Do not synthesize LatLng(0, 0).

- [ ] **Step 6: Add cross-surface Compose tests**

DiscoveryControlsTest proves query binding, Apply-versus-dismiss draft behavior, all canonical filter options, removable chips, Clear All, empty/error/retry, and preserved results during refresh.

MapListParityTest creates one FakeDiscoveryController, supplies test Map/List destinations through MainDestinationContent, and drives the real KINDDMainNavHost with TestNavHostController. It proves:

- both show the same query and active chips;
- List exposes all three provider IDs;
- Map marker models expose only the two coordinate-bearing IDs;
- clicking the real bottom-nav List and Map items changes the actual route without resetting criteria or provider identity;
- the active Home therapy is visible on List.

Keep GoogleMap itself out of semantic assertions by extracting a pure marker-model mapper and testing the surrounding content.

- [ ] **Step 7: Run and commit**

~~~bash
cd chla-android
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest \
  --tests 'com.chla.kindd.ui.screens.MapViewModelTest' \
  --tests 'com.chla.kindd.ui.screens.ProviderListViewModelTest' \
  --tests 'com.chla.kindd.data.discovery.DiscoveryStoreTest' \
  --no-daemon
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew \
  :app:compileDebugAndroidTestKotlin \
  :app:lintDebug \
  :app:assembleDebug \
  --no-daemon
cd ..
git add \
  chla-android/app/src/main/java/com/chla/kindd/ui/discovery \
  chla-android/app/src/main/java/com/chla/kindd/ui/screens/MapViewModel.kt \
  chla-android/app/src/main/java/com/chla/kindd/ui/screens/MapScreen.kt \
  chla-android/app/src/main/java/com/chla/kindd/ui/screens/ProviderListViewModel.kt \
  chla-android/app/src/main/java/com/chla/kindd/ui/screens/ProviderListScreen.kt \
  chla-android/app/src/main/java/com/chla/kindd/ui/navigation/KINDDNavHost.kt \
  chla-android/app/src/main/res/values/strings.xml \
  chla-android/app/src/main/res/values-es/strings.xml \
  chla-android/app/src/test/java/com/chla/kindd/ui/screens/MapViewModelTest.kt \
  chla-android/app/src/test/java/com/chla/kindd/ui/screens/ProviderListViewModelTest.kt \
  chla-android/app/src/androidTest/java/com/chla/kindd/ui/discovery
git diff --cached --check
git commit -m "feat(android): unify Map and List discovery"
~~~

---

### Task 12: Add Profile Controls and Polish Every Touched Surface

**Files:**

- Create: chla-android/app/src/main/java/com/chla/kindd/ui/settings/SettingsViewModel.kt
- Modify: chla-android/app/src/main/java/com/chla/kindd/ui/screens/SettingsScreen.kt
- Modify: chla-android/app/src/main/java/com/chla/kindd/ui/navigation/KINDDNavHost.kt
- Modify: chla-android/app/src/main/java/com/chla/kindd/ui/theme/Color.kt
- Modify: chla-android/app/src/main/java/com/chla/kindd/ui/theme/Theme.kt
- Modify: chla-android/app/src/main/java/com/chla/kindd/ui/theme/Type.kt
- Modify: chla-android/app/src/main/res/values/colors.xml
- Modify: chla-android/app/src/main/res/values/strings.xml
- Modify: chla-android/app/src/main/res/values-es/strings.xml
- Create: chla-android/app/src/test/java/com/chla/kindd/ui/settings/SettingsViewModelTest.kt
- Create: chla-android/app/src/androidTest/java/com/chla/kindd/ui/settings/SettingsContentTest.kt
- Create: chla-android/app/src/androidTest/java/com/chla/kindd/ui/accessibility/TouchedSurfaceAccessibilityTest.kt

- [ ] **Step 1: Write failing Settings tests**

SettingsViewModelTest proves:

- Edit emits a one-shot edit-profile navigation event and writes nothing;
- Clear does nothing before confirmation;
- confirmed Clear calls UserProfileRepository.clearProfile exactly once;
- no manual navigation event is emitted after clear because AppEntryState owns the graph switch.

SettingsContentTest proves Edit Profile & Onboarding exists, Clear Profile & Restart opens a confirmation dialog, Cancel closes without clearing, and confirmation invokes the clear callback. The incomplete-profile reset cases in DiscoveryStoreTest and AppEntryViewModelTest remain the companion integration contract for clearing transient state and replacing the main graph.

- [ ] **Step 2: Wire profile edit and reset**

Add a main-graph route edit-profile that renders OnboardingRoute(mode = EDIT, initialProfile = profile), using the current Ready profile already passed into KINDDMainNavHost. Pass Cancel and successful Save back to navController.popBackStack(). It does not touch first-run navigation.

SettingsScreen collects SettingsViewModel, adds:

- Edit Profile & Onboarding;
- Clear Profile & Restart;
- a destructive confirmation dialog that states onboarding will run again.

On confirmed clear, call clearProfile only. The root observes the incomplete profile, tears down the main graph, and composes first-run onboarding. DiscoveryStore observes the same incomplete profile and performs the tested privacy reset from Task 6, so neither in-flight nor cached old-profile discovery can survive reset.

- [ ] **Step 3: Apply the approved Material visual tokens**

Add semantic KiNDD tokens:

~~~text
Indigo        #6366F1
Deep Indigo   #4F46E5
Violet        #8B5CF6
Purple        #A855F7
Pink          #EC4899
Matched Green #10B981
~~~

Use adaptive light/dark canvas, surface, on-surface, outline, error, and matched colors through MaterialTheme. Touched cards use 20dp shapes, visible pressed/ripple state, and no fixed light-only text color. Keep the native bottom NavigationBar and system navigation; do not introduce a floating glass clone in this phase.

Rename only newly introduced theme identifiers to KiNDD-oriented semantic names. Do not perform a risky app-wide symbol rename of existing infrastructure/package identifiers in this slice.

- [ ] **Step 4: Complete localization and accessibility**

Add exact English and natural Spanish values for every touched Home, onboarding, Map, List, filter, Settings, loading, empty, error, location, and profile-control string. Remove raw user-facing literals from touched composables.

TouchedSurfaceAccessibilityTest composes key content with:

- light and dark MaterialTheme;
- fontScale 1.0 and 1.5;
- narrow phone width;
- Locale.US and Locale("es") by supplying locale-specific configuration contexts to LocalContext;
- TalkBack semantic queries.

Assert no primary action is clipped, every icon-only control has a localized content description, selection state is announced, heading semantics are present, traversal remains logical, and every interactive target is at least 48dp.

Avoid decorative icon descriptions. Use live-region semantics for lookup/loading/error changes where appropriate. Do not add nonessential motion; honor the system animator/reduced-motion setting through standard Material behavior.

- [ ] **Step 5: Run lint, unit, and Android-test compilation**

~~~bash
cd chla-android
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:testDebugUnitTest \
  --tests 'com.chla.kindd.ui.settings.SettingsViewModelTest' \
  --no-daemon
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew \
  :app:compileDebugAndroidTestKotlin \
  :app:lintDebug \
  :app:assembleDebug \
  --no-daemon
~~~

Expected: no missing-translation or accessibility lint errors and a successful APK.

- [ ] **Step 6: Commit**

~~~bash
cd ..
git add \
  chla-android/app/src/main/java/com/chla/kindd/ui/settings \
  chla-android/app/src/main/java/com/chla/kindd/ui/screens/SettingsScreen.kt \
  chla-android/app/src/main/java/com/chla/kindd/ui/navigation/KINDDNavHost.kt \
  chla-android/app/src/main/java/com/chla/kindd/ui/theme/Color.kt \
  chla-android/app/src/main/java/com/chla/kindd/ui/theme/Theme.kt \
  chla-android/app/src/main/java/com/chla/kindd/ui/theme/Type.kt \
  chla-android/app/src/main/res/values/colors.xml \
  chla-android/app/src/main/res/values/strings.xml \
  chla-android/app/src/main/res/values-es/strings.xml \
  chla-android/app/src/test/java/com/chla/kindd/ui/settings \
  chla-android/app/src/androidTest/java/com/chla/kindd/ui/settings \
  chla-android/app/src/androidTest/java/com/chla/kindd/ui/accessibility
git diff --cached --check
git commit -m "feat(android): add profile controls and parity polish"
~~~

---

### Task 13: Verify the Complete Android Slice on the Pixel Emulator

**Files:**

- Modify only if verification reveals a scoped defect: files introduced or intentionally touched in Tasks 1-12
- Do not modify: backend, web, iOS, deployment, marketing, or unrelated dirty files

- [ ] **Step 1: Audit the final Android diff**

From the repository root:

~~~bash
git status --short
git log --oneline --decorate -15
KINDD_FOUNDATION_BASE=84c7f96
git diff "$KINDD_FOUNDATION_BASE"..HEAD --check -- chla-android docs/superpowers
git diff "$KINDD_FOUNDATION_BASE"..HEAD --stat -- chla-android docs/superpowers
rg -n 'TODO|FIXME|TBD|placeholder' \
  chla-android/app/src/main/java/com/chla/kindd/data/profile \
  chla-android/app/src/main/java/com/chla/kindd/data/discovery \
  chla-android/app/src/main/java/com/chla/kindd/ui/app \
  chla-android/app/src/main/java/com/chla/kindd/ui/onboarding \
  chla-android/app/src/main/java/com/chla/kindd/ui/home \
  chla-android/app/src/main/java/com/chla/kindd/ui/discovery \
  chla-android/app/src/main/java/com/chla/kindd/ui/settings
~~~

Expected: no whitespace errors and no placeholders in new feature code. Confirm unrelated root changes are still present and were never staged.

- [ ] **Step 2: Run the complete deterministic gate**

~~~bash
cd chla-android
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew \
  :app:test \
  :app:lintDebug \
  :app:assembleDebug \
  --no-daemon
~~~

Expected: BUILD SUCCESSFUL, all unit/contract tests pass, lint has zero errors, and app/build/outputs/apk/debug/app-debug.apk exists.

- [ ] **Step 3: Run the full connected UI suite**

Ensure the Pixel_8 API 36 emulator is booted:

~~~bash
adb devices
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:connectedDebugAndroidTest --no-daemon
~~~

Expected: the profile backup, entry gate, onboarding, Home, Map/List parity, Settings, and accessibility instrumentation tests all pass.

- [ ] **Step 4: Perform a clean-install functional smoke**

~~~bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew :app:installDebug --no-daemon
adb shell pm clear com.chla.kindd
adb shell am start -n com.chla.kindd/.MainActivity
~~~

Manually prove on the emulator:

1. Loading does not flash Home.
2. First run shows the five-step profile flow.
3. Parent/family defaults selected; clinician can be selected.
4. ZIP 90001 matches SCLARC and shows the matched center.
5. Back preserves the draft; age can be selected and toggled off.
6. Completing onboarding opens Home with the matched card.
7. Home ABA Therapy opens List with ABA active and real deployed providers.
8. List and Map show the same query/filter chips; Map markers are the coordinate subset.
9. Rapid search typing leaves only the latest result state.
10. Location denial is recoverable and does not repeatedly request permission.
11. Edit Profile opens prefilled; Cancel leaves Home unchanged.
12. Clear Profile requires confirmation and returns to onboarding.

- [ ] **Step 5: Prove persistence across process death**

Complete onboarding again, then:

~~~bash
adb shell am force-stop com.chla.kindd
adb shell am start -n com.chla.kindd/.MainActivity
~~~

Expected: Home opens directly with the saved profile; transient query/filter state starts clean for the new process.

- [ ] **Step 6: Check dark theme, Spanish, large text, and privacy**

On the emulator, repeat onboarding and discovery at:

- system dark theme;
- Spanish locale;
- largest practical font scale;
- TalkBack enabled;
- denied location.

Before entering a test ZIP/query, clear logcat. After completing the flow, inspect only this app's process logs and confirm the ZIP, query, coordinates, filters, provider response bodies, and profile fields do not appear. Do not copy log content into the repository.

- [ ] **Step 7: Request an independent diff review**

Invoke superpowers:requesting-code-review with the approved design, this plan, the baseline commit, and the final Android commit range. Ask the reviewer to classify findings as Critical, Important, or Minor and specifically inspect:

- profile atomicity and corrupt-value fallback;
- DataStore backup/device-transfer exclusion;
- cancellation propagation and generation guards;
- exact Retrofit query names and ZIP-therapy local filtering;
- Home unmatched no-write behavior;
- Map/List provider identity;
- raw error/log data leakage;
- localization, dark theme, large font, and TalkBack;
- unrelated dirty-worktree contamination.

Resolve every Critical and Important finding with a failing regression test, minimal fix, focused rerun, and exact-path commit. Re-run Steps 1-3 after any fix.

- [ ] **Step 8: Record the final evidence**

The completion handoff must state:

- Android commit range;
- APK path and size;
- exact unit, lint, build, and connected-test results;
- emulator/device and API level;
- live API flows exercised;
- onboarding/profile/edit/reset outcomes;
- Map/List shared-state outcome;
- accessibility/localization modes checked;
- independent review result;
- any remaining Minor finding or explicitly deferred follow-on.

Do not claim the regional-polygon, provider-detail, chat-streaming/Markdown, floating-navigation, or full visual-parity phases are complete. They remain the separate follow-ons named in the design.

---

## Plan Self-Review Checklist

Before implementation begins, verify this document itself:

- [x] Every task names exact files, a failing test, an implementation contract, a verification command, and a scoped commit.
- [x] No implementation TODO, TBD, placeholder, alternate design branch, or unresolved product choice remains.
- [x] The request matrix matches the deployed backend: repeated therapy only for comprehensive search; ZIP therapy is local.
- [x] Stable profile/API values are separate from localized labels.
- [x] First-run unmatched/offline behavior and Home unmatched no-write behavior are distinct.
- [x] DataStore filename and both backup exclusions are exact.
- [x] Main graph selection waits for profile hydration.
- [x] Home criteria update precedes navigation.
- [x] Map/List use one singleton state and preserve provider identity.
- [x] CancellationException is never swallowed.
- [x] All touched copy is English/Spanish, KiNDD-cased, and free of new CHLA user-facing naming.
- [x] Full tests, lint, APK, connected tests, clean-install smoke, relaunch smoke, privacy check, and independent review are completion gates.
