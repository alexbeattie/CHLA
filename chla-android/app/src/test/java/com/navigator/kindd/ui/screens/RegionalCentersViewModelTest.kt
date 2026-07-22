package com.navigator.kindd.ui.screens

import com.navigator.kindd.data.models.RegionalCenter
import com.navigator.kindd.data.profile.AgeGroup
import com.navigator.kindd.data.profile.AudienceType
import com.navigator.kindd.data.profile.JourneyStage
import com.navigator.kindd.data.profile.RegionalCenterIdentity
import com.navigator.kindd.data.profile.UserProfile
import com.navigator.kindd.data.profile.UserProfileRepository
import com.navigator.kindd.data.source.LookupFailure
import com.navigator.kindd.data.source.RegionalCenterDataSource
import com.navigator.kindd.data.source.RegionalCenterLookup
import com.navigator.kindd.testing.MainDispatcherRule
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegionalCentersViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun savedProfilePrefillsZip_andSevenCenterCatalogStillLoads() =
        runTest(mainDispatcherRule.testDispatcher) {
            val centers = (1..7).map { center(it, "Center $it") }
            val viewModel = RegionalCentersViewModel(
                RecordingProfileRepository(profile()),
                FakeCenterSource(centers = Result.success(centers))
            )
            runCurrent()

            assertEquals("90001", viewModel.uiState.value.zipDraft)
            assertEquals(centers, viewModel.uiState.value.centers)
        }

    @Test
    fun zipDraftKeepsFiveAsciiDigits_andSubmitUsesTypedLookup() =
        runTest(mainDispatcherRule.testDispatcher) {
            val source = FakeCenterSource()
            val viewModel = RegionalCentersViewModel(RecordingProfileRepository(profile()), source)
            runCurrent()

            viewModel.onZipChanged("a1٢2-34567")
            assertEquals("12345", viewModel.uiState.value.zipDraft)
            viewModel.submitZip()
            runCurrent()

            assertEquals(listOf("12345"), source.lookups)
        }

    @Test
    fun matchedLookupReplacesOnlyZipAndCenter_preservingFullProfile() =
        runTest(mainDispatcherRule.testDispatcher) {
            val original = profile()
            val matched = center(9, "Westside Regional Center")
            val repository = RecordingProfileRepository(original)
            val viewModel = RegionalCentersViewModel(
                repository,
                FakeCenterSource(lookup = RegionalCenterLookup.Matched(matched))
            )
            runCurrent()
            viewModel.onZipChanged("90210")

            viewModel.submitZip()
            runCurrent()

            assertEquals(
                original.copy(zipCode = "90210", regionalCenter = RegionalCenterIdentity.from(matched)),
                repository.replacements.single()
            )
            assertEquals(RegionalCentersLookupState.MATCHED, viewModel.uiState.value.lookupState)
        }

    @Test
    fun zipDraftChangeInvalidatesCancellationIgnoringLookup_beforeItCanPersist() =
        runTest(mainDispatcherRule.testDispatcher) {
            val oldMatch = CompletableDeferred<RegionalCenterLookup>()
            val source = ControlledLookupCenterSource(ArrayDeque(listOf(oldMatch)))
            val original = profile()
            val repository = RecordingProfileRepository(original)
            val viewModel = RegionalCentersViewModel(repository, source)
            runCurrent()

            viewModel.onZipChanged("90001")
            viewModel.submitZip()
            runCurrent()
            viewModel.onZipChanged("90210")

            assertEquals("90210", viewModel.uiState.value.zipDraft)
            assertEquals(RegionalCentersLookupState.IDLE, viewModel.uiState.value.lookupState)

            oldMatch.complete(RegionalCenterLookup.Matched(center(1, "Old")))
            runCurrent()

            assertEquals(original, repository.current)
            assertTrue(repository.replacements.isEmpty())
            assertEquals("90210", viewModel.uiState.value.zipDraft)
            assertEquals(RegionalCentersLookupState.IDLE, viewModel.uiState.value.lookupState)
        }

    @Test
    fun everyZipSubmitSupersedesCancellationIgnoringLookup_whenNewMatchFinishesFirst() =
        runTest(mainDispatcherRule.testDispatcher) {
            val oldMatch = CompletableDeferred<RegionalCenterLookup>()
            val newMatch = CompletableDeferred<RegionalCenterLookup>()
            val source = ControlledLookupCenterSource(ArrayDeque(listOf(oldMatch, newMatch)))
            val original = profile()
            val repository = RecordingProfileRepository(original)
            val viewModel = RegionalCentersViewModel(repository, source)
            runCurrent()

            viewModel.onZipChanged("90210")
            viewModel.submitZip()
            runCurrent()
            viewModel.submitZip()
            runCurrent()

            val expectedCenter = center(3, "New")
            newMatch.complete(RegionalCenterLookup.Matched(expectedCenter))
            runCurrent()
            oldMatch.complete(RegionalCenterLookup.Matched(center(2, "Old")))
            runCurrent()

            val expectedProfile = original.copy(
                zipCode = "90210",
                regionalCenter = RegionalCenterIdentity.from(expectedCenter)
            )
            assertEquals(expectedProfile, repository.current)
            assertEquals(listOf(expectedProfile), repository.replacements)
            assertEquals(RegionalCentersLookupState.MATCHED, viewModel.uiState.value.lookupState)
            assertEquals(expectedCenter, viewModel.uiState.value.matchedCenter)
        }

    @Test
    fun externalProfileChangeSupersedesCancellationIgnoringLookup_beforeItCanPersist() =
        runTest(mainDispatcherRule.testDispatcher) {
            val pendingMatch = CompletableDeferred<RegionalCenterLookup>()
            val source = ControlledLookupCenterSource(ArrayDeque(listOf(pendingMatch)))
            val original = profile()
            val externallyEdited = original.copy(
                audienceType = AudienceType.FAMILY,
                zipCode = "91311",
                regionalCenter = RegionalCenterIdentity(91, "Externally edited", "EXTERNAL"),
                journeyStage = JourneyStage.EXPLORING,
                ageGroup = AgeGroup.EARLY_INTERVENTION
            )
            val repository = RecordingProfileRepository(original)
            val viewModel = RegionalCentersViewModel(repository, source)
            runCurrent()

            viewModel.onZipChanged("90210")
            viewModel.submitZip()
            runCurrent()
            repository.emit(externallyEdited)
            runCurrent()

            pendingMatch.complete(RegionalCenterLookup.Matched(center(22, "Late")))
            runCurrent()

            assertEquals(externallyEdited, repository.current)
            assertTrue(repository.replacements.isEmpty())
            assertEquals("91311", viewModel.uiState.value.zipDraft)
            assertEquals(RegionalCentersLookupState.IDLE, viewModel.uiState.value.lookupState)
        }

    @Test
    fun committedProfileChangeNotYetObservedByFlow_cannotBeOverwrittenByLateLookup() =
        runTest(mainDispatcherRule.testDispatcher) {
            val pendingMatch = CompletableDeferred<RegionalCenterLookup>()
            val source = ControlledLookupCenterSource(ArrayDeque(listOf(pendingMatch)))
            val original = profile()
            val externallyEdited = original.copy(
                audienceType = AudienceType.FAMILY,
                zipCode = "91311",
                regionalCenter = RegionalCenterIdentity(91, "Externally edited", "WRC"),
                journeyStage = JourneyStage.EXPLORING,
                ageGroup = AgeGroup.EARLY_INTERVENTION
            )
            val repository = LaggingProfileRepository(original)
            val viewModel = RegionalCentersViewModel(repository, source)
            runCurrent()

            viewModel.onZipChanged("90210")
            viewModel.submitZip()
            runCurrent()
            repository.commitWithoutFlowEmission(externallyEdited)

            pendingMatch.complete(RegionalCenterLookup.Matched(center(22, "Late")))
            runCurrent()

            assertEquals(externallyEdited, repository.actualProfile)
            assertTrue(repository.unconditionalReplacements.isEmpty())
            assertEquals(RegionalCentersLookupState.IDLE, viewModel.uiState.value.lookupState)
        }

    @Test
    fun lookupOrProfileWriteFailure_exposesSanitizedRetryableState_andKeepsProfile() =
        runTest(mainDispatcherRule.testDispatcher) {
            val original = profile()
            val lookupRepository = RecordingProfileRepository(original)
            val lookupViewModel = RegionalCentersViewModel(
                lookupRepository,
                FakeCenterSource(lookupFailure = IllegalStateException("private lookup body 90001"))
            )
            runCurrent()
            lookupViewModel.onZipChanged("90210")
            lookupViewModel.submitZip()
            runCurrent()

            assertEquals(original, lookupRepository.current)
            assertEquals(RegionalCentersLookupState.UNAVAILABLE, lookupViewModel.uiState.value.lookupState)
            assertEquals(RegionalCentersMessage.LOOKUP_UNAVAILABLE, lookupViewModel.uiState.value.message)
            assertTrue(lookupViewModel.uiState.value.message.toString().contains("private").not())

            val writeRepository = RecordingProfileRepository(original).apply {
                replaceFailure = IllegalStateException("private write body 90210")
            }
            val writeViewModel = RegionalCentersViewModel(
                writeRepository,
                FakeCenterSource(lookup = RegionalCenterLookup.Matched(center(9, "Matched")))
            )
            runCurrent()
            writeViewModel.onZipChanged("90210")
            writeViewModel.submitZip()
            runCurrent()

            assertEquals(original, writeRepository.current)
            assertTrue(writeRepository.replacements.isEmpty())
            assertEquals(RegionalCentersLookupState.UNAVAILABLE, writeViewModel.uiState.value.lookupState)
            assertEquals(RegionalCentersMessage.LOOKUP_UNAVAILABLE, writeViewModel.uiState.value.message)
            assertTrue(writeViewModel.uiState.value.message.toString().contains("private").not())
        }

    @Test
    fun unmatchedAndUnavailableChangeOnlySanitizedScreenState() =
        runTest(mainDispatcherRule.testDispatcher) {
            listOf(
                RegionalCenterLookup.Unmatched to RegionalCentersLookupState.UNMATCHED,
                RegionalCenterLookup.Unavailable(LookupFailure.SERVER) to RegionalCentersLookupState.UNAVAILABLE
            ).forEach { (lookup, expected) ->
                val original = profile()
                val repository = RecordingProfileRepository(original)
                val viewModel = RegionalCentersViewModel(repository, FakeCenterSource(lookup = lookup))
                runCurrent()
                viewModel.onZipChanged("90210")
                viewModel.submitZip()
                runCurrent()

                assertEquals(original, repository.current)
                assertTrue(repository.replacements.isEmpty())
                assertEquals(expected, viewModel.uiState.value.lookupState)
                assertTrue(viewModel.uiState.value.message.toString().contains("90210").not())
            }
        }

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

    private class FakeCenterSource(
        private val centers: Result<List<RegionalCenter>> = Result.success(emptyList()),
        private val lookup: RegionalCenterLookup = RegionalCenterLookup.Unmatched,
        private val lookupFailure: Throwable? = null
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

    private class ControlledLookupCenterSource(
        private val gates: ArrayDeque<CompletableDeferred<RegionalCenterLookup>>
    ) : RegionalCenterDataSource {
        override suspend fun getRegionalCenters() = Result.success(emptyList<RegionalCenter>())
        override suspend fun getRegionalCentersNearby(latitude: Double, longitude: Double) =
            Result.success(emptyList<RegionalCenter>())
        override suspend fun lookupRegionalCenter(zipCode: String): RegionalCenterLookup =
            withContext(NonCancellable) { gates.removeFirst().await() }
    }

    private fun profile() = UserProfile(
        onboardingCompleted = true,
        audienceType = AudienceType.CLINICIAN,
        zipCode = "90001",
        regionalCenter = RegionalCenterIdentity(1, "Old center", "OLD"),
        journeyStage = JourneyStage.RECEIVING_SERVICES,
        ageGroup = AgeGroup.ADULT
    )

    private fun center(id: Int, name: String) = RegionalCenter(
        id = id,
        name = name,
        countyServed = "Los Angeles"
    )
}
