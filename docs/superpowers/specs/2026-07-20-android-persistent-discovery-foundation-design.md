# Android Persistent Discovery Foundation Design

**Status:** Approved on 2026-07-20

## Purpose

Bring the Android app to a reliable behavioral baseline with the current iPhone app before attempting the regional-map, chat, and full visual-parity phases. This slice establishes one persisted family or clinician profile and one shared discovery session across Home, Map, and List.

The existing Android production-API repair remains the baseline. This design must preserve its flexible provider and regional-center decoding, corrected endpoint paths and query names, result-envelope handling, client-side search limits, and nearest-first distance ordering.

## Product Decisions

- Android mirrors the current iPhone onboarding profile: audience, ZIP code, matched regional center, journey stage, and optional age group.
- Every existing Android installation enters this onboarding once after receiving the new build because no compatible completion record exists today.
- Subsequent launches go directly to Home unless the user explicitly chooses to edit or clear their profile.
- Profile data persists across process death; text queries, temporary discovery filters, results, and errors persist only for the current application session.
- Android matches iPhone behavior while retaining native Material interaction patterns. A literal clone of the iPhone glass navigation is outside this slice.

## Goals

1. Hydrate persisted state before choosing onboarding or the main application, without flashing Home.
2. Complete or edit the full profile as one atomic operation.
3. Make Home ZIP and therapy actions change real discovery state rather than only navigating.
4. Give Map and List one shared query, filter set, result set, loading state, and error state.
5. Make search requests latest-wins and cancellation-safe.
6. Apply the current KiNDD visual intent to the screens touched by this work.
7. Cover persistence, orchestration, UI state, and deployed API contracts with deterministic tests.

## Non-Goals

- Regional-center GeoJSON polygons or polygon interaction
- Provider marker clustering and final marker artwork
- Full provider-detail parity
- Streaming chat, Markdown rendering, chat history, or provider actions from chat
- Floating glass navigation or a complete app-wide visual rewrite
- Persisting transient queries, discovery results, or temporary filters across process death
- Changing backend search semantics or modifying the iPhone app

Those items are separate follow-on designs so each phase can be tested and reviewed independently.

## Architecture

```text
Preferences DataStore
        |
        v
UserProfileRepository
        +----> AppEntryViewModel ----> Loading | Onboarding | Main
        +----> OnboardingViewModel
        +----> HomeViewModel
        +----> future typed chat context

Home / Map / List actions
        |
        v
DiscoveryStore
        |
        v
ProviderRepository ----> deployed provider API
```

### User profile model

The persisted model is typed and contains no screen-specific state:

```kotlin
enum class AudienceType { FAMILY, CLINICIAN }

enum class JourneyStage {
    JUST_DIAGNOSED,
    WAITING_FOR_INTAKE,
    RECEIVING_SERVICES,
    EXPLORING
}

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
    val ageGroup: String? = null
)
```

`UserProfileRepository` owns the single application-scoped Preferences DataStore instance and exposes:

```kotlin
interface UserProfileRepository {
    val profile: Flow<UserProfile>
    suspend fun replaceProfile(profile: UserProfile)
    suspend fun clearProfile()
}
```

`replaceProfile` writes all fields within one DataStore `edit` transaction. It replaces, rather than merges, regional-center identity so changing to an unmatched ZIP cannot retain a stale center. Unknown enum values and corrupt fields decode to safe defaults. A missing completion key means onboarding is incomplete.

### Application entry gate

`AppEntryViewModel` maps profile hydration to exactly three states:

```kotlin
sealed interface AppEntryState {
    data object Loading : AppEntryState
    data class NeedsOnboarding(val draft: UserProfile) : AppEntryState
    data class Ready(val profile: UserProfile) : AppEntryState
}
```

The root composable shows a neutral KiNDD launch surface while loading. It composes the onboarding graph only for `NeedsOnboarding` and the main navigation graph only for `Ready`. A mutable `startDestination` is not used.

Profile editing opens onboarding in edit mode with a local draft populated from the current profile. Save performs one replacement. Cancel returns to the prior screen without writing. Clear Profile replaces the saved value with a default incomplete profile and returns to onboarding.

### Discovery models

```kotlin
sealed interface DiscoveryOrigin {
    data class ProfileZip(val zipCode: String) : DiscoveryOrigin
    data class DeviceLocation(val latitude: Double, val longitude: Double) : DiscoveryOrigin
    data object LosAngelesCatalog : DiscoveryOrigin
}

data class DiscoveryCriteria(
    val query: String = "",
    val therapyTypes: Set<String> = emptySet(),
    val ageGroup: String? = null,
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
)
```

`DiscoveryStore` is an application-scoped Hilt singleton backed by an injected application coroutine scope and testable dispatchers. It exposes `StateFlow<DiscoveryState>` plus explicit actions for profile changes, query changes, filters, origin changes, refresh, and clear.

Each request receives a monotonically increasing generation ID. A new request cancels the preceding job, and a result may update state only when its generation is still current. Text queries debounce for 300 milliseconds; explicit Find, filter, refresh, location, and Home-shortcut actions run immediately.

### Request selection

The store chooses one request path per criteria snapshot:

| Origin and criteria | Request behavior |
| --- | --- |
| Profile ZIP | Call providers-by-regional-center with ZIP, therapies, age, diagnosis, and insurance; apply a case-insensitive text query across provider name, address, city, description, therapy types, and insurance locally so regional-center coverage remains authoritative. |
| Device location | Call comprehensive search with query, coordinates, radius, therapies, age, diagnosis, and insurance; compute distance, sort nearest-first, and enforce the result cap client-side. |
| LA fallback with query or filters | Call comprehensive search without coordinates using the active query and filters. |
| LA fallback without query or filters | Load the catalog endpoint. |

Therapy values are stable backend values such as `ABA therapy`; localized display labels never become API identifiers. Repeated therapy query parameters preserve all selected values.

Map and List consume the same `providers` list. List retains every returned provider. Map derives only providers with valid coordinates and never manufactures `(0, 0)` markers.

## User Experience

### Onboarding

The flow contains five user moments:

1. Choose `Parent or family` or `Clinician`.
2. Enter a five-digit ZIP or request device location.
3. Review the matched regional center, or continue with a clear unmatched/offline state.
4. Choose the current journey stage.
5. Optionally choose an age group.

ZIP input accepts only five ASCII digits. Device location is translated to ZIP when permission and geocoding succeed. Location denial or failure returns to ZIP entry without repeatedly requesting permission.

A valid unmatched ZIP may continue with `regionalCenter = null`. A network failure is displayed separately and offers Retry; first-run offline users may continue with their ZIP and no center. On edit, a failed lookup never destroys the persisted profile because changes remain in the local draft until Save.

### Home

Home reads profile and discovery state rather than owning ZIP locally. It shows either a matched-center card or a ZIP entry card. A successful ZIP submission replaces profile ZIP and center together, switches discovery origin to that ZIP, refreshes once, and stays on Home so the matched-center card is visible. Separate Map and List actions open those discovery surfaces.

Therapy shortcuts set one canonical therapy filter before opening List. Journey stage supplies the same family-oriented next-step categories as the iPhone app. New user-facing copy uses `KiNDD` capitalization.

### Map and List

Map and List expose the same search field and active-filter model. Changing query or filters on one is visible on the other after a tab switch. Both can open a shared filter sheet for therapy, age, diagnosis, insurance, and radius. Active filters appear as removable chips with Clear All.

Map shows the coordinate-bearing subset of the shared result set. List shows all results and owns presentation-only sorting without mutating shared result identity. Provider-detail navigation remains unchanged.

### Settings and profile edit

Settings gains `Edit Profile & Onboarding` and `Clear Profile & Restart`. Edit opens a prefilled draft. Clear requires confirmation, removes every saved profile field atomically, and returns through the startup gate.

## Loading, Empty, and Error Policy

- Initial load with no successful results uses a full loading or error state.
- Refresh with existing results keeps those results visible and adds a nonblocking progress indicator.
- Refresh failure with existing results preserves them and shows a Retry banner.
- No matches is an explicit empty state that retains query and active filters.
- Invalid ZIP, unmatched ZIP, network failure, and location denial use distinct messages.
- Exceptions are converted to a small typed `DiscoveryError`; raw backend bodies, exception messages, and stack-oriented details are never user-facing.
- Clearing a query or filter immediately updates criteria and supersedes any earlier request.

## Privacy and Data Retention

- The profile DataStore file is excluded from Android cloud backup and device transfer through `backup_rules.xml` and `data_extraction_rules.xml`.
- Profile data is removed when application data is cleared or the app is uninstalled.
- The app provides explicit profile reset behavior.
- Verbose OkHttp body logging is disabled before profile-derived requests are introduced. Diagnostic logging is limited to sanitized endpoint names, status classes, durations, and result counts; it excludes query strings, ZIP codes, chat text, response bodies, and profile fields.

## Visual and Accessibility Contract

Only touched surfaces are aligned in this slice:

- Indigo `#6366F1`
- Deep indigo `#4F46E5`
- Violet `#8B5CF6`
- Purple `#A855F7`
- Pink `#EC4899`
- Matched green `#10B981`
- Grouped adaptive canvas and card surfaces
- 20dp card radius and clear pressed states

Android retains Material components, system navigation behavior, dark-theme adaptation, and Reduce Motion behavior. New strings are added in English and Spanish. All controls have TalkBack labels, meaningful traversal order, 48dp minimum touch targets, and layouts that remain usable with large font scales.

## Testing Strategy

### Persistence tests

- Clean DataStore emits an incomplete default profile.
- Atomic save and repository recreation restore every field.
- Matched ZIP replaced by unmatched ZIP clears the prior center.
- Unknown enum or corrupt values fall back safely.
- Clear Profile restores the default incomplete profile with no stale fields.

### Discovery tests

- Request decision table selects ZIP, location, comprehensive, or catalog behavior correctly.
- Repeated therapy parameters and all deployed query names are exact.
- Home therapy action changes criteria before navigation is emitted.
- Rapid query changes cancel or ignore stale results.
- Failed refresh preserves prior results and exposes Retry.
- Map and List collectors receive identical provider IDs and criteria.
- Providers without coordinates remain in List and are absent from Map markers.

### UI and navigation tests

- Loading gate never flashes Home.
- New and migrated installs enter onboarding once.
- Completed profile enters Home after process recreation.
- Profile edit is prefilled and Cancel is nondestructive.
- Find button and keyboard action have identical ZIP behavior.
- Therapy shortcut opens List with the active filter visible.
- List-to-Map tab switching preserves query, filters, and result identity.
- Dark theme, large font, English, Spanish, TalkBack, and location-denied states remain usable.

### Completion verification

The slice is complete only after:

1. Focused unit and contract tests pass.
2. The full Android unit suite passes.
3. Android lint reports no errors.
4. A debug APK assembles with Android Studio's bundled JBR.
5. The APK installs on the Pixel emulator.
6. A fresh-install smoke proves onboarding, profile persistence, Home shortcut filtering, shared List/Map results, profile editing, and relaunch-to-Home behavior.
7. An independent diff review reports no unresolved Critical or Important findings.

## Follow-On Phases

1. Regional-center GeoJSON polygons, provider marker styling and clustering, Regions navigation, and provider-detail parity.
2. Streaming chat with safe Markdown, Stop/Retry/Clear, typed profile context, provider actions, sanitized failure states, and privacy protections.
3. Full Home/navigation visual parity and final cross-platform accessibility and screenshot pass.

Each follow-on phase receives its own approved design and testable implementation plan.
