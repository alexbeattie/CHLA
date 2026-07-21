package com.chla.kindd.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.chla.kindd.ui.screens.SettingsContent
import com.chla.kindd.ui.theme.KINDDTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun profileControls_editNavigates_andClearRequiresConfirmation() {
        var editCount = 0
        var clearCount = 0
        composeRule.setContent {
            KINDDTheme {
                SettingsContent(
                    onNavigateToFAQ = {},
                    onNavigateToAbout = {},
                    onEditProfile = { editCount += 1 },
                    onClearProfile = { clearCount += 1 }
                )
            }
        }

        composeRule.onNodeWithText("Edit Profile & Onboarding")
            .assertIsDisplayed()
            .performClick()
        composeRule.runOnIdle { assertEquals(1, editCount) }

        composeRule.onNodeWithText("Clear Profile & Restart").performClick()
        composeRule.onNodeWithTag("settings_clear_confirmation").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Your saved profile will be removed, and onboarding will run again."
        ).assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, clearCount) }

        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithTag("settings_clear_confirmation").assertDoesNotExist()
        composeRule.runOnIdle { assertEquals(0, clearCount) }

        composeRule.onNodeWithText("Clear Profile & Restart").performClick()
        composeRule.onNodeWithTag("settings_confirm_clear").performClick()

        composeRule.runOnIdle { assertEquals(1, clearCount) }
        composeRule.onNodeWithTag("settings_clear_confirmation").assertDoesNotExist()
    }

    @Test
    fun clearFailure_showsSanitizedLocalizedRetryMessage() {
        composeRule.setContent {
            KINDDTheme {
                SettingsContent(
                    onNavigateToFAQ = {},
                    onNavigateToAbout = {},
                    onEditProfile = {},
                    onClearProfile = {},
                    clearFailed = true
                )
            }
        }

        composeRule.onNodeWithTag("settings_clear_profile_error").assertIsDisplayed()
        composeRule.onNodeWithText("We couldn't clear your profile. Please try again.")
            .assertIsDisplayed()
    }
}
