package com.chla.kindd.ui.app

import com.chla.kindd.data.profile.AudienceType
import com.chla.kindd.data.profile.FakeUserProfileRepository
import com.chla.kindd.data.profile.JourneyStage
import com.chla.kindd.data.profile.UserProfile
import com.chla.kindd.data.profile.UserProfileRepository
import com.chla.kindd.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppEntryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun state_startsLoadingBeforeProfileEmits() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = AppEntryViewModel(FakeUserProfileRepository())

        assertEquals(AppEntryState.Loading, viewModel.state.value)
    }

    @Test
    fun incompleteProfile_needsOnboardingWithTheProfileDraft() =
        runTest(mainDispatcherRule.testDispatcher) {
            val profile = UserProfile(onboardingCompleted = true)
            val viewModel = AppEntryViewModel(FakeUserProfileRepository(profile))

            runCurrent()

            assertEquals(AppEntryState.NeedsOnboarding(profile), viewModel.state.value)
        }

    @Test
    fun completeProfile_isReady() = runTest(mainDispatcherRule.testDispatcher) {
        val profile = completeProfile()
        val viewModel = AppEntryViewModel(FakeUserProfileRepository(profile))

        runCurrent()

        assertEquals(AppEntryState.Ready(profile), viewModel.state.value)
    }

    @Test
    fun clearAfterReady_returnsToOnboarding() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeUserProfileRepository(completeProfile())
        val viewModel = AppEntryViewModel(repository)
        runCurrent()
        assertEquals(AppEntryState.Ready(completeProfile()), viewModel.state.value)

        repository.clearProfile()
        runCurrent()

        assertEquals(
            AppEntryState.NeedsOnboarding(UserProfile()),
            viewModel.state.value
        )
    }

    @Test
    fun profileFlowFailure_fallsBackToDefaultOnboarding() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = object : UserProfileRepository {
                override val profile: Flow<UserProfile> = flow {
                    throw IllegalStateException("profile unavailable")
                }

                override suspend fun replaceProfile(profile: UserProfile) = Unit

                override suspend fun replaceProfileIfCurrent(
                    expected: UserProfile,
                    replacement: UserProfile
                ) = false

                override suspend fun clearProfile() = Unit
            }
            val viewModel = AppEntryViewModel(repository)

            runCurrent()

            assertEquals(
                AppEntryState.NeedsOnboarding(UserProfile()),
                viewModel.state.value
            )
        }

    private fun completeProfile() = UserProfile(
        onboardingCompleted = true,
        audienceType = AudienceType.FAMILY,
        zipCode = "90001",
        journeyStage = JourneyStage.EXPLORING
    )
}
