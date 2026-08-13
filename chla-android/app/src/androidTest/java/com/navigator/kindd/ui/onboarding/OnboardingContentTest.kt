package com.navigator.kindd.ui.onboarding

import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.navigator.kindd.data.models.RegionalCenter
import com.navigator.kindd.data.profile.AgeGroup
import com.navigator.kindd.data.profile.AudienceType
import com.navigator.kindd.data.profile.JourneyStage
import com.navigator.kindd.data.profile.RegionalCenterIdentity
import com.navigator.kindd.data.profile.UserProfile
import com.navigator.kindd.data.profile.UserProfileRepository
import com.navigator.kindd.data.source.RegionalCenterDataSource
import com.navigator.kindd.data.source.RegionalCenterLookup
import com.navigator.kindd.data.source.UserCoordinates
import com.navigator.kindd.data.source.UserLocationSource
import com.navigator.kindd.ui.regions.RegionalCenterServiceAreaState
import com.navigator.kindd.ui.theme.KINDDTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class OnboardingContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun audience_showsItsHeadingAndExactlyOnePrimaryAction() {
        assertStep(state(step = OnboardingStep.AUDIENCE), "You found the right place.")
    }

    @Test
    fun audience_usesCapsuleProgressLogoAndSegmentedChoice() {
        compose(state(step = OnboardingStep.AUDIENCE))

        composeRule.onNodeWithTag("onboarding_progress_capsules").assertExists()
        composeRule.onNodeWithTag("onboarding_progress_active_0").assertExists()
        composeRule.onNodeWithTag("onboarding_logo").assertExists()
        composeRule.onNodeWithTag("onboarding_logo_image")
            .assertIsDisplayed()
            .assertContentDescriptionEquals("KiNDD")
        composeRule.onNodeWithText("K").assertDoesNotExist()
        composeRule.onNodeWithTag("onboarding_audience_segmented").assertExists()
        composeRule.onNodeWithTag("onboarding_primary_gradient", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun edgeToEdge_progressChromeStartsBelowTheStatusBar() {
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.enableEdgeToEdge()
        }
        compose(state(step = OnboardingStep.AUDIENCE))

        val statusBarBottom = ViewCompat.getRootWindowInsets(
            composeRule.activity.window.decorView
        )?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
        val progressTop = composeRule.onNodeWithTag("onboarding_progress_capsules")
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot
            .top

        assertTrue("Expected a non-zero status bar inset", statusBarBottom > 0)
        assertTrue(
            "Onboarding progress starts behind the status bar: $progressTop < $statusBarBottom",
            progressTop >= statusBarBottom
        )
    }

    @Test
    fun zip_showsItsHeadingAndExactlyOnePrimaryAction() {
        assertStep(
            state(step = OnboardingStep.ZIP, draft = draft(zipCode = "90001")),
            "Where is home?"
        )
    }

    @Test
    fun zip_usesLocationIconAndCompactCenteredControl() {
        compose(state(step = OnboardingStep.ZIP, draft = draft(zipCode = "90001")))

        composeRule.onNodeWithTag("onboarding_zip_icon").assertExists()
        val zipControl = composeRule.onNodeWithTag("onboarding_zip_control")
            .assertExists()
            .fetchSemanticsNode()
        val maximumWidth = 240f * composeRule.density.density
        assertTrue(
            "ZIP control is wider than the compact 240dp contract",
            zipControl.boundsInRoot.width <= maximumWidth
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
    fun matchedCenter_usesBoundaryMapHeroAndOverlaidCard() {
        compose(
            state(
                step = OnboardingStep.REGIONAL_CENTER,
                draft = draft(zipCode = "90001", center = center()),
                centerLookupState = CenterLookupState.MATCHED
            )
        )

        composeRule.onNodeWithTag("onboarding_center_map_hero").assertExists()
        composeRule.onNodeWithTag("regional_center_map_surface").assertExists()
        composeRule.onNodeWithTag("onboarding_matched_center_card").assertExists()
    }

    @Test
    fun matchedCenter_serviceAreaLoadingUsesStableFallbackInsteadOfMap() {
        compose(
            state(
                step = OnboardingStep.REGIONAL_CENTER,
                draft = draft(zipCode = "90001", center = center()),
                centerLookupState = CenterLookupState.MATCHED
            ),
            serviceAreaState = RegionalCenterServiceAreaState.Loading
        )

        composeRule.onNodeWithTag("regional_center_service_areas_loading").assertIsDisplayed()
        composeRule.onNodeWithTag("regional_center_map_surface").assertDoesNotExist()
    }

    @Test
    fun matchedCenter_serviceAreaErrorUsesStableFallbackInsteadOfMap() {
        compose(
            state(
                step = OnboardingStep.REGIONAL_CENTER,
                draft = draft(zipCode = "90001", center = center()),
                centerLookupState = CenterLookupState.MATCHED
            ),
            serviceAreaState = RegionalCenterServiceAreaState.Error
        )

        composeRule.onNodeWithTag("regional_center_service_areas_error").assertIsDisplayed()
        composeRule.onNodeWithTag("regional_center_map_surface").assertDoesNotExist()
    }

    @Test
    fun journey_showsItsHeadingAndExactlyOnePrimaryAction() {
        assertStep(
            state(step = OnboardingStep.JOURNEY, draft = draft(zipCode = "90001")),
            "Where are you in the journey?"
        )
    }

    @Test
    fun journey_usesIconLedSelectionCards() {
        compose(state(step = OnboardingStep.JOURNEY, draft = draft(zipCode = "90001")))

        composeRule.onAllNodesWithTag("onboarding_choice_icon", useUnmergedTree = true)
            .assertCountEquals(JourneyStage.entries.size)
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
    fun age_usesIconLedSelectionCards() {
        compose(
            state(
                step = OnboardingStep.AGE,
                draft = draft(zipCode = "90001", journey = JourneyStage.EXPLORING)
            )
        )

        composeRule.onAllNodesWithTag("onboarding_choice_icon", useUnmergedTree = true)
            .assertCountEquals(AgeGroup.entries.size)
    }

    @Test
    fun shortZip_disablesContinue() {
        compose(state(step = OnboardingStep.ZIP, draft = draft(zipCode = "900")))

        composeRule.onNodeWithTag("onboarding_primary_action").assertIsNotEnabled()
    }

    @Test
    fun zipLookupLoading_showsProgressBeforeCenterNavigation() {
        compose(
            state(
                step = OnboardingStep.ZIP,
                draft = draft(zipCode = "90001"),
                centerLookupState = CenterLookupState.LOADING
            )
        )

        composeRule.onNodeWithTag("onboarding_zip_lookup_loading").assertExists()
        composeRule.onNodeWithTag("onboarding_primary_action").assertIsNotEnabled()
        composeRule.onNodeWithTag("onboarding_use_location").assertIsNotEnabled()
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
    fun ageSelection_usesTheSameRadioRoleAsItsVisualIndicator() {
        compose(
            state(
                step = OnboardingStep.AGE,
                draft = draft(
                    zipCode = "90001",
                    journey = JourneyStage.EXPLORING,
                    age = AgeGroup.SCHOOL_AGE
                )
            )
        )

        val role = composeRule.onNodeWithTag("onboarding_age_school_age")
            .fetchSemanticsNode().config.getOrNull(SemanticsProperties.Role)
        assertEquals(Role.RadioButton, role)
    }

    @Test
    fun savingEdit_disablesCancelBackAndPrimaryNavigation() {
        compose(
            state(
                mode = OnboardingMode.EDIT,
                step = OnboardingStep.AGE,
                draft = draft(zipCode = "90001", journey = JourneyStage.EXPLORING),
                isSaving = true
            )
        )

        composeRule.onNodeWithTag("onboarding_cancel_action").assertIsNotEnabled()
        composeRule.onNodeWithTag("onboarding_back_action").assertIsNotEnabled()
        composeRule.onNodeWithTag("onboarding_primary_action").assertIsNotEnabled()
    }

    @Test
    fun onboardingBackGuard_consumesWhileSaving_thenRoutesToPriorStep() {
        var fallbackBackCalls = 0
        var routedBackCalls = 0
        val fallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                fallbackBackCalls += 1
            }
        }
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.addCallback(fallback)
        }
        val isSaving = mutableStateOf(true)
        composeRule.setContent {
            OnboardingBackGuard(
                state = state(
                    step = OnboardingStep.AGE,
                    draft = draft(zipCode = "90001", journey = JourneyStage.EXPLORING),
                    isSaving = isSaving.value
                ),
                onBack = { routedBackCalls += 1 },
                onClose = {}
            )
        }

        composeRule.runOnIdle {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        assertEquals(0, fallbackBackCalls)
        assertEquals(0, routedBackCalls)

        composeRule.runOnIdle { isSaving.value = false }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }

        assertEquals(0, fallbackBackCalls)
        assertEquals(1, routedBackCalls)
        fallback.remove()
    }

    @Test
    fun routeDisposalDuringRecreation_doesNotResetRetainedViewModelDraft() {
        val viewModel = onboardingViewModel()
        val showRoute = mutableStateOf(true)
        composeRule.setContent {
            if (showRoute.value) {
                OnboardingRoute(
                    mode = OnboardingMode.FIRST_RUN,
                    initialProfile = UserProfile(),
                    viewModel = viewModel
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            viewModel.continueFromCurrentStep()
            viewModel.onZipChanged("90001")
        }
        assertEquals(OnboardingStep.ZIP, viewModel.uiState.value.step)

        composeRule.runOnIdle { showRoute.value = false }
        composeRule.waitForIdle()

        assertEquals(OnboardingStep.ZIP, viewModel.uiState.value.step)
        assertEquals("90001", viewModel.uiState.value.draft.zipCode)

        composeRule.runOnIdle { showRoute.value = true }
        composeRule.waitForIdle()
        assertEquals(OnboardingStep.ZIP, viewModel.uiState.value.step)
        assertEquals("90001", viewModel.uiState.value.draft.zipCode)
    }

    @Test
    fun completedSession_firstReusedRouteFrameIsReset_andSavedEventIsDelivered() {
        val viewModel = onboardingViewModel()
        composeRule.runOnIdle {
            viewModel.initialize(OnboardingMode.FIRST_RUN, UserProfile())
            viewModel.continueFromCurrentStep()
            viewModel.onZipChanged("90001")
            viewModel.continueFromCurrentStep()
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            viewModel.continueFromCurrentStep()
            viewModel.selectJourney(JourneyStage.EXPLORING)
            viewModel.continueFromCurrentStep()
            viewModel.finish()
        }
        composeRule.waitForIdle()

        val renderedSteps = mutableListOf<OnboardingStep>()
        var savedCalls = 0
        composeRule.setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            SideEffect { renderedSteps += state.step }
            OnboardingRoute(
                mode = OnboardingMode.FIRST_RUN,
                initialProfile = UserProfile(),
                onSaved = { savedCalls += 1 },
                viewModel = viewModel
            )
        }
        composeRule.waitForIdle()

        assertEquals(OnboardingStep.AUDIENCE, renderedSteps.first())
        assertEquals(1, savedCalls)
    }

    @Test
    fun systemBack_routesByModeStepAndSavingState() {
        var fallbackBackCalls = 0
        val fallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                fallbackBackCalls += 1
            }
        }
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.addCallback(fallback)
        }

        val firstRunViewModel = onboardingViewModel()
        val editViewModel = onboardingViewModel()
        val audienceViewModel = onboardingViewModel()
        val routeCase = mutableStateOf(0)
        var closeCalls = 0
        composeRule.setContent {
            when (routeCase.value) {
                0 -> OnboardingRoute(
                    mode = OnboardingMode.FIRST_RUN,
                    initialProfile = UserProfile(),
                    viewModel = firstRunViewModel
                )
                1 -> OnboardingRoute(
                    mode = OnboardingMode.EDIT,
                    initialProfile = draft(
                        zipCode = "90001",
                        journey = JourneyStage.EXPLORING
                    ),
                    onClose = { closeCalls += 1 },
                    viewModel = editViewModel
                )
                else -> OnboardingRoute(
                    mode = OnboardingMode.FIRST_RUN,
                    initialProfile = UserProfile(),
                    viewModel = audienceViewModel
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle { firstRunViewModel.continueFromCurrentStep() }
        assertEquals(OnboardingStep.ZIP, firstRunViewModel.uiState.value.step)

        composeRule.runOnIdle {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()

        assertEquals(OnboardingStep.AUDIENCE, firstRunViewModel.uiState.value.step)
        assertEquals(0, fallbackBackCalls)

        composeRule.runOnIdle { routeCase.value = 1 }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()

        assertEquals(1, closeCalls)
        assertEquals(0, fallbackBackCalls)

        composeRule.runOnIdle { routeCase.value = 2 }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }

        assertEquals(1, fallbackBackCalls)
        fallback.remove()
    }

    @Test
    fun savingZip_disablesUseLocation() {
        compose(
            state(
                step = OnboardingStep.ZIP,
                draft = draft(zipCode = "90001"),
                isSaving = true
            )
        )

        composeRule.onNodeWithTag("onboarding_use_location").assertIsNotEnabled()
    }

    @Test
    fun locatingZip_disablesUseLocation() {
        compose(
            state(
                step = OnboardingStep.ZIP,
                locationState = LocationState.LOCATING
            )
        )

        composeRule.onNodeWithTag("onboarding_use_location").assertIsNotEnabled()
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
        composeRule.onNodeWithTag("onboarding_use_location")
            .assertExists()
            .assertHasClickAction()
            .assertIsEnabled()
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

    private fun compose(
        state: OnboardingUiState,
        serviceAreaState: RegionalCenterServiceAreaState =
            RegionalCenterServiceAreaState.Success(emptyList())
    ) {
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
                    onCancel = {},
                    serviceAreaState = serviceAreaState,
                    mapContent = { _, _ -> Box(Modifier.fillMaxSize()) }
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
        locationState: LocationState = LocationState.IDLE,
        isSaving: Boolean = false
    ) = OnboardingUiState(
        mode = mode,
        step = step,
        draft = draft,
        centerLookupState = centerLookupState,
        locationState = locationState,
        isSaving = isSaving
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

    private fun onboardingViewModel(): OnboardingViewModel = OnboardingViewModel(
        profileRepository = object : UserProfileRepository {
            private val profiles = MutableStateFlow(UserProfile())
            override val profile: Flow<UserProfile> = profiles
            override suspend fun replaceProfile(profile: UserProfile) {
                profiles.value = profile
            }
            override suspend fun replaceProfileIfCurrent(
                expected: UserProfile,
                replacement: UserProfile
            ): Boolean {
                if (profiles.value != expected) return false
                profiles.value = replacement
                return true
            }
            override suspend fun clearProfile() {
                profiles.value = UserProfile()
            }
        },
        regionalCenterDataSource = object : RegionalCenterDataSource {
            override suspend fun getRegionalCenters() =
                Result.success(emptyList<RegionalCenter>())
            override suspend fun getRegionalCentersNearby(latitude: Double, longitude: Double) =
                Result.success(emptyList<RegionalCenter>())
            override suspend fun lookupRegionalCenter(zipCode: String) =
                RegionalCenterLookup.Unmatched
        },
        userLocationSource = object : UserLocationSource {
            override fun hasLocationPermission() = false
            override suspend fun currentCoordinates(): UserCoordinates? = null
            override suspend fun zipCodeFor(coordinates: UserCoordinates): String? = null
        }
    )
}
