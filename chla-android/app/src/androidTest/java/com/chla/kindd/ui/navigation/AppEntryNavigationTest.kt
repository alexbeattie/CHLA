package com.chla.kindd.ui.navigation

import android.content.res.Configuration
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
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import com.chla.kindd.R
import com.chla.kindd.data.profile.AudienceType
import com.chla.kindd.data.profile.JourneyStage
import com.chla.kindd.data.profile.RegionalCenterIdentity
import com.chla.kindd.data.profile.UserProfile
import com.chla.kindd.ui.app.AppEntryState
import com.chla.kindd.ui.chat.ChatLaunchPrompt
import com.chla.kindd.ui.theme.KINDDTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Locale

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
    fun readyProfileUpdatesReachHomeForSameAndDifferentCenterIdentities() {
        var state by mutableStateOf<AppEntryState>(
            AppEntryState.Ready(
                completeProfile().copy(
                    regionalCenter = RegionalCenterIdentity(7, "South Central", "SCLARC")
                )
            )
        )
        lateinit var navController: TestNavHostController

        composeRule.setContent {
            navController = testNavController()
            KINDDTheme {
                KINDDRootContent(
                    state = state,
                    onboardingContent = {},
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

        composeRule.onNodeWithTag(READY_PROFILE_TAG)
            .assertTextEquals("90001|SCLARC|EXPLORING")

        composeRule.runOnIdle {
            val current = (state as AppEntryState.Ready).profile
            state = AppEntryState.Ready(
                current.copy(zipCode = "90210", journeyStage = JourneyStage.WAITING_FOR_INTAKE)
            )
        }
        composeRule.onNodeWithTag(READY_PROFILE_TAG)
            .assertTextEquals("90210|SCLARC|WAITING_FOR_INTAKE")

        composeRule.runOnIdle {
            val current = (state as AppEntryState.Ready).profile
            state = AppEntryState.Ready(
                current.copy(
                    regionalCenter = RegionalCenterIdentity(9, "Westside", "WRC")
                )
            )
        }
        composeRule.onNodeWithTag(READY_PROFILE_TAG)
            .assertTextEquals("90210|WRC|WAITING_FOR_INTAKE")
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

    @Test
    fun mainDestinationContent_exposesOnlyThePromptAwareChatContract() {
        val chatMethods = MainDestinationContent::class.java.declaredMethods
            .filter { method -> method.name == "chat" }

        assertEquals(1, chatMethods.size)
        assertTrue(chatMethods.single().parameterTypes.contains(ChatLaunchPrompt::class.java))
    }

    @Test
    fun typedChatRoutesDecodeEveryFixedKey_andSelectChatByDestinationPattern() {
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

        ChatLaunchPrompt.entries.forEach { prompt ->
            composeRule.runOnIdle {
                navController.navigate(Screen.Chat.createRoute(prompt))
            }

            composeRule.onNodeWithTag(CHAT_TAG).assertTextEquals(prompt.routeValue)
            composeRule.onNodeWithTag(BOTTOM_CHAT_TAG).assertIsSelected()
            composeRule.runOnIdle {
                assertEquals(Screen.Chat.destinationRoute, navController.currentDestination?.route)
                assertEquals(
                    prompt.routeValue,
                    navController.currentBackStackEntry?.arguments?.getString("prompt")
                )
                assertFalse(
                    Screen.Chat.createRoute(prompt).contains(
                        localizedString(prompt.promptResId, Locale.ENGLISH)
                    )
                )
            }
        }
    }

    @Test
    fun bottomNavigationChatDeliversNoPrompt_andStillSelectsPatternDestination() {
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

        composeRule.onNodeWithTag(BOTTOM_CHAT_TAG).performClick()

        composeRule.onNodeWithTag(CHAT_TAG).assertTextEquals(NO_PROMPT_TEXT)
        composeRule.onNodeWithTag(BOTTOM_CHAT_TAG).assertIsSelected()
        composeRule.runOnIdle {
            assertEquals(Screen.Chat.destinationRoute, navController.currentDestination?.route)
            assertNull(navController.currentBackStackEntry?.arguments?.getString("prompt"))
        }
    }

    @Test
    fun promptResourcesResolveToExactEnglish_andNaturalSpanishCounterparts() {
        val expectedEnglish = listOf(
            "We just got a diagnosis. What do I say when I call my regional center to request an intake evaluation for my child?",
            "How do we prepare for our regional center intake appointment? What documents and information should we bring?",
            "My child already receives regional center services. How do I prepare for an IPP meeting, and what services can I ask for?"
        )
        val expectedSpanish = listOf(
            "Acabamos de recibir un diagnóstico. ¿Qué debo decir cuando llame a mi centro regional para solicitar una evaluación inicial para mi hijo?",
            "¿Cómo nos preparamos para la cita de evaluación inicial del centro regional? ¿Qué documentos e información debemos llevar?",
            "Mi hijo ya recibe servicios del centro regional. ¿Cómo me preparo para una reunión del IPP y qué servicios puedo solicitar?"
        )

        assertEquals(
            expectedEnglish,
            ChatLaunchPrompt.entries.map { prompt ->
                localizedString(prompt.promptResId, Locale.ENGLISH)
            }
        )
        assertEquals(
            expectedSpanish,
            ChatLaunchPrompt.entries.map { prompt ->
                localizedString(prompt.promptResId, Locale.forLanguageTag("es"))
            }
        )
        assertEquals(
            listOf(
                R.string.chat_prompt_just_diagnosed,
                R.string.chat_prompt_waiting_intake,
                R.string.chat_prompt_receiving_services
            ),
            ChatLaunchPrompt.entries.map(ChatLaunchPrompt::promptResId)
        )
    }

    @Test
    fun launchLoadingDescriptionHasExactEnglishAndSpanishResources() {
        assertEquals(
            "Loading KiNDD",
            localizedString(R.string.app_entry_loading_content_description, Locale.ENGLISH)
        )
        assertEquals(
            "Cargando KiNDD",
            localizedString(
                R.string.app_entry_loading_content_description,
                Locale.forLanguageTag("es")
            )
        )
    }

    private fun localizedString(resourceId: Int, locale: Locale): String {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(locale)
        }
        return context.createConfigurationContext(configuration).getString(resourceId)
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
            Text(
                text = listOf(
                    profile.zipCode.orEmpty(),
                    profile.regionalCenter?.shortName.orEmpty(),
                    profile.journeyStage?.name.orEmpty()
                ).joinToString("|"),
                modifier = Modifier.testTag(READY_PROFILE_TAG)
            )
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
        override fun chat(prompt: ChatLaunchPrompt?, actions: MainNavActions) {
            Text(
                text = prompt?.routeValue ?: NO_PROMPT_TEXT,
                modifier = Modifier.testTag(CHAT_TAG)
            )
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
        const val READY_PROFILE_TAG = "ready_profile_destination_value"
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
        const val BOTTOM_CHAT_TAG = "bottom_nav_chat"
        const val NO_PROMPT_TEXT = "no_prompt"
    }
}

@Composable
private fun TaggedDestination(tag: String) {
    Text(text = tag, modifier = Modifier.testTag(tag))
}
