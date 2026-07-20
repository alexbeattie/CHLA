package com.chla.kindd.ui.app

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.chla.kindd.data.profile.AudienceType
import com.chla.kindd.data.profile.JourneyStage
import com.chla.kindd.data.profile.UserProfile
import com.chla.kindd.ui.theme.KINDDTheme
import org.junit.Rule
import org.junit.Test

class AppEntryContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loading_showsOnlyLaunchSurface() {
        compose(AppEntryState.Loading)

        composeRule.onNodeWithTag("app_entry_loading").assertExists()
        composeRule.onNodeWithTag("app_entry_onboarding").assertDoesNotExist()
        composeRule.onNodeWithTag("app_entry_main").assertDoesNotExist()
    }

    @Test
    fun needsOnboarding_showsOnlyOnboardingSlot() {
        compose(AppEntryState.NeedsOnboarding(UserProfile()))

        composeRule.onNodeWithTag("app_entry_loading").assertDoesNotExist()
        composeRule.onNodeWithTag("app_entry_onboarding").assertExists()
        composeRule.onNodeWithTag("app_entry_main").assertDoesNotExist()
    }

    @Test
    fun ready_showsOnlyMainSlot() {
        compose(AppEntryState.Ready(completeProfile()))

        composeRule.onNodeWithTag("app_entry_loading").assertDoesNotExist()
        composeRule.onNodeWithTag("app_entry_onboarding").assertDoesNotExist()
        composeRule.onNodeWithTag("app_entry_main").assertExists()
    }

    private fun compose(state: AppEntryState) {
        composeRule.setContent {
            KINDDTheme {
                AppEntryContent(
                    state = state,
                    onboardingContent = { Text("Onboarding slot") },
                    mainContent = { Text("Main slot") }
                )
            }
        }
    }

    private fun completeProfile() = UserProfile(
        onboardingCompleted = true,
        audienceType = AudienceType.FAMILY,
        zipCode = "90001",
        journeyStage = JourneyStage.EXPLORING
    )
}
