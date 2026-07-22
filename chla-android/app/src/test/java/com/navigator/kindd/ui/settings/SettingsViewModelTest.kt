package com.navigator.kindd.ui.settings

import com.navigator.kindd.data.discovery.DiscoveryController
import com.navigator.kindd.data.discovery.DiscoveryCriteria
import com.navigator.kindd.data.discovery.DiscoveryState
import com.navigator.kindd.data.discovery.TherapyType
import com.navigator.kindd.data.profile.AgeGroup
import com.navigator.kindd.data.profile.AudienceType
import com.navigator.kindd.data.profile.JourneyStage
import com.navigator.kindd.data.profile.UserProfile
import com.navigator.kindd.data.profile.UserProfileRepository
import com.navigator.kindd.testing.MainDispatcherRule
import com.navigator.kindd.ui.discovery.DiscoveryFilterSelection
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun editProfile_emitsOneShotNavigationEvent_withoutWritingProfile() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = RecordingProfileRepository()
            val viewModel = settingsViewModel(repository)
            val events = mutableListOf<SettingsEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.events.collect(events::add)
            }

            viewModel.editProfile()
            runCurrent()

            assertEquals(listOf(SettingsEvent.NavigateToEditProfile), events)
            assertTrue(repository.replacements.isEmpty())
            assertEquals(0, repository.clearCount)
        }

    @Test
    fun clearProfile_doesNothingBeforeConfirmation() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = RecordingProfileRepository()
            settingsViewModel(repository)

            runCurrent()

            assertEquals(0, repository.clearCount)
            assertTrue(repository.replacements.isEmpty())
        }

    @Test
    fun confirmedClear_clearsExactlyOnce_withoutManualNavigation() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = RecordingProfileRepository()
            val viewModel = settingsViewModel(repository)
            val events = mutableListOf<SettingsEvent>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.events.collect(events::add)
            }

            viewModel.clearProfile()
            runCurrent()

            assertEquals(1, repository.clearCount)
            assertTrue(repository.replacements.isEmpty())
            assertTrue(events.isEmpty())
        }

    @Test
    fun confirmedClear_isSingleFlightWhileRepositoryWriteIsPending() =
        runTest(mainDispatcherRule.testDispatcher) {
            val gate = CompletableDeferred<Unit>()
            val repository = RecordingProfileRepository().apply { clearGate = gate }
            val viewModel = settingsViewModel(repository)

            viewModel.clearProfile()
            viewModel.clearProfile()
            runCurrent()

            assertEquals(1, repository.clearCount)

            gate.complete(Unit)
            runCurrent()
        }

    @Test
    fun failedClear_emitsOnlySanitizedFailure_allowsRetry_andCancellationIsNotFailure() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = RecordingProfileRepository().apply {
                clearFailure = IllegalStateException("private profile contents")
            }
            val viewModel = settingsViewModel(repository)
            val eventNames = mutableListOf<String?>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.events.collect { eventNames += it::class.simpleName }
            }

            viewModel.clearProfile()
            runCurrent()

            assertEquals(listOf("ClearFailed"), eventNames)
            assertTrue(eventNames.none { it?.contains("private") == true })

            repository.clearFailure = null
            viewModel.clearProfile()
            runCurrent()
            assertEquals(2, repository.clearCount)

            repository.clearFailure = CancellationException("cancelled")
            viewModel.clearProfile()
            runCurrent()
            assertEquals(listOf("ClearFailed"), eventNames)

            repository.clearFailure = null
            viewModel.clearProfile()
            runCurrent()
            assertEquals(4, repository.clearCount)
        }

    @Test
    fun appModeSelection_atomicallyUpdatesTheCurrentSavedProfile() =
        runTest(mainDispatcherRule.testDispatcher) {
            val initial = completeProfile(audienceType = AudienceType.FAMILY)
            val repository = RecordingProfileRepository(initial)
            val viewModel = settingsViewModel(repository)
            runCurrent()

            viewModel.updateAppMode(AudienceType.CLINICIAN)
            runCurrent()

            assertEquals(
                listOf(initial.copy(audienceType = AudienceType.CLINICIAN)),
                repository.replacements
            )
        }

    @Test
    fun failedAppModeWrite_emitsOnlySanitizedFailure_andAllowsRetry() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = RecordingProfileRepository(completeProfile()).apply {
                replaceFailure = IllegalStateException("private profile contents")
            }
            val viewModel = settingsViewModel(repository)
            val eventNames = mutableListOf<String?>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.events.collect { eventNames += it::class.simpleName }
            }
            runCurrent()

            viewModel.updateAppMode(AudienceType.CLINICIAN)
            runCurrent()

            assertEquals(listOf("PreferenceUpdateFailed"), eventNames)
            assertTrue(eventNames.none { it?.contains("private") == true })

            repository.replaceFailure = null
            viewModel.updateAppMode(AudienceType.CLINICIAN)
            runCurrent()
            assertEquals(AudienceType.CLINICIAN, repository.replacements.single().audienceType)
        }

    @Test
    fun searchFiltersAndDefaultRadius_updateTheSharedDiscoveryController() =
        runTest(mainDispatcherRule.testDispatcher) {
            val controller = RecordingDiscoveryController()
            val viewModel = settingsViewModel(
                repository = RecordingProfileRepository(completeProfile()),
                discoveryController = controller
            )
            val selection = DiscoveryFilterSelection(
                therapyTypes = setOf(TherapyType.SPEECH),
                ageGroup = AgeGroup.SCHOOL_AGE,
                diagnosis = "Autism Spectrum Disorder",
                insurance = "Medi-Cal",
                radiusMiles = 15
            )

            viewModel.applySearchFilters(selection)
            viewModel.updateDefaultRadius(25)

            assertEquals(2, controller.appliedCriteria.size)
            assertEquals(selection, controller.appliedCriteria.first())
            assertEquals(selection.copy(radiusMiles = 25), controller.appliedCriteria.last())
        }

    private fun settingsViewModel(
        repository: RecordingProfileRepository,
        discoveryController: RecordingDiscoveryController = RecordingDiscoveryController()
    ) = SettingsViewModel(repository, discoveryController)

    private class RecordingProfileRepository(initialProfile: UserProfile = UserProfile()) :
        UserProfileRepository {
        private val profiles = MutableStateFlow(initialProfile)

        override val profile: Flow<UserProfile> = profiles
        val replacements = mutableListOf<UserProfile>()
        var clearCount = 0
        var clearGate: CompletableDeferred<Unit>? = null
        var clearFailure: Throwable? = null
        var replaceFailure: Throwable? = null

        override suspend fun replaceProfile(profile: UserProfile) {
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

        override suspend fun clearProfile() {
            clearCount += 1
            clearGate?.await()
            clearFailure?.let { throw it }
            profiles.value = UserProfile()
        }
    }

    private class RecordingDiscoveryController : DiscoveryController {
        private val mutableState = MutableStateFlow(DiscoveryState())
        override val state: StateFlow<DiscoveryState> = mutableState
        val appliedCriteria = mutableListOf<DiscoveryFilterSelection>()

        override fun ensureLoaded() = Unit

        override fun setQuery(query: String) {
            mutableState.value = mutableState.value.copy(
                criteria = mutableState.value.criteria.copy(query = query)
            )
        }

        override fun applyFilters(
            therapyTypes: Set<TherapyType>,
            ageGroup: AgeGroup?,
            diagnosis: String?,
            insurance: String?,
            radiusMiles: Int
        ) {
            val selection = DiscoveryFilterSelection(
                therapyTypes = therapyTypes,
                ageGroup = ageGroup,
                diagnosis = diagnosis,
                insurance = insurance,
                radiusMiles = radiusMiles
            )
            appliedCriteria += selection
            mutableState.value = mutableState.value.copy(
                criteria = DiscoveryCriteria(
                    therapyTypes = therapyTypes,
                    ageGroup = ageGroup,
                    diagnosis = diagnosis,
                    insurance = insurance,
                    radiusMiles = radiusMiles,
                    origin = mutableState.value.criteria.origin
                )
            )
        }

        override fun setSingleTherapyAndRefresh(therapyType: TherapyType) = Unit
        override fun useDeviceLocation(latitude: Double, longitude: Double) = Unit
        override fun useLosAngelesCatalog() = Unit
        override fun refresh() = Unit
        override fun retry() = Unit
        override fun clearAllFilters() = Unit
    }

    private fun completeProfile(
        audienceType: AudienceType = AudienceType.FAMILY
    ) = UserProfile(
        onboardingCompleted = true,
        audienceType = audienceType,
        zipCode = "91403",
        journeyStage = JourneyStage.EXPLORING
    )
}
