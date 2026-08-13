package com.navigator.kindd.ui.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.platform.app.InstrumentationRegistry
import com.navigator.kindd.data.discovery.TherapyType
import com.navigator.kindd.data.models.RegionalCenter
import com.navigator.kindd.data.profile.AudienceType
import com.navigator.kindd.data.profile.JourneyStage
import com.navigator.kindd.data.profile.RegionalCenterIdentity
import com.navigator.kindd.data.profile.UserProfile
import com.navigator.kindd.data.servicearea.ServiceAreaCoordinate
import com.navigator.kindd.data.servicearea.ServiceAreaFeature
import com.navigator.kindd.ui.chat.ChatLaunchPrompt
import com.navigator.kindd.ui.screens.HomeContent
import com.navigator.kindd.ui.theme.KINDDTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class HomeContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeUsesLiteralKiNDDHierarchyInsteadOfTheLegacyMaterialMasthead() {
        setHome(matchedState(), profile = matchedProfile())

        composeRule.onNodeWithTag("home_compact_logo").assertIsDisplayed()
        composeRule.onNodeWithText("Los Angeles County").assertIsDisplayed()
        composeRule.onNodeWithTag("home_map_hero").assertIsDisplayed()
        composeRule.onNodeWithTag("home_matched_center_card").assertIsDisplayed()
        composeRule.onNodeWithTag("home_service_tiles").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("How can we help?").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("home_ask_capsule").assertIsDisplayed()

        composeRule.onNodeWithTag("home_title").assertDoesNotExist()
        composeRule.onNodeWithText("Developmental services, made easier.").assertDoesNotExist()
        composeRule.onNodeWithText("Discover services").assertDoesNotExist()
    }

    @Test
    fun unmatchedLayoutKeepsTheZipCard() {
        setHome(
            HomeUiState(
                zipDraft = "90001",
                lookupState = HomeLookupState.UNMATCHED,
                message = HomeMessage.NO_MATCH
            )
        )
        composeRule.onNodeWithText("Who serves your family?").assertIsDisplayed()
        composeRule.onNodeWithText("We couldn't match that ZIP code.")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("Your Regional Center").assertDoesNotExist()
    }

    @Test
    fun matchedLayoutShowsCenterActions() {
        setHome(matchedState(), profile = matchedProfile())
        composeRule.onNodeWithText("YOUR REGIONAL CENTER").assertIsDisplayed()
        composeRule.onNodeWithText("Matched").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("Call now").fetchSemanticsNodes().isNotEmpty())
        composeRule.onNodeWithText("Details").assertIsDisplayed()
    }

    @Test
    fun homeSuppliesSclarcAsTheHighlightedMapArea() {
        setHome(
            state = matchedState().copy(
                serviceAreas = listOf(serviceArea("SCLARC"), serviceArea("WRC")),
                serviceAreaLoadState = ServiceAreaLoadState.READY
            ),
            profile = matchedProfile(),
            mapContent = { model, _ ->
                val highlighted = model.areas.single { area -> area.highlighted }
                Box(Modifier.testTag("home_test_map_highlight_${highlighted.sourceAcronym}"))
            }
        )

        composeRule.onNodeWithTag(
            "home_test_map_highlight_SCLARC",
            useUnmergedTree = true
        ).assertExists()
    }

    @Test
    fun homeMountsBaseMapWhenServiceAreaOverlaysAreUnavailable() {
        setHome(
            state = matchedState().copy(
                serviceAreas = emptyList(),
                serviceAreaLoadState = ServiceAreaLoadState.FAILED
            ),
            profile = matchedProfile(),
            mapContent = { model, _ ->
                assertTrue(model.areas.isEmpty())
                Box(Modifier.testTag("home_test_base_map"))
            }
        )

        composeRule.onNodeWithTag("home_test_base_map", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun matchedCenterShowsCanonicalPhoneWhileHydrationIsPending() {
        val dialed = mutableListOf<String>()
        setHome(
            state = HomeUiState(hydratedIdentity = matchedProfile().regionalCenter),
            profile = matchedProfile(),
            onCall = dialed::add
        )

        composeRule.onNodeWithText("(213) 744-7000").performClick()
        composeRule.runOnIdle { assertEquals(listOf("2137447000"), dialed) }
    }

    @Test
    fun completeReadyProfilesRenderTheirAuthoritativeIdentityOnTheFirstFrame() {
        val profile = mutableStateOf(matchedProfile())
        composeRule.setContent {
            KINDDTheme {
                HomeContent(
                    profile = profile.value,
                    uiState = HomeUiState(),
                    onZipChanged = {},
                    onSubmitZip = {},
                    onNavigateToMap = {},
                    onNavigateToList = {},
                    onNavigateToRegionalCenters = {},
                    onNavigateToChat = {},
                    onOpenChat = {},
                    onTherapySelected = {},
                    onCall = {}
                )
            }
        }

        composeRule.onNodeWithText("South Central Los Angeles Regional Center").assertIsDisplayed()
        composeRule.onNodeWithText("Who serves your family?").assertDoesNotExist()

        composeRule.runOnIdle { profile.value = unmatchedProfile("91311") }

        composeRule.onNodeWithText("Who serves your family?").assertIsDisplayed()
        composeRule.onNodeWithTag("home_zip_input").assertTextContains("91311")
    }

    @Test
    fun rootProfileUpdatesImmediatelyReplaceSameCenterJourneyAndDifferentCenterIdentity() {
        val readyProfile = mutableStateOf(matchedProfile(JourneyStage.JUST_DIAGNOSED))
        composeRule.setContent {
            KINDDTheme {
                HomeContent(
                    profile = readyProfile.value,
                    uiState = matchedState(),
                    onZipChanged = {},
                    onSubmitZip = {},
                    onNavigateToMap = {},
                    onNavigateToList = {},
                    onNavigateToRegionalCenters = {},
                    onNavigateToChat = {},
                    onOpenChat = {},
                    onTherapySelected = {},
                    onCall = {}
                )
            }
        }
        composeRule.onNodeWithText("Request an intake evaluation")
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.runOnIdle {
            readyProfile.value = matchedProfile(JourneyStage.WAITING_FOR_INTAKE)
        }
        composeRule.onNodeWithText("Get ready for the intake")
            .performScrollTo()
            .assertIsDisplayed()

        composeRule.runOnIdle {
            readyProfile.value = matchedProfile().copy(
                regionalCenter = RegionalCenterIdentity(9, "Westside Regional Center", "WRC")
            )
        }
        composeRule.onNodeWithText("Westside Regional Center")
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("SCLARC").assertDoesNotExist()
        composeRule.onNodeWithText("(310) 258-4000").assertIsDisplayed()
    }

    @Test
    fun therapyLabelsAreExact_andCallbacksUseCanonicalTypes() {
        val selected = mutableListOf<TherapyType>()
        setHome(HomeUiState(), onTherapy = selected::add)

        listOf("ABA Therapy", "Speech", "Occupational", "Physical").forEach {
            composeRule.onNodeWithText(it).performClick()
        }

        composeRule.runOnIdle {
            assertEquals(
                listOf(TherapyType.ABA, TherapyType.SPEECH, TherapyType.OCCUPATIONAL, TherapyType.PHYSICAL),
                selected
            )
        }
    }

    @Test
    fun journeyCardsSelectExactCopyAndTypedPromptKeys() {
        val prompts = mutableListOf<ChatLaunchPrompt>()
        val journey = mutableStateOf(JourneyStage.JUST_DIAGNOSED)
        composeRule.setContent {
            KINDDTheme {
                HomeContent(
                    profile = matchedProfile(journey.value),
                    uiState = matchedState(journey.value),
                    onZipChanged = {},
                    onSubmitZip = {},
                    onNavigateToMap = {},
                    onNavigateToList = {},
                    onNavigateToRegionalCenters = {},
                    onNavigateToChat = prompts::add,
                    onOpenChat = {},
                    onTherapySelected = {},
                    onCall = {}
                )
            }
        }
        listOf(
            JourneyStage.JUST_DIAGNOSED to ("What do I say?" to ChatLaunchPrompt.JUST_DIAGNOSED),
            JourneyStage.WAITING_FOR_INTAKE to ("Help me prepare" to ChatLaunchPrompt.WAITING_INTAKE),
            JourneyStage.RECEIVING_SERVICES to ("What can I ask for?" to ChatLaunchPrompt.RECEIVING_SERVICES)
        ).forEach { (stage, expected) ->
            composeRule.runOnIdle { journey.value = stage }
            composeRule.onNodeWithText(expected.first)
                .performScrollTo()
                .performSemanticsAction(SemanticsActions.OnClick)
            composeRule.runOnIdle { assertEquals(expected.second, prompts.removeLast()) }
        }

        composeRule.runOnIdle { journey.value = JourneyStage.EXPLORING }
        composeRule.onNodeWithText("Your next step").assertDoesNotExist()
    }

    @Test
    fun callNowSuppliesOnlyNormalizedDigits_andFallsBackWhenApiPhoneIsBlank() {
        val dialed = mutableListOf<String>()
        val state = mutableStateOf(matchedState())
        composeRule.setContent {
            KINDDTheme {
                HomeContent(
                    profile = matchedProfile(),
                    uiState = state.value,
                    onZipChanged = {},
                    onSubmitZip = {},
                    onNavigateToMap = {},
                    onNavigateToList = {},
                    onNavigateToRegionalCenters = {},
                    onNavigateToChat = {},
                    onOpenChat = {},
                    onTherapySelected = {},
                    onCall = dialed::add
                )
            }
        }
        composeRule.onNodeWithText("Call now")
            .performScrollTo()
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.runOnIdle { assertEquals(listOf("12135551212"), dialed) }

        composeRule.runOnIdle {
            state.value = matchedState().copy(hydratedCenter = center(phone = null))
        }
        composeRule.onNodeWithText("(213) 744-7000").assertIsDisplayed()
    }

    @Test
    fun keyboardAndFindButtonUseTheSameSubmitCallback() {
        var submissions = 0
        setHome(HomeUiState(zipDraft = "90001"), onSubmit = { submissions += 1 })

        composeRule.onNodeWithTag("home_zip_input").performImeAction()
        composeRule.onNodeWithText("Find").performClick()

        composeRule.runOnIdle { assertEquals(2, submissions) }
    }

    @Test
    fun primaryActionsForwardTheirDistinctCallbacks() {
        val calls = mutableListOf<String>()
        composeRule.setContent {
            KINDDTheme {
                HomeContent(
                    profile = matchedProfile(JourneyStage.EXPLORING),
                    uiState = matchedState(JourneyStage.EXPLORING),
                    onZipChanged = {},
                    onSubmitZip = {},
                    onNavigateToMap = { calls += "map" },
                    onNavigateToList = { calls += "list" },
                    onNavigateToRegionalCenters = { calls += "regions" },
                    onNavigateToChat = {},
                    onOpenChat = { calls += "chat" },
                    onTherapySelected = {},
                    onCall = {},
                    onNavigateToAbout = { calls += "about" },
                    onNavigateToFaq = { calls += "faq" },
                    onNavigateToEditProfile = { calls += "edit" },
                    onNavigateToSettings = { calls += "settings" }
                )
            }
        }

        listOf(
            "Explore" to "regions",
            "Details" to "regions",
            "Ask KiNDD anything…" to "chat",
            "About" to "about",
            "FAQ" to "faq"
        ).forEach { (label, expected) ->
            val node = composeRule.onNode(hasClickAction() and hasText(label))
            if (label != "Ask KiNDD anything…") node.performScrollTo()
            node.performClick()
            composeRule.runOnIdle { assertEquals(expected, calls.last()) }
        }

        composeRule.onNodeWithTag("home_header_overflow").performScrollTo().performClick()
        composeRule.onNodeWithText("Change Preferences").performClick()
        composeRule.onNodeWithTag("home_header_overflow").performScrollTo().performClick()
        composeRule.onNodeWithText("Settings").performClick()

        composeRule.runOnIdle {
            assertEquals(
                listOf("regions", "regions", "chat", "about", "faq", "edit", "settings"),
                calls
            )
        }
    }

    @Test
    fun guidedQuestionsUseStableTypedPromptKeys() {
        val prompts = mutableListOf<ChatLaunchPrompt>()
        setHome(matchedState(), profile = matchedProfile(), onChat = prompts::add)

        listOf(
            "We just got a diagnosis. What do we do first?" to ChatLaunchPrompt.FIRST_STEPS,
            "Find ABA therapy near me" to ChatLaunchPrompt.FIND_ABA_NEARBY,
            "What services can SCLARC help fund?" to ChatLaunchPrompt.CENTER_FUNDING
        ).forEach { (question, expected) ->
            composeRule.onNodeWithText(question).performScrollTo().performClick()
            composeRule.runOnIdle { assertEquals(expected, prompts.removeLast()) }
        }

    }

    @Test
    fun unmatchedNextStepUsesYourRegionalCenterInsteadOfAPluralScreenTitle() {
        setHome(
            state = HomeUiState(),
            profile = unmatchedProfile("90001").copy(journeyStage = JourneyStage.JUST_DIAGNOSED)
        )

        composeRule.onNodeWithText(
            "One call to your regional center starts everything - eligibility, evaluations, and services. No referral needed."
        ).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun matchedHeroActionsFitAt320DpAndOnePointThreeFontScale() {
        assertMatchedHeroActionsFit(widthDp = 320)
    }

    @Test
    fun matchedHeroActionsFitAt340DpAndOnePointThreeFontScale() {
        assertMatchedHeroActionsFit(widthDp = 340)
    }

    @Test
    fun compactHeaderFitsSpanishAt320DpAndOnePointThreeFontScale() {
        setNarrowHome(
            widthDp = 320,
            locale = Locale.forLanguageTag("es"),
            fontScale = 1.3f,
            state = matchedState(),
            profile = matchedProfile()
        )

        val root = composeRule.onNodeWithTag("narrow_home_root").fetchSemanticsNode().boundsInRoot
        val logo = composeRule.onNodeWithTag("home_compact_logo").fetchSemanticsNode().boundsInRoot
        val county = composeRule.onNodeWithTag("home_county_pill").fetchSemanticsNode().boundsInRoot
        val overflow = composeRule.onNodeWithTag("home_header_overflow").fetchSemanticsNode().boundsInRoot

        composeRule.onNodeWithText("Condado de LA").assertIsDisplayed()
        listOf("logo" to logo, "county" to county, "overflow" to overflow).forEach { (label, bounds) ->
            assertTrue("$label starts outside narrow root", bounds.left >= root.left)
            assertTrue("$label ends outside narrow root", bounds.right <= root.right)
        }
        assertTrue("logo overlaps county", logo.right <= county.left)
        assertTrue("county overlaps overflow", county.right <= overflow.left)
    }

    @Test
    fun unmatchedFundingQuestionUsesRegionalCenterLookupPromptKey() {
        val prompts = mutableListOf<ChatLaunchPrompt>()
        setHome(HomeUiState(zipDraft = "90001"), onChat = prompts::add)

        composeRule.onNodeWithText("Which regional center serves my ZIP?")
            .performScrollTo()
            .performClick()
        composeRule.runOnIdle {
            assertEquals(ChatLaunchPrompt.FIND_REGIONAL_CENTER, prompts.single())
        }
    }

    @Test
    fun primaryTargetsAreAtLeast48Dp_andTouchedSurfaceHasNoLegacyNames() {
        val state = mutableStateOf(matchedState())
        val profile = mutableStateOf(matchedProfile())
        composeRule.setContent {
            KINDDTheme {
                HomeContent(
                    profile = profile.value,
                    uiState = state.value,
                    onZipChanged = {},
                    onSubmitZip = {},
                    onNavigateToMap = {},
                    onNavigateToList = {},
                    onNavigateToRegionalCenters = {},
                    onNavigateToChat = {},
                    onOpenChat = {},
                    onTherapySelected = {},
                    onCall = {}
                )
            }
        }

        val minimumPixels = 48f * composeRule.density.density
        listOf(
            "ABA Therapy",
            "Speech",
            "Occupational",
            "Physical",
            "Explore",
            "Ask KiNDD anything…",
            "Details",
            "What do I say?"
        ).forEach { label ->
            val node = composeRule.onNode(hasClickAction() and hasText(label))
            if (label != "Ask KiNDD anything…") node.performScrollTo()
            val bounds = node.fetchSemanticsNode().boundsInRoot
            assertTrue("$label width", bounds.width >= minimumPixels)
            assertTrue("$label height", bounds.height >= minimumPixels)
        }
        val callNodes = composeRule.onAllNodes(hasClickAction() and hasText("Call now"))
        callNodes.fetchSemanticsNodes().indices.forEach { index ->
            val bounds = callNodes[index].performScrollTo().fetchSemanticsNode().boundsInRoot
            assertTrue("Call now $index width", bounds.width >= minimumPixels)
            assertTrue("Call now $index height", bounds.height >= minimumPixels)
        }
        composeRule.onAllNodesWithText(
            charArrayOf('C', 'H', 'L', 'A').concatToString(),
            substring = true
        ).assertCountEquals(0)
        composeRule.onAllNodesWithText("KINDD", substring = true).assertCountEquals(0)
        assertTrue(composeRule.onAllNodesWithText("KiNDD", substring = true).fetchSemanticsNodes().isNotEmpty())

        composeRule.runOnIdle {
            state.value = HomeUiState(zipDraft = "90001")
            profile.value = unmatchedProfile("90001")
        }
        val findBounds = composeRule.onNode(hasClickAction() and hasText("Find"))
            .performScrollTo()
            .fetchSemanticsNode()
            .boundsInRoot
        assertTrue("Find width", findBounds.width >= minimumPixels)
        assertTrue("Find height", findBounds.height >= minimumPixels)
    }

    private fun setHome(
        state: HomeUiState,
        profile: UserProfile = unmatchedProfile(state.zipDraft.ifBlank { "90001" }),
        onSubmit: () -> Unit = {},
        onTherapy: (TherapyType) -> Unit = {},
        onChat: (ChatLaunchPrompt) -> Unit = {},
        onCall: (String) -> Unit = {},
        mapContent: (@androidx.compose.runtime.Composable (
            com.navigator.kindd.ui.map.RegionalCenterMapRenderModel,
            (String) -> Unit
        ) -> Unit)? = null
    ) {
        composeRule.setContent {
            KINDDTheme {
                HomeContent(
                    profile = profile,
                    uiState = state,
                    onZipChanged = {},
                    onSubmitZip = onSubmit,
                    onNavigateToMap = {},
                    onNavigateToList = {},
                    onNavigateToRegionalCenters = {},
                    onNavigateToChat = onChat,
                    onOpenChat = {},
                    onTherapySelected = onTherapy,
                    onCall = onCall,
                    regionalCenterMapContent = mapContent
                )
            }
        }
    }

    private fun assertMatchedHeroActionsFit(widthDp: Int) {
        var centerRoleLayout: TextLayoutResult? = null
        setNarrowHome(
            widthDp = widthDp,
            locale = Locale.US,
            fontScale = 1.3f,
            state = matchedState(),
            profile = matchedProfile(),
            onCenterRoleTextLayout = { centerRoleLayout = it }
        )

        val root = composeRule.onNodeWithTag("narrow_home_root").fetchSemanticsNode().boundsInRoot
        val hero = composeRule.onNodeWithTag("home_map_hero").fetchSemanticsNode().boundsInRoot
        val centerRole = composeRule.onNodeWithTag("home_center_role").fetchSemanticsNode().boundsInRoot
        val minimumPixels = 48f * composeRule.density.density
        listOf("+1 (213) 555-1212", "Details").forEach { label ->
            val bounds = composeRule.onNode(hasClickAction() and hasText(label))
                .performScrollTo()
                .assertIsDisplayed()
                .fetchSemanticsNode()
                .boundsInRoot
            assertTrue("$label starts outside narrow root", bounds.left >= root.left)
            assertTrue("$label ends outside narrow root", bounds.right <= root.right)
            assertTrue("$label target is shorter than 48dp", bounds.height >= minimumPixels)
            assertTrue("$label target is narrower than 48dp", bounds.width >= minimumPixels)
        }
        composeRule.runOnIdle {
            val layout = requireNotNull(centerRoleLayout)
            assertTrue("center explanation did not wrap at ${widthDp}dp", layout.lineCount > 1)
            assertFalse("center explanation is truncated at ${widthDp}dp", layout.hasVisualOverflow)
        }
        assertTrue("center explanation starts outside hero", centerRole.top >= hero.top)
        assertTrue("center explanation ends outside hero", centerRole.bottom <= hero.bottom)
        assertTrue("center explanation starts outside narrow root", centerRole.left >= root.left)
        assertTrue("center explanation ends outside narrow root", centerRole.right <= root.right)
    }

    private fun setNarrowHome(
        widthDp: Int,
        locale: Locale,
        fontScale: Float,
        state: HomeUiState,
        profile: UserProfile,
        onCenterRoleTextLayout: (TextLayoutResult) -> Unit = {}
    ) {
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(baseContext.resources.configuration).apply {
            setLocale(locale)
            screenWidthDp = widthDp
            this.fontScale = fontScale
        }
        val localizedContext = baseContext.createConfigurationContext(configuration)
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides configuration,
                LocalDensity provides Density(density.density, fontScale)
            ) {
                KINDDTheme {
                    Box(
                        Modifier
                            .width(widthDp.dp)
                            .height(800.dp)
                            .testTag("narrow_home_root")
                    ) {
                        HomeContent(
                            profile = profile,
                            uiState = state,
                            onZipChanged = {},
                            onSubmitZip = {},
                            onNavigateToMap = {},
                            onNavigateToList = {},
                            onNavigateToRegionalCenters = {},
                            onNavigateToChat = {},
                            onOpenChat = {},
                            onTherapySelected = {},
                            onCall = {},
                            onCenterRoleTextLayout = onCenterRoleTextLayout
                        )
                    }
                }
            }
        }
    }

    private fun matchedState(journey: JourneyStage = JourneyStage.JUST_DIAGNOSED) = HomeUiState(
        hydratedIdentity = matchedProfile(journey).regionalCenter,
        hydratedCenter = center(),
        zipDraft = "90001",
        lookupState = HomeLookupState.MATCHED
    )

    private fun matchedProfile(journey: JourneyStage = JourneyStage.JUST_DIAGNOSED) = UserProfile(
        onboardingCompleted = true,
        audienceType = AudienceType.FAMILY,
        zipCode = "90001",
        regionalCenter = RegionalCenterIdentity(
            7,
            "South Central Los Angeles Regional Center",
            "SCLARC"
        ),
        journeyStage = journey
    )

    private fun unmatchedProfile(zip: String) = UserProfile(
        onboardingCompleted = true,
        audienceType = AudienceType.FAMILY,
        zipCode = zip,
        journeyStage = JourneyStage.EXPLORING
    )

    private fun center(phone: String? = "+1 (213) 555-1212") = RegionalCenter(
        id = 7,
        name = "South Central Los Angeles Regional Center",
        telephone = phone,
        countyServed = "Los Angeles"
    )

    private fun serviceArea(acronym: String) = ServiceAreaFeature(
        id = acronym.hashCode(),
        name = acronym,
        acronym = acronym,
        description = "",
        polygons = listOf(
            listOf(
                ServiceAreaCoordinate(33.8, -118.3),
                ServiceAreaCoordinate(33.9, -118.2),
                ServiceAreaCoordinate(33.7, -118.1)
            )
        )
    )
}
