package com.chla.kindd.ui.home

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
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
import com.chla.kindd.data.discovery.TherapyType
import com.chla.kindd.data.models.RegionalCenter
import com.chla.kindd.data.profile.AudienceType
import com.chla.kindd.data.profile.JourneyStage
import com.chla.kindd.data.profile.RegionalCenterIdentity
import com.chla.kindd.data.profile.UserProfile
import com.chla.kindd.ui.chat.ChatLaunchPrompt
import com.chla.kindd.ui.screens.HomeContent
import com.chla.kindd.ui.theme.KINDDTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

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
        composeRule.onNodeWithTag("home_map_highlight_SCLARC").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("Call now").fetchSemanticsNodes().isNotEmpty())
        composeRule.onNodeWithText("Details").assertIsDisplayed()
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
        composeRule.onNodeWithTag("home_zip_input").assertTextEquals("ZIP code", "91311")
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
        composeRule.onNodeWithText("Call now").assertDoesNotExist()
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
    fun callNowSuppliesOnlyNormalizedDigits_andIsAbsentWithoutPhone() {
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
        composeRule.onAllNodesWithText("Call now")[0].performClick()
        composeRule.runOnIdle { assertEquals(listOf("12135551212"), dialed) }

        composeRule.runOnIdle {
            state.value = matchedState().copy(hydratedCenter = center(phone = null))
        }
        composeRule.onNodeWithText("Call now").assertDoesNotExist()
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
            "We just got a diagnosis. What do we do first?" to ChatLaunchPrompt.JUST_DIAGNOSED,
            "Find ABA therapy near me" to ChatLaunchPrompt.FIND_ABA_NEARBY,
            "What services can SCLARC help fund?" to ChatLaunchPrompt.CENTER_FUNDING
        ).forEach { (question, expected) ->
            composeRule.onNodeWithText(question).performScrollTo().performClick()
            composeRule.runOnIdle { assertEquals(expected, prompts.removeLast()) }
        }

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
        composeRule.onAllNodesWithText("CHLA", substring = true).assertCountEquals(0)
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
        onCall: (String) -> Unit = {}
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
                    onCall = onCall
                )
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
}
