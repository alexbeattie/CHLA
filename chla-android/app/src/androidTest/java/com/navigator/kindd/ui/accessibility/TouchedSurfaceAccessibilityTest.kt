package com.navigator.kindd.ui.accessibility

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.navigator.kindd.R
import com.navigator.kindd.data.profile.AudienceType
import com.navigator.kindd.data.discovery.DiscoveryCriteria
import com.navigator.kindd.data.discovery.DiscoveryError
import com.navigator.kindd.data.discovery.DiscoveryState
import com.navigator.kindd.data.discovery.TherapyType
import com.navigator.kindd.data.models.Provider
import com.navigator.kindd.data.profile.AgeGroup
import com.navigator.kindd.data.profile.JourneyStage
import com.navigator.kindd.data.profile.UserProfile
import com.navigator.kindd.ui.discovery.DiscoveryFilterContent
import com.navigator.kindd.ui.discovery.DiscoveryStateContent
import com.navigator.kindd.ui.discovery.DiscoveryUiActions
import com.navigator.kindd.ui.home.HomeLookupState
import com.navigator.kindd.ui.home.HomeMessage
import com.navigator.kindd.ui.home.HomeUiState
import com.navigator.kindd.ui.onboarding.CenterLookupState
import com.navigator.kindd.ui.onboarding.LocationState
import com.navigator.kindd.ui.onboarding.OnboardingContent
import com.navigator.kindd.ui.onboarding.OnboardingMode
import com.navigator.kindd.ui.onboarding.OnboardingStep
import com.navigator.kindd.ui.onboarding.OnboardingUiState
import com.navigator.kindd.ui.discovery.DiscoverySearchField
import com.navigator.kindd.ui.onboarding.AudienceStep
import com.navigator.kindd.ui.screens.HomeContent
import com.navigator.kindd.ui.screens.MapContent
import com.navigator.kindd.ui.screens.MapLocationState
import com.navigator.kindd.ui.screens.MapLocationStatus
import com.navigator.kindd.ui.screens.ProviderListContent
import com.navigator.kindd.ui.screens.ProviderListSort
import com.navigator.kindd.ui.screens.SettingsContent
import com.navigator.kindd.ui.theme.KINDDTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        val clickableTags = composeRule.onAllNodes(hasClickAction())
            .fetchSemanticsNodes()
            .mapNotNull { it.config.getOrNull(SemanticsProperties.TestTag) }
        assertTrue(
            "Restart setup must precede Edit in the semantics tree",
            clickableTags.indexOf("settings_restart_setup") <
                clickableTags.indexOf("settings_edit_profile")
        )
        composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach { node ->
            assertFalse(
                "Settings should rely on complete natural order, not partial traversal indices",
                node.config.contains(SemanticsProperties.TraversalIndex)
            )
        }

        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("settings_clear_profile"))
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

    @Test
    fun filter_content_isDirectlyTestableAtNarrowLargeTextWidth() {
        setLocalizedContent(Locale.forLanguageTag("es"), darkTheme = true, fontScale = 1.5f) {
            DiscoveryFilterContent(
                criteria = DiscoveryCriteria(),
                onDismissRequest = {},
                onApply = {}
            )
        }

        composeRule.onNodeWithText("Filtros").assert(hasHeading())
        composeRule.onNodeWithText("Tipos de Terapia").assert(hasHeading())
        assertTargetAtLeast48Dp("discovery_filter_reset", "Restablecer")
        assertTargetAtLeast48Dp("discovery_filter_apply", "Aplicar")
    }

    @Test
    fun materialRolePairsMeetNormalTextContrastInLightTheme() {
        assertThemeContrast(darkTheme = false)
    }

    @Test
    fun materialRolePairsMeetNormalTextContrastInDarkTheme() {
        assertThemeContrast(darkTheme = true)
    }

    @Test
    fun launchThemeResolvesAsDarkBeforeComposeInNightMode() {
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(baseContext.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                Configuration.UI_MODE_NIGHT_YES
        }
        val context = baseContext.createConfigurationContext(configuration).apply {
            setTheme(R.style.Theme_KINDD)
        }
        val attributes = context.obtainStyledAttributes(intArrayOf(android.R.attr.isLightTheme))
        val isLightTheme = attributes.getBoolean(0, true)
        attributes.recycle()
        assertFalse("Launch theme should follow night mode before Compose starts", isLightTheme)
    }

    @Test
    fun home_spanishLargeTextExposesHeadingsAndPoliteLookupStatus() {
        setLocalizedContent(Locale.forLanguageTag("es"), darkTheme = false, fontScale = 1.5f) {
            HomeContent(
                profile = UserProfile(
                    onboardingCompleted = true,
                    audienceType = AudienceType.FAMILY,
                    zipCode = "9000",
                    journeyStage = JourneyStage.EXPLORING
                ),
                uiState = HomeUiState(
                    zipDraft = "9000",
                    lookupState = HomeLookupState.UNAVAILABLE,
                    message = HomeMessage.LOOKUP_UNAVAILABLE
                ),
                onZipChanged = {}, onSubmitZip = {}, onNavigateToMap = {},
                onNavigateToList = {}, onNavigateToRegionalCenters = {},
                onNavigateToChat = {}, onOpenChat = {}, onTherapySelected = {}, onCall = {}
            )
        }

        composeRule.onNodeWithTag("home_compact_logo").assertIsDisplayed()
        composeRule.onNodeWithText("¿Quién atiende a tu familia?").assert(hasHeading())
        composeRule.onNodeWithText("¿Cómo podemos ayudarte?")
            .performScrollTo()
            .assert(hasHeading())
        composeRule.onNodeWithTag("home_zip_lookup_message")
            .assert(hasLiveRegion(LiveRegionMode.Polite))
        assertTargetAtLeast48Dp("home_explore", "Explorar")
        assertFixedTargetAtLeast48Dp("home_ask_capsule", "Pregúntale a KiNDD")
    }

    @Test
    fun allFiveOnboardingStates_spanishLargeTextExposeSemanticState() {
        lateinit var state: MutableState<OnboardingUiState>
        val profile = UserProfile(
            audienceType = AudienceType.FAMILY,
            zipCode = "90001",
            journeyStage = JourneyStage.EXPLORING,
            ageGroup = AgeGroup.SCHOOL_AGE
        )
        setLocalizedContent(Locale.forLanguageTag("es"), darkTheme = true, fontScale = 1.5f) {
            state = remember {
                mutableStateOf(OnboardingUiState(mode = OnboardingMode.EDIT, draft = profile))
            }
            OnboardingContent(
                state = state.value,
                onAudienceSelected = {}, onZipChanged = {}, onUseLocation = {},
                onRetryCenterLookup = {}, onJourneySelected = {}, onAgeSelected = {},
                onBack = {}, onContinue = {}, onFinish = {}, onCancel = {}
            )
        }

        composeRule.onNodeWithText("Encontraste el lugar indicado.").assert(hasHeading())
        composeRule.onNodeWithTag("onboarding_audience_family").assertIsSelected()

        composeRule.runOnIdle {
            state.value = state.value.copy(
                step = OnboardingStep.ZIP,
                locationState = LocationState.DENIED
            )
        }
        composeRule.onNodeWithText("¿Dónde está tu hogar?").assert(hasHeading())
        composeRule.onNodeWithTag("onboarding_location_status")
            .assert(hasLiveRegion(LiveRegionMode.Polite))

        composeRule.runOnIdle {
            state.value = state.value.copy(
                step = OnboardingStep.REGIONAL_CENTER,
                centerLookupState = CenterLookupState.UNAVAILABLE
            )
        }
        composeRule.onNodeWithText(
            localizedString(
                R.string.onboarding_center_unavailable_title,
                Locale.forLanguageTag("es")
            )
        ).assert(hasHeading())
        composeRule.onNodeWithTag("onboarding_center_status")
            .assert(hasLiveRegion(LiveRegionMode.Polite))

        composeRule.runOnIdle { state.value = state.value.copy(step = OnboardingStep.JOURNEY) }
        composeRule.onNodeWithText("¿En qué etapa del camino estás?").assert(hasHeading())
        composeRule.onNodeWithTag("onboarding_journey_exploring").assertIsSelected()

        composeRule.runOnIdle { state.value = state.value.copy(step = OnboardingStep.AGE) }
        composeRule.onNodeWithText("¿Qué edad tiene tu hijo?").assert(hasHeading())
        composeRule.onNodeWithTag("onboarding_age_school_age").assertIsSelected()
        assertTargetAtLeast48Dp("onboarding_age_school_age", "6-12 años")
        assertFixedTargetAtLeast48Dp("onboarding_primary_action", "Guardar")
    }

    @Test
    fun map_spanishNarrowWidthExposesImmersiveLocationSearchAndFilterTargets() {
        setLocalizedContent(Locale.forLanguageTag("es"), darkTheme = false, fontScale = 1f) {
            MapContent(
                state = DiscoveryState(
                    criteria = DiscoveryCriteria(query = "habla"),
                    hasLoadedOnce = true
                ),
                locationState = MapLocationState(status = MapLocationStatus.PERMISSION_DENIED),
                actions = noOpDiscoveryActions(),
                onUseMyLocation = {}, onProviderClick = {}, onNavigateToList = {},
                markerContent = { _, _ -> }
            )
        }

        composeRule.onNodeWithTag("map_title").assertDoesNotExist()
        composeRule.onNodeWithTag("map_search_chrome").assertIsDisplayed()
        composeRule.onNodeWithTag("map_control_rail").assertIsDisplayed()
        composeRule.onNodeWithTag("map_location_status")
            .assert(hasLiveRegion(LiveRegionMode.Polite))
        composeRule.onNodeWithContentDescription("Borrar búsqueda").assertIsDisplayed()
        assertFixedTargetAtLeast48Dp("discovery_clear_query", "Borrar búsqueda")
        assertFixedTargetAtLeast48Dp("map_top_filter", "Filtros")
        assertFixedTargetAtLeast48Dp("map_use_location", "Usar mi ubicación")
        assertFixedTargetAtLeast48Dp("map_refresh", "Actualizar mapa")
    }

    @Test
    fun providerList_spanishNarrowLargeTextKeepsChromeMetadataAndActionsUsable() {
        val provider = Provider(
            id = "matrix",
            name = "Centro de desarrollo",
            phone = "3235551212",
            therapyTypes = TherapyType.entries.map(TherapyType::apiValue),
            distance = 1.2
        )
        setLocalizedContent(Locale.forLanguageTag("es"), darkTheme = true, fontScale = 1.3f) {
            ProviderListContent(
                state = DiscoveryState(
                    criteria = DiscoveryCriteria(therapyTypes = setOf(TherapyType.ABA)),
                    providers = listOf(provider),
                    hasLoadedOnce = true
                ),
                providers = listOf(provider),
                sort = ProviderListSort.NAME,
                onSortChange = {}, actions = noOpDiscoveryActions(), onProviderClick = {}
            )
        }

        composeRule.onNodeWithTag("list_title").assert(hasHeading())
        composeRule.onNodeWithText("Buscar recursos, servicios o código postal")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("filter_chip_therapy_ABA")
            .assertTextContains("Terapia ABA")
        composeRule.onNodeWithText("Quitar Terapia ABA").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Quitar Terapia ABA").assertIsDisplayed()
        listOf(
            "Terapia ABA", "Terapia del Habla", "Terapia Ocupacional"
        ).forEachIndexed { index, label ->
            composeRule.onNodeWithTag("provider_therapy_matrix_$index", useUnmergedTree = true)
                .assert(hasAnyDescendant(hasText(label)))
        }
        listOf(
            "Fisioterapia", "Terapia de alimentación",
            "Interacción padre-hijo y capacitación parental"
        ).forEach { composeRule.onNodeWithText(it, useUnmergedTree = true).assertDoesNotExist() }
        composeRule.onNodeWithText("+3 más", useUnmergedTree = true).assertIsDisplayed()
        TherapyType.entries.forEach { therapy ->
            composeRule.onNodeWithText(therapy.apiValue, useUnmergedTree = true).assertDoesNotExist()
        }
        assertNodeInsideRoot("list_compact_header")
        assertNodeInsideRoot("discovery_search_field")
        assertFixedTargetAtLeast48Dp("list_sort_button", "Ordenar recursos")
        assertFixedTargetAtLeast48Dp("list_filter_button", "Filtros")
        assertTargetAtLeast48Dp(
            "provider_phone_matrix",
            "(323) 555-1212",
            useUnmergedTree = true
        )
    }

    @Test
    fun discoveryLoadingErrorEmptyAndRetainedErrorUsePoliteLiveRegions() {
        lateinit var state: MutableState<DiscoveryState>
        setLocalizedContent(Locale.forLanguageTag("es"), darkTheme = true, fontScale = 1f) {
            state = remember { mutableStateOf(DiscoveryState(isLoading = true)) }
            DiscoveryStateContent(state = state.value, onRetry = {}) { }
        }

        composeRule.onNodeWithTag("discovery_initial_loading")
            .assert(hasLiveRegion(LiveRegionMode.Polite))
        composeRule.onNodeWithText("Cargando…").assertIsDisplayed()
        composeRule.runOnIdle {
            state.value = DiscoveryState(error = DiscoveryError.NETWORK)
        }
        composeRule.onNodeWithTag("discovery_initial_error")
            .assert(hasLiveRegion(LiveRegionMode.Polite))
        composeRule.runOnIdle {
            state.value = DiscoveryState(hasLoadedOnce = true)
        }
        composeRule.onNodeWithTag("discovery_empty")
            .assert(hasLiveRegion(LiveRegionMode.Polite))
        composeRule.onNodeWithText("No se encontraron recursos").assert(hasHeading())
        composeRule.runOnIdle {
            state.value = DiscoveryState(
                providers = listOf(Provider(id = "one", name = "Uno")),
                error = DiscoveryError.TIMEOUT,
                hasLoadedOnce = true
            )
        }
        composeRule.onNodeWithTag("discovery_error_banner")
            .assert(hasLiveRegion(LiveRegionMode.Polite))
    }

    private fun assertThemeContrast(darkTheme: Boolean) {
        lateinit var scheme: ColorScheme
        setLocalizedContent(Locale.US, darkTheme = darkTheme, fontScale = 1f) {
            val currentScheme = MaterialTheme.colorScheme
            SideEffect { scheme = currentScheme }
        }
        composeRule.waitForIdle()
        listOf(
            "primary" to (scheme.primary to scheme.onPrimary),
            "primaryContainer" to (scheme.primaryContainer to scheme.onPrimaryContainer),
            "secondary" to (scheme.secondary to scheme.onSecondary),
            "tertiary" to (scheme.tertiary to scheme.onTertiary),
            "surface" to (scheme.surface to scheme.onSurface),
            "surfaceVariant" to (scheme.surfaceVariant to scheme.onSurfaceVariant),
            "error" to (scheme.error to scheme.onError),
            "errorContainer" to (scheme.errorContainer to scheme.onErrorContainer)
        ).forEach { (name, colors) ->
            val contrast = contrastRatio(colors.first, colors.second)
            assertTrue("$name contrast was $contrast", contrast >= 4.5)
        }
    }

    private fun contrastRatio(first: Color, second: Color): Double {
        val firstLuminance = first.luminance().toDouble()
        val secondLuminance = second.luminance().toDouble()
        return (maxOf(firstLuminance, secondLuminance) + 0.05) /
            (minOf(firstLuminance, secondLuminance) + 0.05)
    }

    private fun noOpDiscoveryActions() = DiscoveryUiActions(
        onQueryChange = {}, onApplyFilters = {}, onRemoveTherapy = {}, onRemoveAge = {},
        onRemoveDiagnosis = {}, onRemoveInsurance = {}, onRemoveRadius = {},
        onClearAll = {}, onRetry = {}
    )

    private fun assertNodeInsideRoot(tag: String, useUnmergedTree: Boolean = false) {
        val node = composeRule.onNodeWithTag(tag, useUnmergedTree = useUnmergedTree)
            .assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        val root = composeRule.onNodeWithTag("narrow_test_root")
            .fetchSemanticsNode().boundsInRoot
        assertTrue("$tag starts outside the narrow root", node.left >= root.left)
        assertTrue("$tag ends outside the narrow root", node.right <= root.right)
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

        val restartLabel = localizedString(R.string.settings_restart_welcome_setup, locale)
        val editLabel = localizedString(R.string.settings_edit_profile, locale)
        assertTargetAtLeast48Dp("settings_restart_setup", restartLabel)
        assertTargetAtLeast48Dp("settings_edit_profile", editLabel)
    }

    private fun localizedString(resourceId: Int, locale: Locale): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(locale)
        }
        return context.createConfigurationContext(configuration).getString(resourceId)
    }

    private fun assertTargetAtLeast48Dp(
        tag: String,
        label: String,
        useUnmergedTree: Boolean = false
    ) {
        val node = composeRule.onNodeWithTag(tag, useUnmergedTree = useUnmergedTree)
            .performScrollTo()
            .assertIsDisplayed()
        assertNodeAtLeast48Dp(node.fetchSemanticsNode().boundsInRoot, label)
    }

    private fun assertFixedTargetAtLeast48Dp(tag: String, label: String) {
        val node = composeRule.onNodeWithTag(tag).assertIsDisplayed()
        assertNodeAtLeast48Dp(node.fetchSemanticsNode().boundsInRoot, label)
    }

    private fun assertNodeAtLeast48Dp(
        bounds: androidx.compose.ui.geometry.Rect,
        label: String
    ) {
        val minimumPixels = 48f * composeRule.density.density
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
                Box(
                    Modifier
                        .width(320.dp)
                        .height(720.dp)
                        .testTag("narrow_test_root")
                ) {
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
