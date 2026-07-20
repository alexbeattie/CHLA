package com.chla.kindd.ui.onboarding

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.chla.kindd.data.profile.AgeGroup
import com.chla.kindd.data.profile.AudienceType
import com.chla.kindd.data.profile.JourneyStage
import com.chla.kindd.data.profile.RegionalCenterIdentity
import com.chla.kindd.data.profile.UserProfile
import com.chla.kindd.ui.theme.KINDDTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun audience_showsItsHeadingAndExactlyOnePrimaryAction() {
        assertStep(state(step = OnboardingStep.AUDIENCE), "You found the right place.")
    }

    @Test
    fun zip_showsItsHeadingAndExactlyOnePrimaryAction() {
        assertStep(
            state(step = OnboardingStep.ZIP, draft = draft(zipCode = "90001")),
            "Where is home?"
        )
    }

    @Test
    fun matchedCenter_showsItsHeadingAndExactlyOnePrimaryAction() {
        assertStep(
            state(
                step = OnboardingStep.REGIONAL_CENTER,
                draft = draft(zipCode = "90001", center = center()),
                centerLookupState = CenterLookupState.MATCHED
            ),
            "Your Regional Center"
        )
    }

    @Test
    fun journey_showsItsHeadingAndExactlyOnePrimaryAction() {
        assertStep(
            state(step = OnboardingStep.JOURNEY, draft = draft(zipCode = "90001")),
            "Where are you in the journey?"
        )
    }

    @Test
    fun age_showsItsHeadingAndExactlyOnePrimaryAction() {
        assertStep(
            state(
                step = OnboardingStep.AGE,
                draft = draft(zipCode = "90001", journey = JourneyStage.EXPLORING)
            ),
            "How old is your child?"
        )
    }

    @Test
    fun shortZip_disablesContinue() {
        compose(state(step = OnboardingStep.ZIP, draft = draft(zipCode = "900")))

        composeRule.onNodeWithTag("onboarding_primary_action").assertIsNotEnabled()
    }

    @Test
    fun missingJourney_disablesContinue() {
        compose(state(step = OnboardingStep.JOURNEY, draft = draft(zipCode = "90001")))

        composeRule.onNodeWithTag("onboarding_primary_action").assertIsNotEnabled()
    }

    @Test
    fun unmatchedCenter_explainsFallbackAndAllowsContinue() {
        compose(
            state(
                step = OnboardingStep.REGIONAL_CENTER,
                draft = draft(zipCode = "90001"),
                centerLookupState = CenterLookupState.UNMATCHED
            )
        )

        composeRule.onNodeWithText("We'll figure it out together").assertExists()
        composeRule.onNodeWithTag("onboarding_primary_action").assertIsEnabled()
    }

    @Test
    fun unavailableFirstRun_offersRetryAndAllowsContinue() {
        compose(
            state(
                step = OnboardingStep.REGIONAL_CENTER,
                draft = draft(zipCode = "90001"),
                centerLookupState = CenterLookupState.UNAVAILABLE
            )
        )

        composeRule.onNodeWithTag("onboarding_retry_center").assertExists()
        composeRule.onNodeWithTag("onboarding_primary_action").assertIsEnabled()
    }

    @Test
    fun unavailableEdit_offersRetryButDoesNotDiscardPersistedCenterByContinuing() {
        compose(
            state(
                mode = OnboardingMode.EDIT,
                step = OnboardingStep.REGIONAL_CENTER,
                draft = draft(zipCode = "90001"),
                centerLookupState = CenterLookupState.UNAVAILABLE
            )
        )

        composeRule.onNodeWithTag("onboarding_retry_center").assertExists()
        composeRule.onNodeWithTag("onboarding_primary_action").assertIsNotEnabled()
    }

    @Test
    fun editMode_showsCancelAndPrefilledSelections() {
        compose(
            state(
                mode = OnboardingMode.EDIT,
                step = OnboardingStep.AUDIENCE,
                draft = draft(audience = AudienceType.CLINICIAN)
            )
        )

        composeRule.onNodeWithTag("onboarding_cancel_action").assertExists()
        composeRule.onNodeWithTag("onboarding_audience_clinician").assertIsSelected()
    }

    @Test
    fun ageStep_showsLocalizedPrefilledSelection() {
        compose(
            state(
                mode = OnboardingMode.EDIT,
                step = OnboardingStep.AGE,
                draft = draft(
                    zipCode = "90001",
                    journey = JourneyStage.EXPLORING,
                    age = AgeGroup.SCHOOL_AGE
                )
            )
        )

        composeRule.onNodeWithText("6-12 years (School Age)").assertExists()
        composeRule.onNodeWithTag("onboarding_age_school_age").assertIsSelected()
        composeRule.onNodeWithText("Save").assertExists()
    }

    @Test
    fun locationDenied_keepsLocationAsAUserControlledAction() {
        compose(
            state(
                step = OnboardingStep.ZIP,
                locationState = LocationState.DENIED
            )
        )

        composeRule.onNodeWithText("Location access wasn't allowed. Enter your ZIP code or try again.")
            .assertExists()
        composeRule.onNodeWithTag("onboarding_use_location").assertExists().assertHasClickAction()
    }

    @Test
    fun audienceClickableControls_haveRolesLabelsAndAtLeast48DpBounds() {
        assertAccessibleControls(state(step = OnboardingStep.AUDIENCE))
    }

    @Test
    fun zipClickableControls_haveRolesLabelsAndAtLeast48DpBounds() {
        assertAccessibleControls(
            state(step = OnboardingStep.ZIP, draft = draft(zipCode = "90001"))
        )
    }

    @Test
    fun unavailableCenterClickableControls_haveRolesLabelsAndAtLeast48DpBounds() {
        assertAccessibleControls(
            state(
                step = OnboardingStep.REGIONAL_CENTER,
                draft = draft(zipCode = "90001"),
                centerLookupState = CenterLookupState.UNAVAILABLE
            )
        )
    }

    @Test
    fun journeyClickableControls_haveRolesLabelsAndAtLeast48DpBounds() {
        assertAccessibleControls(
            state(step = OnboardingStep.JOURNEY, draft = draft(zipCode = "90001"))
        )
    }

    @Test
    fun editAgeClickableControls_haveRolesLabelsAndAtLeast48DpBounds() {
        assertAccessibleControls(
            state(
                mode = OnboardingMode.EDIT,
                step = OnboardingStep.AGE,
                draft = draft(zipCode = "90001", journey = JourneyStage.EXPLORING)
            )
        )
    }

    private fun assertAccessibleControls(state: OnboardingUiState) {
        compose(state)

        val minimumPixels = 48f * composeRule.density.density
        composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach { node ->
            val hasRole = node.config.getOrNull(SemanticsProperties.Role) != null
            val isEditableText = node.config.getOrNull(SemanticsProperties.EditableText) != null
            val hasText = node.config.getOrNull(SemanticsProperties.Text).orEmpty().isNotEmpty()
            val hasDescription =
                node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().isNotEmpty()
            assertTrue(
                "Clickable control is missing a semantic role: ${node.config}",
                hasRole || isEditableText
            )
            assertTrue(
                "Clickable control is missing an accessible label: ${node.config}",
                hasText || hasDescription
            )
            assertTrue(
                "Clickable control is narrower than 48dp: ${node.config}",
                node.boundsInRoot.width >= minimumPixels
            )
            assertTrue(
                "Clickable control is shorter than 48dp: ${node.config}",
                node.boundsInRoot.height >= minimumPixels
            )
        }
    }

    private fun compose(state: OnboardingUiState) {
        composeRule.setContent {
            KINDDTheme {
                OnboardingContent(
                    state = state,
                    onAudienceSelected = {},
                    onZipChanged = {},
                    onUseLocation = {},
                    onRetryCenterLookup = {},
                    onJourneySelected = {},
                    onAgeSelected = {},
                    onBack = {},
                    onContinue = {},
                    onFinish = {},
                    onCancel = {}
                )
            }
        }
    }

    private fun assertStep(state: OnboardingUiState, heading: String) {
        compose(state)
        composeRule.onNodeWithText(heading).assertExists()
        composeRule.onAllNodesWithTag("onboarding_primary_action").assertCountEquals(1)
    }

    private fun state(
        mode: OnboardingMode = OnboardingMode.FIRST_RUN,
        step: OnboardingStep,
        draft: UserProfile = draft(),
        centerLookupState: CenterLookupState = CenterLookupState.IDLE,
        locationState: LocationState = LocationState.IDLE
    ) = OnboardingUiState(
        mode = mode,
        step = step,
        draft = draft,
        centerLookupState = centerLookupState,
        locationState = locationState
    )

    private fun draft(
        audience: AudienceType = AudienceType.FAMILY,
        zipCode: String? = null,
        center: RegionalCenterIdentity? = null,
        journey: JourneyStage? = null,
        age: AgeGroup? = null
    ) = UserProfile(
        onboardingCompleted = false,
        audienceType = audience,
        zipCode = zipCode,
        regionalCenter = center,
        journeyStage = journey,
        ageGroup = age
    )

    private fun center() = RegionalCenterIdentity(
        id = 7,
        name = "Westside Regional Center",
        shortName = "WRC"
    )
}
