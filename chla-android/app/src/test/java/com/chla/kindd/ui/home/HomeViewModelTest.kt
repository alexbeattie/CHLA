package com.chla.kindd.ui.home

import com.chla.kindd.data.discovery.FakeDiscoveryController
import com.chla.kindd.data.discovery.TherapyType
import com.chla.kindd.data.models.RegionalCenter
import com.chla.kindd.data.profile.AgeGroup
import com.chla.kindd.data.profile.AudienceType
import com.chla.kindd.data.profile.JourneyStage
import com.chla.kindd.data.profile.RegionalCenterIdentity
import com.chla.kindd.data.profile.UserProfile
import com.chla.kindd.data.profile.UserProfileRepository
import com.chla.kindd.data.source.LookupFailure
import com.chla.kindd.data.source.RegionalCenterDataSource
import com.chla.kindd.data.source.RegionalCenterLookup
import com.chla.kindd.testing.MainDispatcherRule
import com.chla.kindd.ui.chat.ChatLaunchPrompt
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun profileFlowRendersEverySavedField_andHydratesCenterByDeployedId() =
        runTest(mainDispatcherRule.testDispatcher) {
            val details = center(id = 41, name = "Current deployed name", phone = "(213) 555-1212")
            val profile = profile(
                zip = "90001",
                identity = RegionalCenterIdentity(41, "Saved name", "SCLARC"),
                audience = AudienceType.CLINICIAN,
                journey = JourneyStage.RECEIVING_SERVICES,
                age = AgeGroup.ADOLESCENT
            )
            val fixture = fixture(profile, CenterSource(centers = Result.success(listOf(details))))
            runCurrent()

            assertEquals(profile, fixture.viewModel.uiState.value.profile)
            assertEquals("90001", fixture.viewModel.uiState.value.zipDraft)
            assertEquals(AudienceType.CLINICIAN, fixture.viewModel.uiState.value.profile.audienceType)
            assertEquals(JourneyStage.RECEIVING_SERVICES, fixture.viewModel.uiState.value.profile.journeyStage)
            assertEquals(AgeGroup.ADOLESCENT, fixture.viewModel.uiState.value.profile.ageGroup)
            assertEquals(details, fixture.viewModel.uiState.value.hydratedCenter)
        }

    @Test
    fun zipLookupRequiresExactlyFiveAsciiDigits() = runTest(mainDispatcherRule.testDispatcher) {
        val fixture = fixture(profile())

        fixture.viewModel.onZipChanged("12a3٤4")
        assertEquals("1234", fixture.viewModel.uiState.value.zipDraft)
        fixture.viewModel.submitZip()
        runCurrent()

        assertTrue(fixture.centers.lookups.isEmpty())
        assertEquals(HomeMessage.INVALID_ZIP, fixture.viewModel.uiState.value.message)
    }

    @Test
    fun matchedLookupReplacesOnlyZipAndIdentity_once_withoutDirectDiscoveryRefresh() =
        runTest(mainDispatcherRule.testDispatcher) {
            val original = profile(
                zip = "90001",
                identity = null,
                audience = AudienceType.CLINICIAN,
                journey = JourneyStage.WAITING_FOR_INTAKE,
                age = AgeGroup.SCHOOL_AGE
            )
            val matched = center()
            val fixture = fixture(
                original,
                CenterSource(lookup = RegionalCenterLookup.Matched(matched))
            )
            fixture.viewModel.onZipChanged("90210")

            fixture.viewModel.submitZip()
            runCurrent()

            assertEquals(1, fixture.repository.replacements.size)
            assertEquals(
                original.copy(zipCode = "90210", regionalCenter = RegionalCenterIdentity.from(matched)),
                fixture.repository.replacements.single()
            )
            assertEquals(emptyList<String>(), fixture.discovery.calls)
            assertEquals(HomeLookupState.MATCHED, fixture.viewModel.uiState.value.lookupState)
        }

    @Test
    fun zipDraftChangeInvalidatesCancellationIgnoringLookup_beforeItCanPersist() =
        runTest(mainDispatcherRule.testDispatcher) {
            val oldMatch = CompletableDeferred<RegionalCenterLookup>()
            val source = ControlledLookupCenterSource(ArrayDeque(listOf(oldMatch)))
            val original = profile(zip = "90001", identity = null)
            val repository = RecordingProfileRepository(original)
            val viewModel = HomeViewModel(repository, source, FakeDiscoveryController())
            runCurrent()

            viewModel.onZipChanged("90001")
            viewModel.submitZip()
            runCurrent()
            viewModel.onZipChanged("90210")

            assertEquals("90210", viewModel.uiState.value.zipDraft)
            assertEquals(HomeLookupState.IDLE, viewModel.uiState.value.lookupState)

            oldMatch.complete(RegionalCenterLookup.Matched(center(id = 1, name = "Old")))
            runCurrent()

            assertEquals(original, repository.current)
            assertTrue(repository.replacements.isEmpty())
            assertEquals("90210", viewModel.uiState.value.zipDraft)
            assertEquals(HomeLookupState.IDLE, viewModel.uiState.value.lookupState)
        }

    @Test
    fun everyZipSubmitSupersedesCancellationIgnoringLookup_whenNewMatchFinishesFirst() =
        runTest(mainDispatcherRule.testDispatcher) {
            val oldMatch = CompletableDeferred<RegionalCenterLookup>()
            val newMatch = CompletableDeferred<RegionalCenterLookup>()
            val source = ControlledLookupCenterSource(ArrayDeque(listOf(oldMatch, newMatch)))
            val original = profile(zip = "90001", identity = null)
            val repository = RecordingProfileRepository(original)
            val viewModel = HomeViewModel(repository, source, FakeDiscoveryController())
            runCurrent()

            viewModel.onZipChanged("90210")
            viewModel.submitZip()
            runCurrent()
            viewModel.submitZip()
            runCurrent()

            val expectedCenter = center(id = 2, name = "New")
            newMatch.complete(RegionalCenterLookup.Matched(expectedCenter))
            runCurrent()
            oldMatch.complete(RegionalCenterLookup.Matched(center(id = 1, name = "Old")))
            runCurrent()

            val expectedProfile = original.copy(
                zipCode = "90210",
                regionalCenter = RegionalCenterIdentity.from(expectedCenter)
            )
            assertEquals(expectedProfile, repository.current)
            assertEquals(listOf(expectedProfile), repository.replacements)
            assertEquals(HomeLookupState.MATCHED, viewModel.uiState.value.lookupState)
        }

    @Test
    fun externalProfileChangeSupersedesCancellationIgnoringLookup_beforeItCanPersist() =
        runTest(mainDispatcherRule.testDispatcher) {
            val pendingMatch = CompletableDeferred<RegionalCenterLookup>()
            val source = ControlledLookupCenterSource(ArrayDeque(listOf(pendingMatch)))
            val original = profile(zip = "90001", identity = null)
            val externallyEdited = profile(
                zip = "91311",
                identity = RegionalCenterIdentity(91, "Externally edited", "EXTERNAL"),
                audience = AudienceType.CLINICIAN,
                journey = JourneyStage.RECEIVING_SERVICES,
                age = AgeGroup.ADULT
            )
            val repository = RecordingProfileRepository(original)
            val viewModel = HomeViewModel(repository, source, FakeDiscoveryController())
            runCurrent()

            viewModel.onZipChanged("90210")
            viewModel.submitZip()
            runCurrent()
            repository.emit(externallyEdited)
            runCurrent()

            pendingMatch.complete(RegionalCenterLookup.Matched(center(id = 22, name = "Late")))
            runCurrent()

            assertEquals(externallyEdited, repository.current)
            assertTrue(repository.replacements.isEmpty())
            assertEquals("91311", viewModel.uiState.value.zipDraft)
            assertEquals(HomeLookupState.IDLE, viewModel.uiState.value.lookupState)
        }

    @Test
    fun committedProfileChangeNotYetObservedByFlow_cannotBeOverwrittenByLateLookup() =
        runTest(mainDispatcherRule.testDispatcher) {
            val pendingMatch = CompletableDeferred<RegionalCenterLookup>()
            val source = ControlledLookupCenterSource(ArrayDeque(listOf(pendingMatch)))
            val original = profile(zip = "90001", identity = null)
            val externallyEdited = profile(
                zip = "91311",
                identity = RegionalCenterIdentity(91, "Externally edited", "WRC"),
                audience = AudienceType.CLINICIAN,
                journey = JourneyStage.RECEIVING_SERVICES,
                age = AgeGroup.ADULT
            )
            val repository = LaggingProfileRepository(original)
            val viewModel = HomeViewModel(repository, source, FakeDiscoveryController())
            runCurrent()

            viewModel.onZipChanged("90210")
            viewModel.submitZip()
            runCurrent()
            repository.commitWithoutFlowEmission(externallyEdited)

            pendingMatch.complete(RegionalCenterLookup.Matched(center(id = 22, name = "Late")))
            runCurrent()

            assertEquals(externallyEdited, repository.actualProfile)
            assertTrue(repository.unconditionalReplacements.isEmpty())
            assertEquals(HomeLookupState.IDLE, viewModel.uiState.value.lookupState)
        }

    @Test
    fun lookupOrProfileWriteFailure_exposesSanitizedRetryableState_andKeepsProfile() =
        runTest(mainDispatcherRule.testDispatcher) {
            val original = profile(zip = "90001", identity = null)

            val lookupFailureFixture = fixture(
                original,
                CenterSource(lookupFailure = IllegalStateException("private lookup body 90001"))
            )
            lookupFailureFixture.viewModel.onZipChanged("90210")
            lookupFailureFixture.viewModel.submitZip()
            runCurrent()

            assertEquals(original, lookupFailureFixture.repository.current)
            assertEquals(HomeLookupState.UNAVAILABLE, lookupFailureFixture.viewModel.uiState.value.lookupState)
            assertEquals(HomeMessage.LOOKUP_UNAVAILABLE, lookupFailureFixture.viewModel.uiState.value.message)
            assertFalse(lookupFailureFixture.viewModel.uiState.value.message.toString().contains("private"))

            val writeFailureFixture = fixture(
                original,
                CenterSource(lookup = RegionalCenterLookup.Matched(center()))
            )
            writeFailureFixture.repository.replaceFailure =
                IllegalStateException("private write body 90210")
            writeFailureFixture.viewModel.onZipChanged("90210")
            writeFailureFixture.viewModel.submitZip()
            runCurrent()

            assertEquals(original, writeFailureFixture.repository.current)
            assertTrue(writeFailureFixture.repository.replacements.isEmpty())
            assertEquals(HomeLookupState.UNAVAILABLE, writeFailureFixture.viewModel.uiState.value.lookupState)
            assertEquals(HomeMessage.LOOKUP_UNAVAILABLE, writeFailureFixture.viewModel.uiState.value.message)
            assertFalse(writeFailureFixture.viewModel.uiState.value.message.toString().contains("private"))
        }

    @Test
    fun unmatchedAndUnavailableLeaveCurrentProfileByteForByteUnchanged() =
        runTest(mainDispatcherRule.testDispatcher) {
            val original = profile()
            listOf(
                RegionalCenterLookup.Unmatched,
                RegionalCenterLookup.Unavailable(LookupFailure.NETWORK)
            ).forEach { result ->
                val fixture = fixture(original, CenterSource(lookup = result))
                fixture.viewModel.onZipChanged("90210")
                fixture.viewModel.submitZip()
                runCurrent()

                assertEquals(original, fixture.repository.current)
                assertTrue(fixture.repository.replacements.isEmpty())
                val message = fixture.viewModel.uiState.value.message
                assertFalse(message.toString().contains("90210"))
            }
        }

    @Test
    fun therapyIsSetSynchronouslyBeforeListNavigation_forAllHomeShortcuts() =
        runTest(mainDispatcherRule.testDispatcher) {
            val fixture = fixture(profile())
            val timeline = mutableListOf<String>()
            fixture.discovery.onSingleTherapyAndRefresh = { therapy ->
                timeline += "therapy:${therapy.apiValue}"
            }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                fixture.viewModel.events.collect { event -> timeline += "event:$event" }
            }

            listOf(TherapyType.ABA, TherapyType.SPEECH, TherapyType.OCCUPATIONAL, TherapyType.PHYSICAL)
                .forEach { therapy ->
                    timeline.clear()
                    fixture.viewModel.selectTherapy(therapy)
                    runCurrent()
                    assertEquals(therapy, fixture.discovery.singleTherapies.last())
                    assertEquals(
                        listOf(
                            "therapy:${therapy.apiValue}",
                            "event:${HomeEvent.NavigateToList}"
                        ),
                        timeline
                    )
                }
        }

    @Test
    fun mapListDetailsAndJourneyChatEmitDistinctTypedEvents() =
        runTest(mainDispatcherRule.testDispatcher) {
            val fixture = fixture(profile())
            val events = collectEvents(fixture.viewModel)

            fixture.viewModel.openMap()
            fixture.viewModel.openList()
            fixture.viewModel.openRegionalCenters()
            fixture.viewModel.openChat(ChatLaunchPrompt.JUST_DIAGNOSED)
            fixture.viewModel.openChat(ChatLaunchPrompt.WAITING_INTAKE)
            fixture.viewModel.openChat(ChatLaunchPrompt.RECEIVING_SERVICES)
            runCurrent()

            assertEquals(
                listOf(
                    HomeEvent.NavigateToMap,
                    HomeEvent.NavigateToList,
                    HomeEvent.NavigateToRegionalCenters,
                    HomeEvent.NavigateToChat(ChatLaunchPrompt.JUST_DIAGNOSED),
                    HomeEvent.NavigateToChat(ChatLaunchPrompt.WAITING_INTAKE),
                    HomeEvent.NavigateToChat(ChatLaunchPrompt.RECEIVING_SERVICES)
                ),
                events
            )
        }

    @Test
    fun callNowEmitsOnlyAsciiDialDigits_andIsUnavailableWithoutHydratedPhone() =
        runTest(mainDispatcherRule.testDispatcher) {
            val identity = RegionalCenterIdentity(7, "South Central Los Angeles Regional Center", "SCLARC")
            val fixture = fixture(
                profile(identity = identity),
                CenterSource(centers = Result.success(listOf(center(phone = "+1 (213) 555-12٥6"))))
            )
            val events = collectEvents(fixture.viewModel)
            runCurrent()

            val dialDigits = fixture.viewModel.uiState.value.dialDigits
            requireNotNull(dialDigits)
            fixture.viewModel.callCenter(dialDigits)
            runCurrent()
            assertEquals(HomeEvent.Dial("1213555126"), events.single())

            fixture.repository.emit(
                profile(identity = identity.copy(id = 99, name = "Unknown", shortName = "UNKNOWN"))
            )
            runCurrent()
            fixture.viewModel.uiState.value.dialDigits?.let(fixture.viewModel::callCenter)
            fixture.viewModel.callCenter("+1 (213) 555-1212")
            fixture.viewModel.callCenter("١٢٣")
            runCurrent()
            assertEquals(1, events.size)
        }

    @Test
    fun hydrationFallsBackToCanonicalNameOrShortName_andFailureKeepsIdentityCardUsable() =
        runTest(mainDispatcherRule.testDispatcher) {
            val fallback = center(id = 8, name = "Westside Regional Center", phone = "310-555-1010")
            val byName = fixture(
                profile(identity = RegionalCenterIdentity(999, "Westside Regional Center", "WRC")),
                CenterSource(centers = Result.success(listOf(fallback)))
            )
            runCurrent()
            assertEquals(fallback, byName.viewModel.uiState.value.hydratedCenter)

            val failedProfile = profile(identity = RegionalCenterIdentity(999, "Saved Center", "WRC"))
            val failed = fixture(failedProfile, CenterSource(centers = Result.failure(Exception("private"))))
            runCurrent()
            assertEquals(failedProfile.regionalCenter, failed.viewModel.uiState.value.profile.regionalCenter)
            assertNull(failed.viewModel.uiState.value.hydratedCenter)
            assertNull(failed.viewModel.uiState.value.dialDigits)
        }

    @Test
    fun identityChangeClearsDetailsImmediately_andLateOldResponseCannotRestoreThemOrDial() =
        runTest(mainDispatcherRule.testDispatcher) {
            val initialGate = CompletableDeferred<Result<List<RegionalCenter>>>()
            val staleGate = CompletableDeferred<Result<List<RegionalCenter>>>()
            val latestGate = CompletableDeferred<Result<List<RegionalCenter>>>()
            val source = QueuedCenterSource(
                ArrayDeque(listOf(initialGate, staleGate, latestGate))
            )
            val initialIdentity = RegionalCenterIdentity(1, "Initial", "INITIAL")
            val staleIdentity = RegionalCenterIdentity(2, "Stale", "STALE")
            val latestIdentity = RegionalCenterIdentity(3, "Latest", "LATEST")
            val fixture = fixture(profile(identity = initialIdentity), source)
            val events = collectEvents(fixture.viewModel)
            runCurrent()

            val initialDetails = center(id = 1, name = "Initial", phone = "111-111-1111")
            initialGate.complete(Result.success(listOf(initialDetails)))
            runCurrent()
            assertEquals(initialDetails, fixture.viewModel.uiState.value.hydratedCenter)
            val capturedInitialDigits = fixture.viewModel.uiState.value.dialDigits
            requireNotNull(capturedInitialDigits)
            fixture.viewModel.callCenter(capturedInitialDigits)
            runCurrent()
            assertEquals(HomeEvent.Dial("1111111111"), events.single())
            events.clear()

            fixture.repository.emit(profile(identity = staleIdentity))
            runCurrent()
            assertNull(fixture.viewModel.uiState.value.hydratedCenter)
            fixture.viewModel.callCenter(capturedInitialDigits)
            runCurrent()
            assertTrue(events.isEmpty())

            fixture.repository.emit(profile(identity = latestIdentity))
            runCurrent()
            val latestDetails = center(id = 3, name = "Latest", phone = "333-333-3333")
            latestGate.complete(Result.success(listOf(latestDetails)))
            runCurrent()
            assertEquals(latestDetails, fixture.viewModel.uiState.value.hydratedCenter)
            fixture.viewModel.callCenter(capturedInitialDigits)
            runCurrent()
            assertTrue(events.isEmpty())

            staleGate.complete(
                Result.success(listOf(center(id = 2, name = "Stale", phone = "222-222-2222")))
            )
            runCurrent()

            assertEquals(latestDetails, fixture.viewModel.uiState.value.hydratedCenter)
            fixture.viewModel.callCenter(capturedInitialDigits)
            runCurrent()
            assertTrue(events.isEmpty())
            val dialDigits = fixture.viewModel.uiState.value.dialDigits
            requireNotNull(dialDigits)
            fixture.viewModel.callCenter(dialDigits)
            runCurrent()
            assertEquals(HomeEvent.Dial("3333333333"), events.single())
        }

    private fun kotlinx.coroutines.test.TestScope.collectEvents(viewModel: HomeViewModel): MutableList<HomeEvent> {
        val events = mutableListOf<HomeEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.events.collect(events::add) }
        return events
    }

    private fun fixture(
        initial: UserProfile,
        centers: RegionalCenterDataSource = CenterSource()
    ): Fixture {
        val repository = RecordingProfileRepository(initial)
        val discovery = FakeDiscoveryController()
        return Fixture(
            HomeViewModel(repository, centers, discovery),
            repository,
            centers as? CenterSource ?: CenterSource(),
            discovery
        )
    }

    private data class Fixture(
        val viewModel: HomeViewModel,
        val repository: RecordingProfileRepository,
        val centers: CenterSource,
        val discovery: FakeDiscoveryController
    )

    private class RecordingProfileRepository(initial: UserProfile) : UserProfileRepository {
        private val profiles = MutableStateFlow(initial)
        val replacements = mutableListOf<UserProfile>()
        val current get() = profiles.value
        var replaceFailure: Throwable? = null
        override val profile: Flow<UserProfile> = profiles
        override suspend fun replaceProfile(profile: UserProfile) {
            replaceFailure?.let { throw it }
            replacements += profile
            profiles.value = profile
        }
        override suspend fun replaceProfileIfCurrent(
            expected: UserProfile,
            replacement: UserProfile
        ): Boolean {
            replaceFailure?.let { throw it }
            if (profiles.value != expected) return false
            replacements += replacement
            profiles.value = replacement
            return true
        }
        override suspend fun clearProfile() { profiles.value = UserProfile() }
        fun emit(profile: UserProfile) { profiles.value = profile }
    }

    private class LaggingProfileRepository(initial: UserProfile) : UserProfileRepository {
        private val observedProfiles = MutableStateFlow(initial)
        var actualProfile = initial
            private set
        val unconditionalReplacements = mutableListOf<UserProfile>()
        override val profile: Flow<UserProfile> = observedProfiles

        override suspend fun replaceProfile(profile: UserProfile) {
            unconditionalReplacements += profile
            actualProfile = profile
        }

        override suspend fun replaceProfileIfCurrent(
            expected: UserProfile,
            replacement: UserProfile
        ): Boolean {
            if (actualProfile != expected) return false
            actualProfile = replacement
            return true
        }

        override suspend fun clearProfile() {
            actualProfile = UserProfile()
        }

        fun commitWithoutFlowEmission(profile: UserProfile) {
            actualProfile = profile
        }
    }

    private class CenterSource(
        var centers: Result<List<RegionalCenter>> = Result.success(emptyList()),
        var lookup: RegionalCenterLookup = RegionalCenterLookup.Unmatched,
        var lookupFailure: Throwable? = null
    ) : RegionalCenterDataSource {
        val lookups = mutableListOf<String>()
        override suspend fun getRegionalCenters() = centers
        override suspend fun getRegionalCentersNearby(latitude: Double, longitude: Double) = centers
        override suspend fun lookupRegionalCenter(zipCode: String): RegionalCenterLookup {
            lookups += zipCode
            lookupFailure?.let { throw it }
            return lookup
        }
    }

    private class QueuedCenterSource(
        private val gates: ArrayDeque<CompletableDeferred<Result<List<RegionalCenter>>>>
    ) : RegionalCenterDataSource {
        override suspend fun getRegionalCenters(): Result<List<RegionalCenter>> =
            withContext(NonCancellable) { gates.removeFirst().await() }
        override suspend fun getRegionalCentersNearby(latitude: Double, longitude: Double) =
            Result.success(emptyList<RegionalCenter>())
        override suspend fun lookupRegionalCenter(zipCode: String) = RegionalCenterLookup.Unmatched
    }

    private class ControlledLookupCenterSource(
        private val gates: ArrayDeque<CompletableDeferred<RegionalCenterLookup>>
    ) : RegionalCenterDataSource {
        val lookups = mutableListOf<String>()
        override suspend fun getRegionalCenters() = Result.success(emptyList<RegionalCenter>())
        override suspend fun getRegionalCentersNearby(latitude: Double, longitude: Double) =
            Result.success(emptyList<RegionalCenter>())
        override suspend fun lookupRegionalCenter(zipCode: String): RegionalCenterLookup {
            lookups += zipCode
            return withContext(NonCancellable) { gates.removeFirst().await() }
        }
    }

    private fun profile(
        zip: String = "90001",
        identity: RegionalCenterIdentity? = null,
        audience: AudienceType = AudienceType.FAMILY,
        journey: JourneyStage = JourneyStage.JUST_DIAGNOSED,
        age: AgeGroup? = AgeGroup.EARLY_INTERVENTION
    ) = UserProfile(true, audience, zip, identity, journey, age)

    private fun center(
        id: Int = 7,
        name: String = "South Central Los Angeles Regional Center",
        phone: String? = "213-555-1212"
    ) = RegionalCenter(id = id, name = name, telephone = phone, countyServed = "Los Angeles")
}
