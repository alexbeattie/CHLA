package com.chla.kindd.ui.more

import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.chla.kindd.ui.screens.MoreContent
import com.chla.kindd.ui.theme.KINDDTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MoreContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun groupedMore_exposesOnlyRealActions_andInvokesEachExactlyOnce() {
        var faqCount = 0
        var websiteCount = 0
        var editCount = 0
        var settingsCount = 0
        composeRule.setContent {
            KINDDTheme {
                MoreContent(
                    onNavigateToFAQ = { faqCount += 1 },
                    onOpenWebsite = { websiteCount += 1 },
                    onNavigateToEditProfile = { editCount += 1 },
                    onNavigateToSettings = { settingsCount += 1 },
                    versionName = "1.4.1"
                )
            }
        }

        composeRule.onNodeWithTag("more_grouped_canvas").assertIsDisplayed()
        composeRule.onNodeWithTag("more_title").assertIsDisplayed()
        composeRule.onNodeWithTag("more_faq").performClick()
        composeRule.onNodeWithTag("more_website").performClick()
        composeRule.onNodeWithTag("more_edit_profile").performClick()
        composeRule.onNodeWithTag("more_settings").performClick()
        composeRule.runOnIdle {
            assertEquals(1, faqCount)
            assertEquals(1, websiteCount)
            assertEquals(1, editCount)
            assertEquals(1, settingsCount)
        }

        composeRule.onNodeWithTag("more_version").assertHasNoClickAction()
        composeRule.onNodeWithText("1.4.1").assertIsDisplayed()
        composeRule.onNodeWithText("Privacy Policy").assertDoesNotExist()
        composeRule.onNodeWithText("Terms of Service").assertDoesNotExist()
        composeRule.onNodeWithText("Clinicians").assertDoesNotExist()
        composeRule.onNodeWithText("CHLA", substring = true).assertDoesNotExist()
    }
}
