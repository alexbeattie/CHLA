package com.chla.kindd.ui.settings

import com.chla.kindd.data.profile.UserProfile
import com.chla.kindd.data.profile.UserProfileRepository
import com.chla.kindd.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
            val viewModel = SettingsViewModel(repository)
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
            SettingsViewModel(repository)

            runCurrent()

            assertEquals(0, repository.clearCount)
            assertTrue(repository.replacements.isEmpty())
        }

    @Test
    fun confirmedClear_clearsExactlyOnce_withoutManualNavigation() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = RecordingProfileRepository()
            val viewModel = SettingsViewModel(repository)
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
            val viewModel = SettingsViewModel(repository)

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
            val viewModel = SettingsViewModel(repository)
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

    private class RecordingProfileRepository : UserProfileRepository {
        private val profiles = MutableStateFlow(UserProfile())

        override val profile: Flow<UserProfile> = profiles
        val replacements = mutableListOf<UserProfile>()
        var clearCount = 0
        var clearGate: CompletableDeferred<Unit>? = null
        var clearFailure: Throwable? = null

        override suspend fun replaceProfile(profile: UserProfile) {
            replacements += profile
            profiles.value = profile
        }

        override suspend fun clearProfile() {
            clearCount += 1
            clearGate?.await()
            clearFailure?.let { throw it }
            profiles.value = UserProfile()
        }
    }
}
