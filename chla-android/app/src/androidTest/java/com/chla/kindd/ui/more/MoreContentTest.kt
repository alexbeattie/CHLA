package com.chla.kindd.ui.more

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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
        var aboutCount = 0
        var regionsCount = 0
        var websiteCount = 0
        var privacyCount = 0
        var termsCount = 0
        var editCount = 0
        var settingsCount = 0
        composeRule.setContent {
            KINDDTheme {
                MoreContent(
                    onNavigateToFAQ = { faqCount += 1 },
                    onNavigateToAbout = { aboutCount += 1 },
                    onNavigateToRegions = { regionsCount += 1 },
                    onOpenWebsite = { websiteCount += 1 },
                    onOpenPrivacy = { privacyCount += 1 },
                    onOpenTerms = { termsCount += 1 },
                    onNavigateToEditProfile = { editCount += 1 },
                    onNavigateToSettings = { settingsCount += 1 },
                    versionName = "1.4.1"
                )
            }
        }

        composeRule.onNodeWithTag("more_grouped_canvas").assertIsDisplayed()
        composeRule.onNodeWithTag("more_title").assertIsDisplayed()
        listOf(
            "more_faq",
            "more_about",
            "more_regions",
            "more_website",
            "more_privacy",
            "more_terms",
            "more_edit_profile",
            "more_settings"
        ).forEach { tag ->
            composeRule.onNodeWithTag("more_list").performScrollToNode(hasTestTag(tag))
            composeRule.onNodeWithTag(tag).performClick()
        }
        composeRule.runOnIdle {
            assertEquals(1, faqCount)
            assertEquals(1, aboutCount)
            assertEquals(1, regionsCount)
            assertEquals(1, websiteCount)
            assertEquals(1, privacyCount)
            assertEquals(1, termsCount)
            assertEquals(1, editCount)
            assertEquals(1, settingsCount)
        }

        composeRule.onNodeWithTag("more_list")
            .performScrollToNode(hasTestTag("more_privacy"))
        composeRule.onNodeWithTag("more_privacy").assertIsDisplayed()
        composeRule.onNodeWithTag("more_list")
            .performScrollToNode(hasTestTag("more_terms"))
        composeRule.onNodeWithTag("more_terms").assertIsDisplayed()
        composeRule.onNodeWithTag("more_list")
            .performScrollToNode(hasTestTag("more_version"))
        composeRule.onNodeWithTag("more_version").assertHasNoClickAction()
        composeRule.onNodeWithText("1.4.1").assertIsDisplayed()
        composeRule.onNodeWithText("Clinicians").assertDoesNotExist()
        composeRule.onNodeWithText("CHLA", substring = true).assertDoesNotExist()
    }

    @Test
    fun largeTextOnNarrowScreen_keepsEveryMoreDestinationReachable() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, fontScale = 1.5f)
            ) {
                KINDDTheme {
                    MoreContent(
                        onNavigateToFAQ = {},
                        onNavigateToAbout = {},
                        onNavigateToRegions = {},
                        onOpenWebsite = {},
                        onOpenPrivacy = {},
                        onOpenTerms = {},
                        onNavigateToEditProfile = {},
                        onNavigateToSettings = {},
                        modifier = Modifier.width(320.dp).height(640.dp)
                    )
                }
            }
        }

        listOf(
            "more_faq",
            "more_about",
            "more_regions",
            "more_website",
            "more_privacy",
            "more_terms",
            "more_edit_profile",
            "more_settings"
        ).forEach { tag ->
            composeRule.onNodeWithTag("more_list").performScrollToNode(hasTestTag(tag))
            composeRule.onNodeWithTag(tag).assertIsDisplayed()
        }
    }
}
