package com.chla.kindd.ui.settings

import com.chla.kindd.data.profile.UserProfile
import com.chla.kindd.data.profile.UserProfileRepository
import com.chla.kindd.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private class RecordingProfileRepository : UserProfileRepository {
        private val profiles = MutableStateFlow(UserProfile())

        override val profile: Flow<UserProfile> = profiles
        val replacements = mutableListOf<UserProfile>()
        var clearCount = 0

        override suspend fun replaceProfile(profile: UserProfile) {
            replacements += profile
            profiles.value = profile
        }

        override suspend fun clearProfile() {
            clearCount += 1
            profiles.value = UserProfile()
        }
    }
}
