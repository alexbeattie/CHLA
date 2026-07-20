package com.chla.kindd.ui.home

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
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
        setHome(matchedState())
        composeRule.onNodeWithText("Your Regional Center").assertIsDisplayed()
        composeRule.onNodeWithText("Matched").assertIsDisplayed()
        composeRule.onNodeWithText("SCLARC").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("Call now").fetchSemanticsNodes().isNotEmpty())
        composeRule.onNodeWithText("Details").assertIsDisplayed()
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
        val state = mutableStateOf(matchedState(JourneyStage.JUST_DIAGNOSED))
        composeRule.setContent {
            KINDDTheme {
                HomeContent(
                    uiState = state.value,
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
        ).forEach { (journey, expected) ->
            composeRule.runOnIdle { state.value = matchedState(journey) }
            composeRule.onNodeWithText(expected.first).performScrollTo().performClick()
            composeRule.runOnIdle { assertEquals(expected.second, prompts.removeLast()) }
        }

        composeRule.runOnIdle { state.value = matchedState(JourneyStage.EXPLORING) }
        composeRule.onNodeWithText("Your next step").assertDoesNotExist()
    }

    @Test
    fun callNowSuppliesOnlyNormalizedDigits_andIsAbsentWithoutPhone() {
        val dialed = mutableListOf<String>()
        val state = mutableStateOf(matchedState())
        composeRule.setContent {
            KINDDTheme {
                HomeContent(
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
    fun primaryTargetsAreAtLeast48Dp_andTouchedSurfaceHasNoLegacyNames() {
        setHome(matchedState())

        val minimumPixels = 48f * composeRule.density.density
        listOf("Map", "List", "Explore", "Ask KiNDD", "Details").forEach { label ->
            val node = composeRule.onNode(hasClickAction() and hasText(label)).performScrollTo()
            val bounds = node.fetchSemanticsNode().boundsInRoot
            assertTrue("$label width", bounds.width >= minimumPixels)
            assertTrue("$label height", bounds.height >= minimumPixels)
        }
        composeRule.onAllNodesWithText("CHLA", substring = true).assertCountEquals(0)
        composeRule.onAllNodesWithText("KINDD", substring = true).assertCountEquals(0)
        assertTrue(composeRule.onAllNodesWithText("KiNDD", substring = true).fetchSemanticsNodes().isNotEmpty())
    }

    private fun setHome(
        state: HomeUiState,
        onSubmit: () -> Unit = {},
        onTherapy: (TherapyType) -> Unit = {},
        onChat: (ChatLaunchPrompt) -> Unit = {},
        onCall: (String) -> Unit = {}
    ) {
        composeRule.setContent {
            KINDDTheme {
                HomeContent(
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
        profile = UserProfile(
            onboardingCompleted = true,
            audienceType = AudienceType.FAMILY,
            zipCode = "90001",
            regionalCenter = RegionalCenterIdentity(7, "South Central Los Angeles Regional Center", "SCLARC"),
            journeyStage = journey
        ),
        hydratedCenter = center(),
        zipDraft = "90001",
        lookupState = HomeLookupState.MATCHED
    )

    private fun center(phone: String? = "+1 (213) 555-1212") = RegionalCenter(
        id = 7,
        name = "South Central Los Angeles Regional Center",
        telephone = phone,
        countyServed = "Los Angeles"
    )
}
