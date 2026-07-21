package com.chla.kindd.ui.onboarding

import com.chla.kindd.data.models.RegionalCenter
import com.chla.kindd.data.profile.AgeGroup
import com.chla.kindd.data.profile.AudienceType
import com.chla.kindd.data.profile.JourneyStage
import com.chla.kindd.data.profile.RegionalCenterIdentity
import com.chla.kindd.data.profile.UserProfile
import com.chla.kindd.data.profile.UserProfileRepository
import com.chla.kindd.data.source.FakeRegionalCenterDataSource
import com.chla.kindd.data.source.FakeUserLocationSource
import com.chla.kindd.data.source.LookupFailure
import com.chla.kindd.data.source.RegionalCenterLookup
import com.chla.kindd.data.source.UserCoordinates
import com.chla.kindd.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun firstRun_defaultsOnlyAudienceToFamily() = runTest(mainDispatcherRule.testDispatcher) {
        val fixture = fixture()

        fixture.viewModel.initialize(OnboardingMode.FIRST_RUN, UserProfile())

        assertEquals(OnboardingMode.FIRST_RUN, fixture.viewModel.uiState.value.mode)
        assertEquals(OnboardingStep.AUDIENCE, fixture.viewModel.uiState.value.step)
        assertEquals(
            UserProfile(audienceType = AudienceType.FAMILY),
            fixture.viewModel.uiState.value.draft
        )
    }

    @Test
    fun edit_snapshotsTheCompleteProfileExactlyOnce() = runTest(mainDispatcherRule.testDispatcher) {
        val fixture = fixture()
        val saved = completeProfile()

        fixture.viewModel.initialize(OnboardingMode.EDIT, saved)

        assertEquals(saved, fixture.viewModel.uiState.value.draft)
        fixture.viewModel.onZipChanged("90210")
        fixture.viewModel.initialize(
            OnboardingMode.EDIT,
            completeProfile(zipCode = "91311", ageGroup = AgeGroup.ADULT)
        )
        assertEquals("90210", fixture.viewModel.uiState.value.draft.zipCode)
        assertNull(fixture.viewModel.uiState.value.draft.regionalCenter)
        assertEquals(AgeGroup.SCHOOL_AGE, fixture.viewModel.uiState.value.draft.ageGroup)
    }

    @Test
    fun completedFirstRunSession_teardownAllowsFreshClearedProfileInitialization() =
        runTest(mainDispatcherRule.testDispatcher) {
            val fixture = fixture()
            advanceToAge(fixture)
            fixture.viewModel.selectAudience(AudienceType.CLINICIAN)
            fixture.viewModel.finish()
            runCurrent()

            fixture.viewModel.endSession()
            val laterEvents = mutableListOf<OnboardingEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                fixture.viewModel.events.collect(laterEvents::add)
            }
            fixture.viewModel.initialize(OnboardingMode.FIRST_RUN, UserProfile())
            runCurrent()

            assertEquals(
                OnboardingUiState(
                    mode = OnboardingMode.FIRST_RUN,
                    step = OnboardingStep.AUDIENCE,
                    draft = UserProfile(audienceType = AudienceType.FAMILY)
                ),
                fixture.viewModel.uiState.value
            )
            assertTrue(laterEvents.isEmpty())
        }

    @Test
    fun successfulSave_marksSessionEndedSoRetainedViewModelCanInitializeFreshProfile() =
        runTest(mainDispatcherRule.testDispatcher) {
            val fixture = fixture()
            advanceToAge(fixture)
            fixture.viewModel.finish()
            runCurrent()

            fixture.viewModel.initialize(OnboardingMode.FIRST_RUN, UserProfile())

            assertEquals(OnboardingStep.AUDIENCE, fixture.viewModel.uiState.value.step)
            assertEquals(
                UserProfile(audienceType = AudienceType.FAMILY),
                fixture.viewModel.uiState.value.draft
            )
        }

    @Test
    fun editCancel_marksSessionEndedSoRetainedViewModelCanInitializeFreshProfile() =
        runTest(mainDispatcherRule.testDispatcher) {
            val saved = completeProfile()
            val fixture = fixture(repository = RecordingProfileRepository(saved))
            fixture.viewModel.initialize(OnboardingMode.EDIT, saved)
            fixture.viewModel.onZipChanged("90210")

            fixture.viewModel.cancel()
            fixture.viewModel.initialize(OnboardingMode.FIRST_RUN, UserProfile())

            assertEquals(OnboardingStep.AUDIENCE, fixture.viewModel.uiState.value.step)
            assertEquals(
                UserProfile(audienceType = AudienceType.FAMILY),
                fixture.viewModel.uiState.value.draft
            )
            assertTrue(fixture.repository.replacedProfiles.isEmpty())
        }

    @Test
    fun successfulSave_resetsPresentationSynchronously_withoutDroppingSavedEvent() =
        runTest(mainDispatcherRule.testDispatcher) {
            val fixture = fixture()
            val events = mutableListOf<OnboardingEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                fixture.viewModel.events.collect(events::add)
            }
            advanceToAge(fixture)

            fixture.viewModel.finish()
            runCurrent()

            assertEquals(listOf(OnboardingEvent.Saved), events)
            assertEquals(OnboardingUiState(), fixture.viewModel.uiState.value)
        }

    @Test
    fun editCancel_resetsPresentationSynchronously_withoutDroppingCloseEvent() =
        runTest(mainDispatcherRule.testDispatcher) {
            val saved = completeProfile()
            val fixture = fixture(repository = RecordingProfileRepository(saved))
            val events = mutableListOf<OnboardingEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                fixture.viewModel.events.collect(events::add)
            }
            fixture.viewModel.initialize(OnboardingMode.EDIT, saved)
            fixture.viewModel.onZipChanged("90210")

            fixture.viewModel.cancel()
            runCurrent()

            assertEquals(listOf(OnboardingEvent.Close), events)
            assertEquals(OnboardingUiState(), fixture.viewModel.uiState.value)
            assertTrue(fixture.repository.replacedProfiles.isEmpty())
        }

    @Test
    fun zipInput_keepsOnlyAsciiDigitsAndClampsToFive() =
        runTest(mainDispatcherRule.testDispatcher) {
            val fixture = fixture()
            fixture.viewModel.initialize(OnboardingMode.FIRST_RUN, UserProfile())

            fixture.viewModel.onZipChanged("a1٢2-34567")

            assertEquals("12345", fixture.viewModel.uiState.value.draft.zipCode)
        }

    @Test
    fun changingZip_clearsOnlyDraftCenterAndNeverWritesRepository() =
        runTest(mainDispatcherRule.testDispatcher) {
            val saved = completeProfile()
            val fixture = fixture(repository = RecordingProfileRepository(saved))
            fixture.viewModel.initialize(OnboardingMode.EDIT, saved)

            fixture.viewModel.onZipChanged("90002")

            assertNull(fixture.viewModel.uiState.value.draft.regionalCenter)
            assertEquals(saved, fixture.repository.currentProfile)
            assertTrue(fixture.repository.replacedProfiles.isEmpty())
        }

    @Test
    fun fiveDigitZipLookup_advancesToMatchedCenter() = runTest(mainDispatcherRule.testDispatcher) {
        val center = regionalCenter()
        val fixture = fixture(
            regionalCenters = FakeRegionalCenterDataSource(
                RegionalCenterLookup.Matched(center)
            )
        )
        fixture.viewModel.initialize(OnboardingMode.FIRST_RUN, UserProfile())
        fixture.viewModel.continueFromCurrentStep()
        fixture.viewModel.onZipChanged("90001")

        fixture.viewModel.continueFromCurrentStep()
        runCurrent()

        assertEquals(listOf("90001"), fixture.regionalCenters.lookedUpZipCodes)
        assertEquals(OnboardingStep.REGIONAL_CENTER, fixture.viewModel.uiState.value.step)
        assertEquals(CenterLookupState.MATCHED, fixture.viewModel.uiState.value.centerLookupState)
        assertEquals(
            RegionalCenterIdentity.from(center),
            fixture.viewModel.uiState.value.draft.regionalCenter
        )
        assertTrue(fixture.viewModel.uiState.value.canContinue)
    }

    @Test
    fun backDuringCancellationIgnoringLookup_preventsLateNavigationAndMutation() =
        runTest(mainDispatcherRule.testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            val centers = FakeRegionalCenterDataSource(
                RegionalCenterLookup.Matched(regionalCenter())
            ).apply {
                lookupGate = gate
                ignoreLookupCancellation = true
            }
            val fixture = fixture(regionalCenters = centers)
            fixture.viewModel.initialize(OnboardingMode.FIRST_RUN, UserProfile())
            fixture.viewModel.continueFromCurrentStep()
            fixture.viewModel.onZipChanged("90001")
            fixture.viewModel.continueFromCurrentStep()
            runCurrent()
            assertEquals(CenterLookupState.LOADING, fixture.viewModel.uiState.value.centerLookupState)

            fixture.viewModel.goBack()
            gate.complete(Unit)
            runCurrent()

            assertEquals(OnboardingStep.AUDIENCE, fixture.viewModel.uiState.value.step)
            assertNull(fixture.viewModel.uiState.value.draft.regionalCenter)
            assertEquals(CenterLookupState.IDLE, fixture.viewModel.uiState.value.centerLookupState)
        }

    @Test
    fun zipChangeDuringCancellationIgnoringLookup_preventsLateResultFromReplacingNewDraft() =
        runTest(mainDispatcherRule.testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            val centers = FakeRegionalCenterDataSource(
                RegionalCenterLookup.Matched(regionalCenter())
            ).apply {
                lookupGate = gate
                ignoreLookupCancellation = true
            }
            val fixture = fixture(regionalCenters = centers)
            fixture.viewModel.initialize(OnboardingMode.FIRST_RUN, UserProfile())
            fixture.viewModel.continueFromCurrentStep()
            fixture.viewModel.onZipChanged("90001")
            fixture.viewModel.continueFromCurrentStep()
            runCurrent()

            fixture.viewModel.onZipChanged("90002")
            gate.complete(Unit)
            runCurrent()

            assertEquals(OnboardingStep.ZIP, fixture.viewModel.uiState.value.step)
            assertEquals("90002", fixture.viewModel.uiState.value.draft.zipCode)
            assertNull(fixture.viewModel.uiState.value.draft.regionalCenter)
            assertEquals(CenterLookupState.IDLE, fixture.viewModel.uiState.value.centerLookupState)
        }

    @Test
    fun sessionTeardownDuringCancellationIgnoringLookup_protectsTheNextSession() =
        runTest(mainDispatcherRule.testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            val centers = FakeRegionalCenterDataSource(
                RegionalCenterLookup.Matched(regionalCenter())
            ).apply {
                lookupGate = gate
                ignoreLookupCancellation = true
            }
            val fixture = fixture(regionalCenters = centers)
            fixture.viewModel.initialize(OnboardingMode.FIRST_RUN, UserProfile())
            fixture.viewModel.continueFromCurrentStep()
            fixture.viewModel.onZipChanged("90001")
            fixture.viewModel.continueFromCurrentStep()
            runCurrent()

            fixture.viewModel.endSession()
            fixture.viewModel.initialize(OnboardingMode.FIRST_RUN, UserProfile())
            gate.complete(Unit)
            runCurrent()

            assertEquals(OnboardingStep.AUDIENCE, fixture.viewModel.uiState.value.step)
            assertEquals(
                UserProfile(audienceType = AudienceType.FAMILY),
                fixture.viewModel.uiState.value.draft
            )
            assertEquals(CenterLookupState.IDLE, fixture.viewModel.uiState.value.centerLookupState)
        }

    @Test
    fun noMatch_advancesToUnmatchedAndAllowsContinue() =
        runTest(mainDispatcherRule.testDispatcher) {
            val fixture = fixture()
            advanceToCenter(fixture)

            assertEquals(CenterLookupState.UNMATCHED, fixture.viewModel.uiState.value.centerLookupState)
            assertNull(fixture.viewModel.uiState.value.draft.regionalCenter)
            assertTrue(fixture.viewModel.uiState.value.canContinue)
        }

    @Test
    fun unavailable_firstRunAllowsContinueAndRetry() =
        runTest(mainDispatcherRule.testDispatcher) {
            val fixture = fixture(
                regionalCenters = FakeRegionalCenterDataSource(
                    RegionalCenterLookup.Unavailable(LookupFailure.NETWORK)
                )
            )
            advanceToCenter(fixture)

            assertEquals(CenterLookupState.UNAVAILABLE, fixture.viewModel.uiState.value.centerLookupState)
            assertTrue(fixture.viewModel.uiState.value.canRetryCenterLookup)
            assertTrue(fixture.viewModel.uiState.value.canContinue)

            fixture.regionalCenters.lookupResult = RegionalCenterLookup.Matched(regionalCenter())
            fixture.viewModel.retryCenterLookup()
            runCurrent()

            assertEquals(listOf("90001", "90001"), fixture.regionalCenters.lookedUpZipCodes)
            assertEquals(CenterLookupState.MATCHED, fixture.viewModel.uiState.value.centerLookupState)
        }

    @Test
    fun deviceLocation_reverseGeocodesZipAndUsesTheSameCenterLookup() =
        runTest(mainDispatcherRule.testDispatcher) {
            val coordinates = UserCoordinates(34.0522, -118.2437)
            val location = FakeUserLocationSource(
                permissionGranted = true,
                coordinates = coordinates,
                zipCode = "90001"
            )
            val regionalCenters = FakeRegionalCenterDataSource(
                RegionalCenterLookup.Matched(regionalCenter())
            )
            val fixture = fixture(regionalCenters = regionalCenters, location = location)
            fixture.viewModel.initialize(OnboardingMode.FIRST_RUN, UserProfile())
            fixture.viewModel.continueFromCurrentStep()

            fixture.viewModel.useCurrentLocation()
            runCurrent()

            assertEquals(listOf(coordinates), location.geocodedCoordinates)
            assertEquals("90001", fixture.viewModel.uiState.value.draft.zipCode)
            assertEquals(listOf("90001"), regionalCenters.lookedUpZipCodes)
            assertEquals(CenterLookupState.MATCHED, fixture.viewModel.uiState.value.centerLookupState)
            assertEquals(OnboardingStep.REGIONAL_CENTER, fixture.viewModel.uiState.value.step)
        }

    @Test
    fun backDuringCancellationIgnoringLocation_preventsLateZipAndLookupMutation() =
        runTest(mainDispatcherRule.testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            val location = FakeUserLocationSource(
                permissionGranted = true,
                coordinates = UserCoordinates(34.0522, -118.2437),
                zipCode = "90001"
            ).apply {
                coordinatesGate = gate
                ignoreCancellation = true
            }
            val fixture = fixture(location = location)
            fixture.viewModel.initialize(OnboardingMode.FIRST_RUN, UserProfile())
            fixture.viewModel.continueFromCurrentStep()
            fixture.viewModel.useCurrentLocation()
            runCurrent()
            assertEquals(LocationState.LOCATING, fixture.viewModel.uiState.value.locationState)

            fixture.viewModel.goBack()
            gate.complete(Unit)
            runCurrent()

            assertEquals(OnboardingStep.AUDIENCE, fixture.viewModel.uiState.value.step)
            assertNull(fixture.viewModel.uiState.value.draft.zipCode)
            assertNull(fixture.viewModel.uiState.value.draft.regionalCenter)
            assertTrue(fixture.regionalCenters.lookedUpZipCodes.isEmpty())
            assertEquals(LocationState.IDLE, fixture.viewModel.uiState.value.locationState)
        }

    @Test
    fun permissionDeniedDuringCancellationIgnoringLookup_restoresUsableZipAndRejectsLateResult() =
        runTest(mainDispatcherRule.testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            val centers = FakeRegionalCenterDataSource(
                RegionalCenterLookup.Matched(regionalCenter())
            ).apply {
                lookupGate = gate
                ignoreLookupCancellation = true
            }
            val fixture = fixture(regionalCenters = centers)
            fixture.viewModel.initialize(OnboardingMode.FIRST_RUN, UserProfile())
            fixture.viewModel.continueFromCurrentStep()
            fixture.viewModel.onZipChanged("90001")
            fixture.viewModel.continueFromCurrentStep()
            runCurrent()
            assertEquals(CenterLookupState.LOADING, fixture.viewModel.uiState.value.centerLookupState)

            fixture.viewModel.onLocationPermissionResult(granted = false)

            assertEquals(OnboardingStep.ZIP, fixture.viewModel.uiState.value.step)
            assertEquals(LocationState.DENIED, fixture.viewModel.uiState.value.locationState)
            assertEquals(CenterLookupState.IDLE, fixture.viewModel.uiState.value.centerLookupState)
            assertEquals("90001", fixture.viewModel.uiState.value.draft.zipCode)
            assertNull(fixture.viewModel.uiState.value.draft.regionalCenter)
            assertTrue(fixture.viewModel.uiState.value.canContinue)

            gate.complete(Unit)
            runCurrent()

            assertEquals(OnboardingStep.ZIP, fixture.viewModel.uiState.value.step)
            assertEquals(LocationState.DENIED, fixture.viewModel.uiState.value.locationState)
            assertEquals(CenterLookupState.IDLE, fixture.viewModel.uiState.value.centerLookupState)
            assertEquals("90001", fixture.viewModel.uiState.value.draft.zipCode)
            assertNull(fixture.viewModel.uiState.value.draft.regionalCenter)
            assertTrue(fixture.viewModel.uiState.value.canContinue)
        }

    @Test
    fun permissionDenialAndLocationFailure_returnToZipWithoutAutomaticRelaunch() =
        runTest(mainDispatcherRule.testDispatcher) {
            val location = FakeUserLocationSource(permissionGranted = false, coordinates = null)
            val fixture = fixture(location = location)
            fixture.viewModel.initialize(OnboardingMode.FIRST_RUN, UserProfile())
            fixture.viewModel.continueFromCurrentStep()

            fixture.viewModel.onLocationPermissionResult(granted = false)
            runCurrent()

            assertEquals(OnboardingStep.ZIP, fixture.viewModel.uiState.value.step)
            assertEquals(LocationState.DENIED, fixture.viewModel.uiState.value.locationState)
            assertEquals(0, location.currentCoordinatesCalls)

            fixture.viewModel.onLocationPermissionResult(granted = true)
            runCurrent()
            assertEquals(LocationState.FAILED, fixture.viewModel.uiState.value.locationState)
            assertEquals(OnboardingStep.ZIP, fixture.viewModel.uiState.value.step)
            assertEquals(1, location.currentCoordinatesCalls)

            runCurrent()
            assertEquals(1, location.currentCoordinatesCalls)
        }

    @Test
    fun journeyIsRequiredAndSelectingItAgainDoesNotToggleItOff() =
        runTest(mainDispatcherRule.testDispatcher) {
            val fixture = fixture()
            advanceToJourney(fixture)

            assertFalse(fixture.viewModel.uiState.value.canContinue)
            fixture.viewModel.selectJourney(JourneyStage.EXPLORING)
            assertTrue(fixture.viewModel.uiState.value.canContinue)
            fixture.viewModel.selectJourney(JourneyStage.EXPLORING)
            assertEquals(JourneyStage.EXPLORING, fixture.viewModel.uiState.value.draft.journeyStage)
            assertTrue(fixture.viewModel.uiState.value.canContinue)
        }

    @Test
    fun selectedAge_canBeTappedAgainToClearIt() = runTest(mainDispatcherRule.testDispatcher) {
        val fixture = fixture()
        advanceToAge(fixture)

        fixture.viewModel.selectAgeGroup(AgeGroup.ADOLESCENT)
        assertEquals(AgeGroup.ADOLESCENT, fixture.viewModel.uiState.value.draft.ageGroup)
        fixture.viewModel.selectAgeGroup(AgeGroup.ADOLESCENT)
        assertNull(fixture.viewModel.uiState.value.draft.ageGroup)
        assertTrue(fixture.viewModel.uiState.value.canContinue)
    }

    @Test
    fun getStarted_replacesTheWholeCompletedProfileOnce() =
        runTest(mainDispatcherRule.testDispatcher) {
            val fixture = fixture()
            advanceToAge(fixture)
            fixture.viewModel.selectAgeGroup(AgeGroup.EARLY_INTERVENTION)

            fixture.viewModel.finish()
            runCurrent()

            assertEquals(1, fixture.repository.replacedProfiles.size)
            assertEquals(
                UserProfile(
                    onboardingCompleted = true,
                    audienceType = AudienceType.FAMILY,
                    zipCode = "90001",
                    regionalCenter = null,
                    journeyStage = JourneyStage.EXPLORING,
                    ageGroup = AgeGroup.EARLY_INTERVENTION
                ),
                fixture.repository.replacedProfiles.single()
            )
        }

    @Test
    fun editCancel_emitsCloseAndWritesNothing() = runTest(mainDispatcherRule.testDispatcher) {
        val fixture = fixture(repository = RecordingProfileRepository(completeProfile()))
        val events = mutableListOf<OnboardingEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            fixture.viewModel.events.collect(events::add)
        }
        fixture.viewModel.initialize(OnboardingMode.EDIT, completeProfile())
        fixture.viewModel.onZipChanged("90210")

        fixture.viewModel.cancel()
        runCurrent()

        assertEquals(listOf(OnboardingEvent.Close), events)
        assertTrue(fixture.repository.replacedProfiles.isEmpty())
    }

    @Test
    fun editSave_replacesAllFieldsAndClearsStaleCenterAfterUnmatchedZip() =
        runTest(mainDispatcherRule.testDispatcher) {
            val saved = completeProfile()
            val fixture = fixture(repository = RecordingProfileRepository(saved))
            fixture.viewModel.initialize(OnboardingMode.EDIT, saved)
            fixture.viewModel.continueFromCurrentStep()
            fixture.viewModel.onZipChanged("90002")
            fixture.viewModel.continueFromCurrentStep()
            runCurrent()
            fixture.viewModel.continueFromCurrentStep()
            fixture.viewModel.selectJourney(JourneyStage.RECEIVING_SERVICES)
            fixture.viewModel.continueFromCurrentStep()
            fixture.viewModel.selectAgeGroup(AgeGroup.ADULT)

            fixture.viewModel.finish()
            runCurrent()

            assertEquals(1, fixture.repository.replacedProfiles.size)
            assertEquals(
                UserProfile(
                    onboardingCompleted = true,
                    audienceType = AudienceType.FAMILY,
                    zipCode = "90002",
                    regionalCenter = null,
                    journeyStage = JourneyStage.RECEIVING_SERVICES,
                    ageGroup = AgeGroup.ADULT
                ),
                fixture.repository.replacedProfiles.single()
            )
        }

    @Test
    fun savedEvent_followsSuccessfulReplaceWhileFailureExposesOnlyRetryState() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = RecordingProfileRepository().apply {
                replaceFailure = IllegalStateException("private backend response")
            }
            val fixture = fixture(repository = repository)
            val events = mutableListOf<OnboardingEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                fixture.viewModel.events.collect(events::add)
            }
            advanceToAge(fixture)

            fixture.viewModel.finish()
            runCurrent()

            assertTrue(events.isEmpty())
            assertFalse(fixture.viewModel.uiState.value.isSaving)
            assertEquals(SaveError.RETRY, fixture.viewModel.uiState.value.saveError)
            assertEquals(OnboardingStep.AGE, fixture.viewModel.uiState.value.step)

            repository.replaceFailure = null
            fixture.viewModel.finish()
            runCurrent()

            assertEquals(1, repository.replacedProfiles.size)
            assertEquals(listOf(OnboardingEvent.Saved), events)
            assertNull(fixture.viewModel.uiState.value.saveError)
        }

    @Test
    fun suspendedSave_isSingleFlightAndCancelOrBackCannotRaceSaved() =
        runTest(mainDispatcherRule.testDispatcher) {
            val saveGate = CompletableDeferred<Unit>()
            val repository = RecordingProfileRepository().apply {
                replaceGate = saveGate
            }
            val fixture = fixture(repository = repository)
            val events = mutableListOf<OnboardingEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                fixture.viewModel.events.collect(events::add)
            }
            advanceToAge(fixture)

            fixture.viewModel.finish()
            fixture.viewModel.finish()
            runCurrent()
            assertTrue(fixture.viewModel.uiState.value.isSaving)
            assertEquals(1, repository.replaceAttempts.size)

            fixture.viewModel.cancel()
            fixture.viewModel.goBack()
            fixture.viewModel.selectAgeGroup(AgeGroup.ADULT)
            runCurrent()

            assertTrue(events.isEmpty())
            assertEquals(OnboardingStep.AGE, fixture.viewModel.uiState.value.step)
            assertNull(fixture.viewModel.uiState.value.draft.ageGroup)

            saveGate.complete(Unit)
            runCurrent()

            assertEquals(1, repository.replacedProfiles.size)
            assertEquals(listOf(OnboardingEvent.Saved), events)
            assertFalse(fixture.viewModel.uiState.value.isSaving)
        }

    private suspend fun kotlinx.coroutines.test.TestScope.advanceToCenter(fixture: Fixture) {
        fixture.viewModel.initialize(OnboardingMode.FIRST_RUN, UserProfile())
        fixture.viewModel.continueFromCurrentStep()
        fixture.viewModel.onZipChanged("90001")
        fixture.viewModel.continueFromCurrentStep()
        runCurrent()
    }

    private suspend fun kotlinx.coroutines.test.TestScope.advanceToJourney(fixture: Fixture) {
        advanceToCenter(fixture)
        fixture.viewModel.continueFromCurrentStep()
    }

    private suspend fun kotlinx.coroutines.test.TestScope.advanceToAge(fixture: Fixture) {
        advanceToJourney(fixture)
        fixture.viewModel.selectJourney(JourneyStage.EXPLORING)
        fixture.viewModel.continueFromCurrentStep()
    }

    private fun fixture(
        repository: RecordingProfileRepository = RecordingProfileRepository(),
        regionalCenters: FakeRegionalCenterDataSource = FakeRegionalCenterDataSource(),
        location: FakeUserLocationSource = FakeUserLocationSource()
    ): Fixture {
        val viewModel = OnboardingViewModel(repository, regionalCenters, location)
        return Fixture(viewModel, repository, regionalCenters, location)
    }

    private fun completeProfile(
        zipCode: String = "90001",
        ageGroup: AgeGroup = AgeGroup.SCHOOL_AGE
    ) = UserProfile(
        onboardingCompleted = true,
        audienceType = AudienceType.FAMILY,
        zipCode = zipCode,
        regionalCenter = RegionalCenterIdentity.from(regionalCenter()),
        journeyStage = JourneyStage.WAITING_FOR_INTAKE,
        ageGroup = ageGroup
    )

    private fun regionalCenter() = RegionalCenter(
        id = 7,
        name = "Westside Regional Center",
        address = "5901 Green Valley Circle",
        city = "Culver City",
        state = "CA",
        zipCode = "90230",
        telephone = "3105550100",
        website = "https://example.test",
        latitude = 33.98,
        longitude = -118.39,
        zipCodes = listOf("90001"),
        serviceAreas = listOf("West Los Angeles"),
        countyServed = "Los Angeles",
        distance = null
    )

    private data class Fixture(
        val viewModel: OnboardingViewModel,
        val repository: RecordingProfileRepository,
        val regionalCenters: FakeRegionalCenterDataSource,
        val location: FakeUserLocationSource
    )

    private class RecordingProfileRepository(
        initialProfile: UserProfile = UserProfile()
    ) : UserProfileRepository {
        private val profiles = MutableStateFlow(initialProfile)
        val replacedProfiles = mutableListOf<UserProfile>()
        val replaceAttempts = mutableListOf<UserProfile>()
        var replaceFailure: Throwable? = null
        var replaceGate: CompletableDeferred<Unit>? = null

        val currentProfile: UserProfile
            get() = profiles.value

        override val profile: Flow<UserProfile> = profiles

        override suspend fun replaceProfile(profile: UserProfile) {
            replaceAttempts += profile
            replaceGate?.await()
            replaceFailure?.let { throw it }
            replacedProfiles += profile
            profiles.value = profile
        }

        override suspend fun replaceProfileIfCurrent(
            expected: UserProfile,
            replacement: UserProfile
        ): Boolean {
            if (profiles.value != expected) return false
            replaceProfile(replacement)
            return true
        }

        override suspend fun clearProfile() = Unit
    }
}
