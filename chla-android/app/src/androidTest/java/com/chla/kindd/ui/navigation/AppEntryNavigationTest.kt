package com.chla.kindd.ui.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import com.chla.kindd.data.profile.AudienceType
import com.chla.kindd.data.profile.JourneyStage
import com.chla.kindd.data.profile.UserProfile
import com.chla.kindd.ui.app.AppEntryState
import com.chla.kindd.ui.theme.KINDDTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class AppEntryNavigationTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loading_neverComposesHome() {
        composeRule.setContent {
            KINDDTheme {
                KINDDRootContent(
                    state = AppEntryState.Loading,
                    onboardingContent = { TaggedDestination(ONBOARDING_TAG) },
                    mainContent = { TaggedDestination(HOME_TAG) }
                )
            }
        }

        composeRule.onNodeWithTag(HOME_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(BOTTOM_HOME_TAG).assertDoesNotExist()
    }

    @Test
    fun needsOnboarding_composesOnboardingWithoutBottomBar() {
        composeRule.setContent {
            KINDDTheme {
                KINDDRootContent(
                    state = AppEntryState.NeedsOnboarding(UserProfile()),
                    onboardingContent = { TaggedDestination(ONBOARDING_TAG) },
                    mainContent = { TaggedDestination(HOME_TAG) }
                )
            }
        }

        composeRule.onNodeWithTag(ONBOARDING_TAG).assertExists()
        composeRule.onNodeWithTag(HOME_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(BOTTOM_HOME_TAG).assertDoesNotExist()
    }

    @Test
    fun ready_composesMainGraph() {
        lateinit var navController: TestNavHostController

        composeRule.setContent {
            navController = testNavController()
            KINDDTheme {
                KINDDRootContent(
                    state = AppEntryState.Ready(completeProfile()),
                    onboardingContent = { TaggedDestination(ONBOARDING_TAG) },
                    mainContent = { profile ->
                        KINDDMainNavHost(
                            profile = profile,
                            navController = navController,
                            destinationContent = TaggedMainDestinationContent
                        )
                    }
                )
            }
        }

        composeRule.onNodeWithTag(HOME_TAG).assertExists()
        composeRule.onNodeWithTag(ONBOARDING_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(BOTTOM_HOME_TAG).assertExists()
    }

    @Test
    fun readyToNeedsOnboarding_disposesMainGraph() {
        var state by mutableStateOf<AppEntryState>(AppEntryState.Ready(completeProfile()))
        var mainDisposeCount by mutableIntStateOf(0)

        composeRule.setContent {
            KINDDTheme {
                KINDDRootContent(
                    state = state,
                    onboardingContent = { TaggedDestination(ONBOARDING_TAG) },
                    mainContent = {
                        DisposableEffect(Unit) {
                            onDispose { mainDisposeCount += 1 }
                        }
                        TaggedDestination(HOME_TAG)
                    }
                )
            }
        }
        composeRule.onNodeWithTag(HOME_TAG).assertExists()

        composeRule.runOnIdle {
            state = AppEntryState.NeedsOnboarding(UserProfile())
        }

        composeRule.onNodeWithTag(HOME_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(ONBOARDING_TAG).assertExists()
        composeRule.runOnIdle { assertEquals(1, mainDisposeCount) }
    }

    @Test
    fun needsOnboardingToReady_startsNewMainGraphAtHome() {
        var state by mutableStateOf<AppEntryState>(
            AppEntryState.NeedsOnboarding(UserProfile())
        )
        lateinit var navController: TestNavHostController

        composeRule.setContent {
            navController = testNavController()
            KINDDTheme {
                KINDDRootContent(
                    state = state,
                    onboardingContent = { TaggedDestination(ONBOARDING_TAG) },
                    mainContent = { profile ->
                        KINDDMainNavHost(
                            profile = profile,
                            navController = navController,
                            destinationContent = TaggedMainDestinationContent
                        )
                    }
                )
            }
        }
        composeRule.onNodeWithTag(ONBOARDING_TAG).assertExists()

        composeRule.runOnIdle {
            state = AppEntryState.Ready(completeProfile())
        }

        composeRule.onNodeWithTag(HOME_TAG).assertExists()
        composeRule.runOnIdle {
            assertEquals(Screen.Home.route, navController.currentDestination?.route)
        }
    }

    @Test
    fun mainNavHost_usesRealRoutesAndNeverConstructsOnboarding() {
        lateinit var navController: TestNavHostController

        composeRule.setContent {
            navController = testNavController()
            KINDDTheme {
                KINDDMainNavHost(
                    profile = completeProfile(),
                    navController = navController,
                    destinationContent = TaggedMainDestinationContent
                )
            }
        }

        composeRule.onNodeWithTag(HOME_TAG).assertExists()
        composeRule.runOnIdle {
            assertEquals(Screen.Home.route, navController.currentDestination?.route)
        }

        composeRule.onNodeWithTag(BOTTOM_LIST_TAG).performClick()
        composeRule.onNodeWithTag(LIST_TAG).assertExists()
        composeRule.runOnIdle {
            assertEquals(Screen.Providers.route, navController.currentDestination?.route)
        }

        composeRule.onNodeWithTag(BOTTOM_MAP_TAG).performClick()
        composeRule.onNodeWithTag(MAP_TAG).assertExists()
        composeRule.runOnIdle {
            assertEquals(Screen.Map.route, navController.currentDestination?.route)
        }

        composeRule.onNodeWithTag(BOTTOM_LIST_TAG).performClick()
        composeRule.onNodeWithTag(LIST_TAG).assertExists()
        composeRule.runOnIdle {
            assertEquals(Screen.Providers.route, navController.currentDestination?.route)
            assertNull(navController.graph.findNode("onboarding"))
        }
    }

    @Composable
    private fun testNavController(): TestNavHostController {
        val context = LocalContext.current
        return remember {
            TestNavHostController(context).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
        }
    }

    private fun completeProfile() = UserProfile(
        onboardingCompleted = true,
        audienceType = AudienceType.FAMILY,
        zipCode = "90001",
        journeyStage = JourneyStage.EXPLORING
    )

    private object TaggedMainDestinationContent : MainDestinationContent {
        @Composable
        override fun home(profile: UserProfile, actions: MainNavActions) {
            TaggedDestination(HOME_TAG)
        }

        @Composable
        override fun map(actions: MainNavActions) {
            TaggedDestination(MAP_TAG)
        }

        @Composable
        override fun list(actions: MainNavActions) {
            TaggedDestination(LIST_TAG)
        }

        @Composable
        override fun chat(actions: MainNavActions) {
            TaggedDestination(CHAT_TAG)
        }

        @Composable
        override fun settings(actions: MainNavActions) {
            TaggedDestination(SETTINGS_TAG)
        }

        @Composable
        override fun providerDetail(providerId: String, actions: MainNavActions) {
            TaggedDestination("provider_$providerId")
        }

        @Composable
        override fun regions(actions: MainNavActions) {
            TaggedDestination(REGIONS_TAG)
        }

        @Composable
        override fun faq(actions: MainNavActions) {
            TaggedDestination(FAQ_TAG)
        }

        @Composable
        override fun about(actions: MainNavActions) {
            TaggedDestination(ABOUT_TAG)
        }

        @Composable
        override fun editProfile(profile: UserProfile, actions: MainNavActions) {
            TaggedDestination(EDIT_PROFILE_TAG)
        }
    }

    private companion object {
        const val HOME_TAG = "fake_home_destination"
        const val ONBOARDING_TAG = "fake_onboarding_destination"
        const val MAP_TAG = "fake_map_destination"
        const val LIST_TAG = "fake_list_destination"
        const val CHAT_TAG = "fake_chat_destination"
        const val SETTINGS_TAG = "fake_settings_destination"
        const val REGIONS_TAG = "fake_regions_destination"
        const val FAQ_TAG = "fake_faq_destination"
        const val ABOUT_TAG = "fake_about_destination"
        const val EDIT_PROFILE_TAG = "fake_edit_profile_destination"
        const val BOTTOM_HOME_TAG = "bottom_nav_home"
        const val BOTTOM_MAP_TAG = "bottom_nav_map"
        const val BOTTOM_LIST_TAG = "bottom_nav_list"
    }
}

@Composable
private fun TaggedDestination(tag: String) {
    Text(text = tag, modifier = Modifier.testTag(tag))
}
