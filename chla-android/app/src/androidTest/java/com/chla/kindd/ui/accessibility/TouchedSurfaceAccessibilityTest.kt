package com.chla.kindd.ui.accessibility

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.chla.kindd.data.profile.AudienceType
import com.chla.kindd.ui.discovery.DiscoverySearchField
import com.chla.kindd.ui.onboarding.AudienceStep
import com.chla.kindd.ui.screens.SettingsContent
import com.chla.kindd.ui.theme.KINDDTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class TouchedSurfaceAccessibilityTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settings_narrowLargeTextLightEnglish_keepsPrimaryActionsUsable() {
        assertNarrowLargeTextSettings(Locale.US, darkTheme = false)
    }

    @Test
    fun settings_narrowLargeTextDarkEnglish_keepsPrimaryActionsUsable() {
        assertNarrowLargeTextSettings(Locale.US, darkTheme = true)
    }

    @Test
    fun settings_narrowLargeTextLightSpanish_keepsPrimaryActionsUsable() {
        assertNarrowLargeTextSettings(Locale.forLanguageTag("es"), darkTheme = false)
    }

    @Test
    fun settings_narrowLargeTextDarkSpanish_keepsPrimaryActionsUsable() {
        assertNarrowLargeTextSettings(Locale.forLanguageTag("es"), darkTheme = true)
    }

    @Test
    fun settings_talkBackSemanticsExposeHeadingsOrderAndAnnouncements() {
        setLocalizedContent(Locale.US, darkTheme = false, fontScale = 1f) {
            SettingsContent(
                onNavigateToFAQ = {},
                onNavigateToAbout = {},
                onEditProfile = {},
                onClearProfile = {}
            )
        }

        composeRule.onNodeWithTag("settings_title").assert(hasHeading())
        composeRule.onNodeWithTag("settings_profile_heading").assert(hasHeading())
        val editTop = composeRule.onNodeWithTag("settings_edit_profile")
            .fetchSemanticsNode().boundsInRoot.top
        val clearTop = composeRule.onNodeWithTag("settings_clear_profile")
            .fetchSemanticsNode().boundsInRoot.top
        assertTrue("Edit must precede Clear in traversal order", editTop < clearTop)

        composeRule.onNodeWithTag("settings_clear_profile").performClick()
        composeRule.onNodeWithTag("settings_clear_confirmation")
            .assert(hasLiveRegion(LiveRegionMode.Polite))
    }

    @Test
    fun onboarding_talkBackSemanticsExposeHeadingAndSelection() {
        setLocalizedContent(Locale.US, darkTheme = true, fontScale = 1f) {
            AudienceStep(
                selectedAudience = AudienceType.FAMILY,
                onAudienceSelected = {}
            )
        }
        composeRule.onNodeWithText("You found the right place.").assert(hasHeading())
        composeRule.onNodeWithTag("onboarding_audience_family").assertIsSelected()
    }

    @Test
    fun discovery_iconControlsHaveLocalizedDescriptionsAnd48DpTargets() {
        setLocalizedContent(Locale.forLanguageTag("es"), darkTheme = false, fontScale = 1f) {
            DiscoverySearchField(
                query = "habla",
                onQueryChange = {},
                onFilterClick = {}
            )
        }
        composeRule.onNodeWithContentDescription("Borrar búsqueda").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Filtros").assertIsDisplayed()
        assertEveryClickableTargetIsAtLeast48Dp()
        assertEveryIconOnlyControlHasDescription()
    }

    private fun assertNarrowLargeTextSettings(locale: Locale, darkTheme: Boolean) {
        setLocalizedContent(
            locale = locale,
            darkTheme = darkTheme,
            fontScale = 1.5f
        ) {
            SettingsContent(
                onNavigateToFAQ = {},
                onNavigateToAbout = {},
                onEditProfile = {},
                onClearProfile = {}
            )
        }

        val editLabel = if (locale.language == "es") {
            "Editar perfil e introducción"
        } else {
            "Edit Profile & Onboarding"
        }
        val clearLabel = if (locale.language == "es") {
            "Borrar perfil y reiniciar"
        } else {
            "Clear Profile & Restart"
        }
        assertTargetAtLeast48Dp("settings_edit_profile", editLabel)
        assertTargetAtLeast48Dp("settings_clear_profile", clearLabel)
    }

    private fun assertTargetAtLeast48Dp(tag: String, label: String) {
        val node = composeRule.onNodeWithTag(tag).performScrollTo().assertIsDisplayed()
        val minimumPixels = 48f * composeRule.density.density
        val bounds = node.fetchSemanticsNode().boundsInRoot
        assertTrue("$label is narrower than 48dp", bounds.width >= minimumPixels)
        assertTrue("$label is shorter than 48dp", bounds.height >= minimumPixels)
    }

    private fun assertEveryClickableTargetIsAtLeast48Dp() {
        val minimumPixels = 48f * composeRule.density.density
        composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach { node ->
            assertTrue("Clickable target is narrower than 48dp: ${node.config}",
                node.boundsInRoot.width >= minimumPixels)
            assertTrue("Clickable target is shorter than 48dp: ${node.config}",
                node.boundsInRoot.height >= minimumPixels)
        }
    }

    private fun assertEveryIconOnlyControlHasDescription() {
        composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach { node ->
            val text = node.config.getOrNull(SemanticsProperties.Text).orEmpty()
            val editableText = node.config.getOrNull(SemanticsProperties.EditableText)
            if (text.isEmpty() && editableText == null) {
                val descriptions =
                    node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
                assertTrue("Icon-only control has no content description: ${node.config}",
                    descriptions.isNotEmpty())
            }
        }
    }

    private fun setLocalizedContent(
        locale: Locale,
        darkTheme: Boolean,
        fontScale: Float,
        content: @Composable () -> Unit
    ) {
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(baseContext.resources.configuration).apply {
            setLocale(locale)
            screenWidthDp = 320
            this.fontScale = fontScale
        }
        val localizedContext = baseContext.createConfigurationContext(configuration)
        composeRule.setContent {
            LocalizedSurface(
                context = localizedContext,
                configuration = configuration,
                darkTheme = darkTheme,
                fontScale = fontScale,
                content = content
            )
        }
    }

    @Composable
    private fun LocalizedSurface(
        context: Context,
        configuration: Configuration,
        darkTheme: Boolean,
        fontScale: Float,
        content: @Composable () -> Unit
    ) {
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalContext provides context,
            LocalConfiguration provides configuration,
            LocalDensity provides Density(density.density, fontScale)
        ) {
            KINDDTheme(darkTheme = darkTheme) {
                Box(Modifier.width(320.dp).height(640.dp)) {
                    content()
                }
            }
        }
    }

    private fun hasHeading() = SemanticsMatcher("is heading") { node ->
        node.config.contains(SemanticsProperties.Heading)
    }

    private fun hasLiveRegion(mode: LiveRegionMode) =
        SemanticsMatcher("has $mode live region") { node ->
            node.config.getOrNull(SemanticsProperties.LiveRegion) == mode
        }
}
