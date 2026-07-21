package com.chla.kindd.ui.settings

import android.content.res.Configuration
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import com.chla.kindd.ui.screens.SettingsContent
import com.chla.kindd.ui.theme.KINDDTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Locale

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

        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(SemanticsMatcher.expectValue(
                SemanticsProperties.TestTag,
                "settings_clear_profile_error"
            ))
        composeRule.onNodeWithTag("settings_clear_profile_error").assertIsDisplayed()
        composeRule.onNodeWithText("We couldn't clear your profile. Please try again.")
            .assertIsDisplayed()
    }

    @Test
    fun settings_usesCompactGroupedHierarchy_withoutDeadActions() {
        var faqCount = 0
        var aboutCount = 0
        composeRule.setContent {
            KINDDTheme {
                SettingsContent(
                    onNavigateToFAQ = { faqCount += 1 },
                    onNavigateToAbout = { aboutCount += 1 },
                    onEditProfile = {},
                    onClearProfile = {}
                )
            }
        }

        composeRule.onNodeWithTag("settings_grouped_canvas").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_solid_top_app_bar").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_setup_group").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_information_group").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_app_info_group").assertIsDisplayed()

        composeRule.onNodeWithText("Privacy Policy").assertDoesNotExist()
        composeRule.onNodeWithText("Terms of Service").assertDoesNotExist()

        composeRule.onNodeWithText("About KiNDD").performClick()
        composeRule.onNodeWithText("FAQ").performClick()
        composeRule.runOnIdle {
            assertEquals(1, aboutCount)
            assertEquals(1, faqCount)
        }
    }

    @Test
    fun settings_matchesIPhoneSectionOrder_andRetainsAccessibleProfileHeading() {
        composeRule.setContent {
            KINDDTheme {
                SettingsContent(
                    onNavigateToFAQ = {},
                    onNavigateToAbout = {},
                    onEditProfile = {},
                    onClearProfile = {},
                    onNavigateBack = {},
                    onOpenLanguageSettings = {},
                    onOpenLocationSettings = {},
                    locationStatus = "Allowed while using KiNDD",
                    appVersion = "1.4.1 (1)"
                )
            }
        }

        val orderedTags = listOf(
            "settings_title",
            "settings_language_heading",
            "settings_profile_heading",
            "settings_search_preferences_heading",
            "settings_location_heading"
        )
        val topCoordinates = orderedTags.map { tag ->
            composeRule.onNodeWithTag(tag).assert(hasHeading())
                .fetchSemanticsNode().boundsInRoot.top
        }

        assertEquals(topCoordinates.sorted(), topCoordinates)
        composeRule.onNodeWithText("1.4.1 (1)").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(SemanticsMatcher.expectValue(
                SemanticsProperties.TestTag,
                "settings_bottom_navigation_clearance"
            ))
        composeRule.onNodeWithTag("settings_bottom_navigation_clearance")
            .assertIsDisplayed()
    }

    @Test
    fun settings_invokesEveryVisibleNavigationCallback() {
        var back = 0
        var language = 0
        var setup = 0
        var location = 0
        var about = 0
        var faq = 0
        var website = 0
        composeRule.setContent {
            KINDDTheme {
                SettingsContent(
                    onNavigateToFAQ = { faq += 1 },
                    onNavigateToAbout = { about += 1 },
                    onEditProfile = { setup += 1 },
                    onClearProfile = {},
                    onNavigateBack = { back += 1 },
                    onOpenLanguageSettings = { language += 1 },
                    onOpenLocationSettings = { location += 1 },
                    onOpenWebsite = { website += 1 },
                    locationStatus = "Not allowed"
                )
            }
        }

        listOf(
            "settings_back_to_more",
            "settings_language",
            "settings_restart_setup",
            "settings_edit_profile",
            "settings_location",
            "settings_about",
            "settings_faq",
            "settings_website"
        ).forEach { tag ->
            composeRule.onNodeWithTag(tag).performScrollTo().performClick()
        }

        composeRule.runOnIdle {
            assertEquals(1, back)
            assertEquals(1, language)
            assertEquals(2, setup)
            assertEquals(1, location)
            assertEquals(1, about)
            assertEquals(1, faq)
            assertEquals(1, website)
        }
    }

    @Test
    fun preAndroid13Contract_canHonestlyOmitLanguageSettings() {
        composeRule.setContent {
            KINDDTheme {
                SettingsContent(
                    onNavigateToFAQ = {},
                    onNavigateToAbout = {},
                    onEditProfile = {},
                    onClearProfile = {},
                    onOpenLanguageSettings = null,
                    onOpenLocationSettings = {}
                )
            }
        }

        composeRule.onNodeWithTag("settings_language_heading").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_language").assertDoesNotExist()
        composeRule.onNodeWithTag("settings_profile_heading").assert(hasHeading())
    }

    @Test
    fun spanishLargeText_keepsFirstActionsReachableAndLocalized() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(targetContext.resources.configuration).apply {
            setLocale(Locale.forLanguageTag("es"))
        }
        val spanishContext = targetContext.createConfigurationContext(configuration)

        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalContext provides spanishContext,
                LocalDensity provides Density(density.density, fontScale = 1.5f)
            ) {
                KINDDTheme {
                    SettingsContent(
                        onNavigateToFAQ = {},
                        onNavigateToAbout = {},
                        onEditProfile = {},
                        onClearProfile = {},
                        onNavigateBack = {},
                        onOpenLanguageSettings = {},
                        onOpenLocationSettings = {},
                        locationStatus = "No permitido"
                    )
                }
            }
        }

        composeRule.onNodeWithText("Ajustes").assertIsDisplayed()
        composeRule.onNodeWithText("IDIOMA").assertIsDisplayed()
        listOf(
            "settings_back_to_more",
            "settings_language",
            "settings_restart_setup",
            "settings_edit_profile",
            "settings_location"
        ).forEach { tag ->
            val node = composeRule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
            assertTrue(
                "$tag must remain a button under Spanish large text",
                hasClickAction().matches(node.fetchSemanticsNode())
            )
        }
    }

    private fun hasHeading() = SemanticsMatcher("is heading") { node ->
        node.config.contains(SemanticsProperties.Heading)
    }
}
