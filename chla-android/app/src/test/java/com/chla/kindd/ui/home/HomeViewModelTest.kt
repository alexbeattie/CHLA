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
            val events = collectEvents(fixture.viewModel)

            listOf(TherapyType.ABA, TherapyType.SPEECH, TherapyType.OCCUPATIONAL, TherapyType.PHYSICAL)
                .forEach { therapy ->
                    fixture.viewModel.selectTherapy(therapy)
                    runCurrent()
                    assertEquals(therapy, fixture.discovery.singleTherapies.last())
                    assertEquals(HomeEvent.NavigateToList, events.last())
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

            fixture.viewModel.callCenter()
            runCurrent()
            assertEquals(HomeEvent.Dial("1213555126"), events.single())

            fixture.repository.emit(
                profile(identity = identity.copy(id = 99, name = "Unknown", shortName = "UNKNOWN"))
            )
            runCurrent()
            fixture.viewModel.callCenter()
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
            val firstGate = CompletableDeferred<Result<List<RegionalCenter>>>()
            val secondGate = CompletableDeferred<Result<List<RegionalCenter>>>()
            val source = QueuedCenterSource(ArrayDeque(listOf(firstGate, secondGate)))
            val firstIdentity = RegionalCenterIdentity(1, "First", "FIRST")
            val secondIdentity = RegionalCenterIdentity(2, "Second", "SECOND")
            val fixture = fixture(profile(identity = firstIdentity), source)
            val events = collectEvents(fixture.viewModel)
            runCurrent()

            fixture.repository.emit(profile(identity = secondIdentity))
            assertNull(fixture.viewModel.uiState.value.hydratedCenter)
            fixture.viewModel.callCenter()
            runCurrent()
            assertTrue(events.isEmpty())

            secondGate.complete(Result.success(listOf(center(id = 2, name = "Second", phone = "222-222-2222"))))
            runCurrent()
            firstGate.complete(Result.success(listOf(center(id = 1, name = "First", phone = "111-111-1111"))))
            runCurrent()

            assertEquals(2, fixture.viewModel.uiState.value.hydratedCenter?.id)
            fixture.viewModel.callCenter()
            runCurrent()
            assertEquals(HomeEvent.Dial("2222222222"), events.single())
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
        override val profile: Flow<UserProfile> = profiles
        override suspend fun replaceProfile(profile: UserProfile) {
            replacements += profile
            profiles.value = profile
        }
        override suspend fun clearProfile() { profiles.value = UserProfile() }
        fun emit(profile: UserProfile) { profiles.value = profile }
    }

    private class CenterSource(
        var centers: Result<List<RegionalCenter>> = Result.success(emptyList()),
        var lookup: RegionalCenterLookup = RegionalCenterLookup.Unmatched
    ) : RegionalCenterDataSource {
        val lookups = mutableListOf<String>()
        override suspend fun getRegionalCenters() = centers
        override suspend fun getRegionalCentersNearby(latitude: Double, longitude: Double) = centers
        override suspend fun lookupRegionalCenter(zipCode: String): RegionalCenterLookup {
            lookups += zipCode
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
