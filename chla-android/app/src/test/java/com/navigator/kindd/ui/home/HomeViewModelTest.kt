package com.navigator.kindd.ui.home

import com.navigator.kindd.data.discovery.FakeDiscoveryController
import com.navigator.kindd.data.discovery.TherapyType
import com.navigator.kindd.data.models.RegionalCenter
import com.navigator.kindd.data.profile.AgeGroup
import com.navigator.kindd.data.profile.AudienceType
import com.navigator.kindd.data.profile.JourneyStage
import com.navigator.kindd.data.profile.RegionalCenterIdentity
import com.navigator.kindd.data.profile.UserProfile
import com.navigator.kindd.data.profile.UserProfileRepository
import com.navigator.kindd.data.servicearea.ServiceAreaCoordinate
import com.navigator.kindd.data.servicearea.ServiceAreaDataSource
import com.navigator.kindd.data.servicearea.ServiceAreaFeature
import com.navigator.kindd.data.source.LookupFailure
import com.navigator.kindd.data.source.RegionalCenterDataSource
import com.navigator.kindd.data.source.RegionalCenterLookup
import com.navigator.kindd.testing.MainDispatcherRule
import com.navigator.kindd.ui.chat.ChatLaunchPrompt
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
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
    fun bundledServiceAreasLoadIntoHomeWithoutChangingTheAuthoritativeCenter() =
        runTest(mainDispatcherRule.testDispatcher) {
            val ready = profile(
                zip = "90001",
                identity = RegionalCenterIdentity(7, "South Central Los Angeles Regional Center", "SCLARC")
            )
            val area = serviceArea("SCLARC")
            val viewModel = HomeViewModel(
                RecordingProfileRepository(ready),
                CenterSource(centers = Result.success(listOf(center(id = 7)))),
                FakeDiscoveryController(),
                FixedServiceAreaSource(Result.success(listOf(area)))
            )

            viewModel.onReadyProfileChanged(ready)
            runCurrent()

            assertEquals(ServiceAreaLoadState.READY, viewModel.uiState.value.serviceAreaLoadState)
            assertEquals(listOf(area), viewModel.uiState.value.serviceAreas)
            assertEquals(ready.regionalCenter, viewModel.uiState.value.hydratedIdentity)
            assertEquals(7, viewModel.uiState.value.hydratedCenter?.id)
        }

    @Test
    fun serviceAreaFailureLeavesProfileAndCenterHydrationIntact() =
        runTest(mainDispatcherRule.testDispatcher) {
            val identity = RegionalCenterIdentity(7, "South Central Los Angeles Regional Center", "SCLARC")
            val ready = profile(zip = "90001", identity = identity)
            val viewModel = HomeViewModel(
                RecordingProfileRepository(ready),
                CenterSource(centers = Result.success(listOf(center(id = 7)))),
                FakeDiscoveryController(),
                FixedServiceAreaSource(Result.failure(IllegalStateException("offline")))
            )

            viewModel.onReadyProfileChanged(ready)
            runCurrent()

            assertEquals(ServiceAreaLoadState.FAILED, viewModel.uiState.value.serviceAreaLoadState)
            assertTrue(viewModel.uiState.value.serviceAreas.isEmpty())
            assertEquals(identity, viewModel.uiState.value.hydratedIdentity)
            assertEquals(7, viewModel.uiState.value.hydratedCenter?.id)
        }

    @Test
    fun constructionDoesNotCollectASecondAuthoritativeProfileFlow() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = CountingProfileRepository(profile())

            HomeViewModel(repository, CenterSource(), FakeDiscoveryController())
            runCurrent()

            assertEquals(0, repository.collectionCount)
        }

    @Test
    fun findBeforeAnyProfileSyncUsesReadyZip_andPreservesTheCompleteReadyProfile() =
        runTest(mainDispatcherRule.testDispatcher) {
            val ready = profile(
                zip = "90001",
                identity = null,
                audience = AudienceType.CLINICIAN,
                journey = JourneyStage.RECEIVING_SERVICES,
                age = AgeGroup.ADULT
            )
            val matched = center(id = 72, name = "South Central Los Angeles Regional Center")
            val repository = RecordingProfileRepository(ready)
            val source = CenterSource(lookup = RegionalCenterLookup.Matched(matched))
            val viewModel = HomeViewModel(repository, source, FakeDiscoveryController())

            viewModel.submitZip(ready, ready.zipCode.orEmpty())
            runCurrent()

            assertEquals(listOf("90001"), source.lookups)
            assertEquals(
                ready.copy(regionalCenter = RegionalCenterIdentity.from(matched)),
                repository.current
            )
            assertFalse(viewModel.uiState.value.isZipDraftDirty)
            assertEquals(HomeLookupState.MATCHED, viewModel.uiState.value.lookupState)
        }

    @Test
    fun earlyTypedZipDraftSurvivesProfileSynchronization() =
        runTest(mainDispatcherRule.testDispatcher) {
            val ready = profile(zip = "90001", identity = null)
            val viewModel = HomeViewModel(
                RecordingProfileRepository(ready),
                CenterSource(),
                FakeDiscoveryController()
            )

            viewModel.onZipChanged("91311")
            viewModel.onReadyProfileChanged(
                ready.copy(
                    regionalCenter = RegionalCenterIdentity(7, "Synced center", "SYNC")
                )
            )
            runCurrent()

            assertEquals("91311", viewModel.uiState.value.displayedZip(ready))
            assertTrue(viewModel.uiState.value.isZipDraftDirty)
        }

    @Test
    fun repeatedReadyProfileWithSameIdentityDoesNotDuplicateHydration() =
        runTest(mainDispatcherRule.testDispatcher) {
            val identity = RegionalCenterIdentity(7, "Saved center", "SAVED")
            val source = CenterSource(centers = Result.success(listOf(center(id = 7))))
            val viewModel = HomeViewModel(
                RecordingProfileRepository(profile(identity = identity)),
                source,
                FakeDiscoveryController()
            )

            viewModel.onReadyProfileChanged(profile(identity = identity))
            viewModel.onReadyProfileChanged(
                profile(identity = identity, journey = JourneyStage.RECEIVING_SERVICES)
            )
            runCurrent()

            assertEquals(1, source.centerRequests)
        }

    @Test
    fun distinctReadyUpdateWithSameIdentityInvalidatesCancellationIgnoringLookup() =
        runTest(mainDispatcherRule.testDispatcher) {
            val pending = CompletableDeferred<RegionalCenterLookup>()
            val source = ControlledLookupCenterSource(ArrayDeque(listOf(pending)))
            val original = profile(zip = "90001", identity = null)
            val updated = original.copy(
                zipCode = "91311",
                journeyStage = JourneyStage.RECEIVING_SERVICES
            )
            val viewModel = HomeViewModel(
                RecordingProfileRepository(original),
                source,
                FakeDiscoveryController()
            )
            viewModel.onReadyProfileChanged(original)
            viewModel.onZipChanged("90210")
            viewModel.submitZip(original, viewModel.uiState.value.displayedZip(original))
            runCurrent()
            assertEquals(HomeLookupState.LOADING, viewModel.uiState.value.lookupState)

            viewModel.onReadyProfileChanged(updated)

            assertEquals(HomeLookupState.IDLE, viewModel.uiState.value.lookupState)
            assertNull(viewModel.uiState.value.message)
            assertEquals("90210", viewModel.uiState.value.displayedZip(updated))
            assertTrue(viewModel.uiState.value.isZipDraftDirty)

            pending.complete(RegionalCenterLookup.Unmatched)
            runCurrent()

            assertEquals(HomeLookupState.IDLE, viewModel.uiState.value.lookupState)
            assertNull(viewModel.uiState.value.message)
        }

    @Test
    fun distinctReadyUpdateWithSameIdentityClearsPriorLookupError() =
        runTest(mainDispatcherRule.testDispatcher) {
            val original = profile(zip = "90001", identity = null)
            val viewModel = HomeViewModel(
                RecordingProfileRepository(original),
                CenterSource(lookup = RegionalCenterLookup.Unmatched),
                FakeDiscoveryController()
            )
            viewModel.onReadyProfileChanged(original)
            viewModel.onZipChanged("90210")
            viewModel.submitZip(original, viewModel.uiState.value.displayedZip(original))
            runCurrent()
            assertEquals(HomeMessage.NO_MATCH, viewModel.uiState.value.message)

            viewModel.onReadyProfileChanged(
                original.copy(audienceType = AudienceType.CLINICIAN)
            )

            assertEquals(HomeLookupState.IDLE, viewModel.uiState.value.lookupState)
            assertNull(viewModel.uiState.value.message)
            assertEquals("90210", viewModel.uiState.value.zipDraft)
        }

    @Test
    fun exactRepeatedReadyProfileDoesNotInvalidateLookup() =
        runTest(mainDispatcherRule.testDispatcher) {
            val pending = CompletableDeferred<RegionalCenterLookup>()
            val source = ControlledLookupCenterSource(ArrayDeque(listOf(pending)))
            val ready = profile(zip = "90001", identity = null)
            val viewModel = HomeViewModel(
                RecordingProfileRepository(ready),
                source,
                FakeDiscoveryController()
            )
            viewModel.onReadyProfileChanged(ready)
            viewModel.onZipChanged("90210")
            viewModel.submitZip(ready, viewModel.uiState.value.displayedZip(ready))
            runCurrent()

            viewModel.onReadyProfileChanged(ready)
            pending.complete(RegionalCenterLookup.Unmatched)
            runCurrent()

            assertEquals(HomeLookupState.UNMATCHED, viewModel.uiState.value.lookupState)
            assertEquals(HomeMessage.NO_MATCH, viewModel.uiState.value.message)
        }

    @Test
    fun selfAuthoredRootCallbackBeforeCasReturnKeepsGeneration_andCompletesMatchedState() =
        runTest(mainDispatcherRule.testDispatcher) {
            val original = profile(zip = "90001", identity = null)
            val matched = center(id = 72, name = "Matched center")
            val repository = OrderedCasRepository(original, CasBehavior.TRUE)
            val viewModel = HomeViewModel(
                repository,
                CenterSource(
                    centers = Result.success(listOf(matched)),
                    lookup = RegionalCenterLookup.Matched(matched)
                ),
                FakeDiscoveryController()
            )
            viewModel.onReadyProfileChanged(original)
            repository.beforeCasReturn = viewModel::onReadyProfileChanged
            viewModel.onZipChanged("90210")

            viewModel.submitZip(original, viewModel.uiState.value.displayedZip(original))
            runCurrent()

            val replacement = original.copy(
                zipCode = "90210",
                regionalCenter = RegionalCenterIdentity.from(matched)
            )
            assertEquals(replacement, repository.actualProfile)
            assertEquals(HomeLookupState.MATCHED, viewModel.uiState.value.lookupState)
            assertFalse(viewModel.uiState.value.isZipDraftDirty)
            assertEquals("90210", viewModel.uiState.value.displayedZip(replacement))
            assertEquals(replacement.regionalCenter, viewModel.uiState.value.hydratedIdentity)

            viewModel.onReadyProfileChanged(
                replacement.copy(journeyStage = JourneyStage.RECEIVING_SERVICES)
            )

            assertEquals(HomeLookupState.IDLE, viewModel.uiState.value.lookupState)
        }

    @Test
    fun selfAuthoredRootCallbackAfterCasReturnDoesNotClearMatchedState() =
        runTest(mainDispatcherRule.testDispatcher) {
            val original = profile(zip = "90001", identity = null)
            val matched = center(id = 72, name = "Matched center")
            val repository = OrderedCasRepository(original, CasBehavior.TRUE)
            val viewModel = HomeViewModel(
                repository,
                CenterSource(lookup = RegionalCenterLookup.Matched(matched)),
                FakeDiscoveryController()
            )
            viewModel.onReadyProfileChanged(original)
            viewModel.onZipChanged("90210")

            viewModel.submitZip(original, viewModel.uiState.value.displayedZip(original))
            runCurrent()
            assertEquals(HomeLookupState.MATCHED, viewModel.uiState.value.lookupState)
            assertFalse(viewModel.uiState.value.isZipDraftDirty)

            viewModel.onReadyProfileChanged(repository.actualProfile)
            runCurrent()

            assertEquals(HomeLookupState.MATCHED, viewModel.uiState.value.lookupState)
            assertFalse(viewModel.uiState.value.isZipDraftDirty)
        }

    @Test
    fun failedOrThrowingCasDoesNotLeaveAReplacementTokenThatSuppressesExternalRoot() =
        runTest(mainDispatcherRule.testDispatcher) {
            listOf(CasBehavior.FALSE, CasBehavior.THROW).forEach { behavior ->
                val original = profile(zip = "90001", identity = null)
                val matched = center(id = 72, name = "Matched center")
                val firstLookup = CompletableDeferred<RegionalCenterLookup>().apply {
                    complete(RegionalCenterLookup.Matched(matched))
                }
                val secondLookup = CompletableDeferred<RegionalCenterLookup>()
                val repository = OrderedCasRepository(original, behavior)
                val viewModel = HomeViewModel(
                    repository,
                    ControlledLookupCenterSource(ArrayDeque(listOf(firstLookup, secondLookup))),
                    FakeDiscoveryController()
                )
                viewModel.onReadyProfileChanged(original)
                viewModel.onZipChanged("90210")
                viewModel.submitZip(original, viewModel.uiState.value.displayedZip(original))
                runCurrent()

                viewModel.onZipChanged("91311")
                viewModel.submitZip(original, viewModel.uiState.value.displayedZip(original))
                runCurrent()
                assertEquals(HomeLookupState.LOADING, viewModel.uiState.value.lookupState)

                val externalProfile = original.copy(
                    zipCode = "90210",
                    regionalCenter = RegionalCenterIdentity.from(matched)
                )
                viewModel.onReadyProfileChanged(externalProfile)

                assertEquals(HomeLookupState.IDLE, viewModel.uiState.value.lookupState)
                secondLookup.complete(RegionalCenterLookup.Unmatched)
                runCurrent()
                assertEquals(HomeLookupState.IDLE, viewModel.uiState.value.lookupState)
            }
        }

    @Test
    fun readyProfileHydratesCenterByDeployedId_withoutOwningProfilePresentation() =
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

            assertEquals(profile.regionalCenter, fixture.viewModel.uiState.value.hydratedIdentity)
            assertEquals(details, fixture.viewModel.uiState.value.hydratedCenter)
        }

    @Test
    fun zipLookupRequiresExactlyFiveAsciiDigits() = runTest(mainDispatcherRule.testDispatcher) {
        val fixture = fixture(profile())

        fixture.viewModel.onZipChanged("12a3٤4")
        assertEquals("1234", fixture.viewModel.uiState.value.zipDraft)
        fixture.viewModel.submitZip(profile(), fixture.viewModel.uiState.value.displayedZip(profile()))
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

            fixture.viewModel.submitZip(original, fixture.viewModel.uiState.value.displayedZip(original))
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
            viewModel.submitZip(original, viewModel.uiState.value.displayedZip(original))
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
            viewModel.submitZip(original, viewModel.uiState.value.displayedZip(original))
            runCurrent()
            viewModel.submitZip(original, viewModel.uiState.value.displayedZip(original))
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
            viewModel.submitZip(original, viewModel.uiState.value.displayedZip(original))
            runCurrent()
            repository.emit(externallyEdited)
            viewModel.onReadyProfileChanged(externallyEdited)
            runCurrent()

            pendingMatch.complete(RegionalCenterLookup.Matched(center(id = 22, name = "Late")))
            runCurrent()

            assertEquals(externallyEdited, repository.current)
            assertTrue(repository.replacements.isEmpty())
            assertEquals("90210", viewModel.uiState.value.zipDraft)
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
            viewModel.submitZip(original, viewModel.uiState.value.displayedZip(original))
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
            lookupFailureFixture.viewModel.submitZip(
                original,
                lookupFailureFixture.viewModel.uiState.value.displayedZip(original)
            )
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
            writeFailureFixture.viewModel.submitZip(
                original,
                writeFailureFixture.viewModel.uiState.value.displayedZip(original)
            )
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
                fixture.viewModel.submitZip(
                    original,
                    fixture.viewModel.uiState.value.displayedZip(original)
                )
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

            val dialDigits = fixture.viewModel.uiState.value.dialDigitsFor(fixture.repository.current)
            requireNotNull(dialDigits)
            fixture.viewModel.callCenter(fixture.repository.current, dialDigits)
            runCurrent()
            assertEquals(HomeEvent.Dial("1213555126"), events.single())

            fixture.repository.emit(
                profile(identity = identity.copy(id = 99, name = "Unknown", shortName = "UNKNOWN"))
            )
            fixture.viewModel.onReadyProfileChanged(fixture.repository.current)
            runCurrent()
            fixture.viewModel.uiState.value.dialDigitsFor(fixture.repository.current)?.let { digits ->
                fixture.viewModel.callCenter(fixture.repository.current, digits)
            }
            fixture.viewModel.callCenter(fixture.repository.current, "+1 (213) 555-1212")
            fixture.viewModel.callCenter(fixture.repository.current, "١٢٣")
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
            assertEquals(failedProfile.regionalCenter, failed.viewModel.uiState.value.hydratedIdentity)
            assertNull(failed.viewModel.uiState.value.hydratedCenter)
            assertEquals(
                "3102584000",
                failed.viewModel.uiState.value.dialDigitsFor(failedProfile)
            )
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
            val capturedInitialDigits = fixture.viewModel.uiState.value.dialDigitsFor(fixture.repository.current)
            requireNotNull(capturedInitialDigits)
            fixture.viewModel.callCenter(fixture.repository.current, capturedInitialDigits)
            runCurrent()
            assertEquals(HomeEvent.Dial("1111111111"), events.single())
            events.clear()

            fixture.repository.emit(profile(identity = staleIdentity))
            fixture.viewModel.onReadyProfileChanged(fixture.repository.current)
            runCurrent()
            assertNull(fixture.viewModel.uiState.value.hydratedCenter)
            fixture.viewModel.callCenter(fixture.repository.current, capturedInitialDigits)
            runCurrent()
            assertTrue(events.isEmpty())

            fixture.repository.emit(profile(identity = latestIdentity))
            fixture.viewModel.onReadyProfileChanged(fixture.repository.current)
            runCurrent()
            val latestDetails = center(id = 3, name = "Latest", phone = "333-333-3333")
            latestGate.complete(Result.success(listOf(latestDetails)))
            runCurrent()
            assertEquals(latestDetails, fixture.viewModel.uiState.value.hydratedCenter)
            fixture.viewModel.callCenter(fixture.repository.current, capturedInitialDigits)
            runCurrent()
            assertTrue(events.isEmpty())

            staleGate.complete(
                Result.success(listOf(center(id = 2, name = "Stale", phone = "222-222-2222")))
            )
            runCurrent()

            assertEquals(latestDetails, fixture.viewModel.uiState.value.hydratedCenter)
            fixture.viewModel.callCenter(fixture.repository.current, capturedInitialDigits)
            runCurrent()
            assertTrue(events.isEmpty())
            val dialDigits = fixture.viewModel.uiState.value.dialDigitsFor(fixture.repository.current)
            requireNotNull(dialDigits)
            fixture.viewModel.callCenter(fixture.repository.current, dialDigits)
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
        val viewModel = HomeViewModel(repository, centers, discovery)
        viewModel.onReadyProfileChanged(initial)
        return Fixture(
            viewModel,
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

    private class CountingProfileRepository(initial: UserProfile) : UserProfileRepository {
        var collectionCount = 0
            private set
        override val profile: Flow<UserProfile> = flow {
            collectionCount += 1
            emit(initial)
        }

        override suspend fun replaceProfile(profile: UserProfile) = Unit

        override suspend fun replaceProfileIfCurrent(
            expected: UserProfile,
            replacement: UserProfile
        ): Boolean = false

        override suspend fun clearProfile() = Unit
    }

    private enum class CasBehavior { TRUE, FALSE, THROW }

    private class OrderedCasRepository(
        initial: UserProfile,
        private val behavior: CasBehavior
    ) : UserProfileRepository {
        private val profiles = MutableStateFlow(initial)
        var actualProfile: UserProfile = initial
            private set
        var beforeCasReturn: ((UserProfile) -> Unit)? = null

        override val profile: Flow<UserProfile> = profiles

        override suspend fun replaceProfile(profile: UserProfile) {
            actualProfile = profile
            profiles.value = profile
        }

        override suspend fun replaceProfileIfCurrent(
            expected: UserProfile,
            replacement: UserProfile
        ): Boolean {
            return when (behavior) {
                CasBehavior.FALSE -> false
                CasBehavior.THROW -> throw IllegalStateException("private CAS failure")
                CasBehavior.TRUE -> {
                    if (actualProfile != expected) return false
                    actualProfile = replacement
                    profiles.value = replacement
                    beforeCasReturn?.invoke(replacement)
                    true
                }
            }
        }

        override suspend fun clearProfile() {
            actualProfile = UserProfile()
            profiles.value = actualProfile
        }
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
        var centerRequests = 0
            private set
        override suspend fun getRegionalCenters(): Result<List<RegionalCenter>> {
            centerRequests += 1
            return centers
        }
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

    private class FixedServiceAreaSource(
        private val result: Result<List<ServiceAreaFeature>>
    ) : ServiceAreaDataSource {
        override suspend fun getServiceAreas(): Result<List<ServiceAreaFeature>> = result
    }

    private fun serviceArea(acronym: String) = ServiceAreaFeature(
        id = 1,
        name = "South Central Los Angeles Regional Center",
        acronym = acronym,
        description = "",
        polygons = listOf(
            listOf(
                ServiceAreaCoordinate(33.8, -118.3),
                ServiceAreaCoordinate(33.9, -118.2),
                ServiceAreaCoordinate(33.7, -118.1)
            )
        )
    )

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
