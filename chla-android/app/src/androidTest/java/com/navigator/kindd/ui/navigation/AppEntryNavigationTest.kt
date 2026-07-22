package com.navigator.kindd.ui.navigation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import androidx.test.platform.app.InstrumentationRegistry
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import com.navigator.kindd.R
import com.navigator.kindd.data.profile.AudienceType
import com.navigator.kindd.data.profile.JourneyStage
import com.navigator.kindd.data.profile.RegionalCenterIdentity
import com.navigator.kindd.data.profile.UserProfile
import com.navigator.kindd.ui.app.AppEntryState
import com.navigator.kindd.ui.chat.ChatLaunchPrompt
import com.navigator.kindd.ui.theme.KINDDTheme
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
    fun floatingNavigation_exposesSixIconOnlyLocalizedActions_withMinimumTouchBounds() {
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

        val expectedActions = listOf(
            BOTTOM_HOME_TAG to localizedString(R.string.nav_home, Locale.ENGLISH),
            BOTTOM_MAP_TAG to localizedString(R.string.nav_map, Locale.ENGLISH),
            BOTTOM_ASK_TAG to "Ask KiNDD",
            BOTTOM_REGIONS_TAG to "Regions",
            BOTTOM_LIST_TAG to localizedString(R.string.nav_list, Locale.ENGLISH),
            BOTTOM_MORE_TAG to "More"
        )
        val minimumPixels = 48f * composeRule.density.density

        expectedActions.forEach { (tag, label) ->
            val node = composeRule.onNodeWithTag(tag)
                .assertExists()
                .assertContentDescriptionEquals(label)
                .fetchSemanticsNode()
            assertTrue("$tag is narrower than 48dp", node.boundsInRoot.width >= minimumPixels)
            assertTrue("$tag is shorter than 48dp", node.boundsInRoot.height >= minimumPixels)
            assertTrue(
                "$tag exposes a visible navigation text label",
                node.config.getOrNull(SemanticsProperties.Text).orEmpty().isEmpty()
            )
        }

        composeRule.onNodeWithTag(BOTTOM_HOME_TAG).assertIsSelected()
        composeRule.runOnIdle {
            assertEquals(Screen.Home.route, navController.currentDestination?.route)
        }
    }

    @Test
    fun askAction_usesNeutralGlassContainerWithAColoredGradientGlyph() {
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

        val pixels = composeRule.onNodeWithTag(BOTTOM_ASK_TAG)
            .captureToImage()
            .toPixelMap()
        val containerSample = pixels[pixels.width / 6, pixels.height / 2]
        val containerChannels = listOf(
            containerSample.red,
            containerSample.green,
            containerSample.blue
        )
        assertTrue(
            "Ask container must remain neutral glass instead of a filled color circle",
            containerChannels.max() - containerChannels.min() < 0.12f
        )

        val centerColors = buildList {
            for (x in pixels.width / 3 until pixels.width * 2 / 3) {
                for (y in pixels.height / 3 until pixels.height * 2 / 3) {
                    add(pixels[x, y])
                }
            }
        }
        assertTrue(
            "Ask glyph must contain violet-to-pink color rather than only white",
            centerColors.any { color ->
                val channels = listOf(color.red, color.green, color.blue)
                channels.max() - channels.min() > 0.18f
            }
        )
    }

    @Test
    fun floatingNavigation_isNarrowerThanRoot_andOverlaysDestinationContent() {
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

        val rootBounds = composeRule.onNodeWithTag(MAIN_NAV_ROOT_TAG)
            .fetchSemanticsNode().boundsInRoot
        val capsuleBounds = composeRule.onNodeWithTag(FLOATING_NAV_CAPSULE_TAG)
            .fetchSemanticsNode().boundsInRoot

        assertTrue("Floating navigation must not consume the full width", capsuleBounds.width < rootBounds.width)
        assertTrue("Floating navigation must sit inside the content root", capsuleBounds.bottom <= rootBounds.bottom)
        composeRule.onNodeWithTag(HOME_TAG).assertExists()
    }

    @Test
    fun floatingNavigation_reservesBottomClearance_butHomeAndMapOwnTheirFullHeightInsets() {
        lateinit var navController: TestNavHostController
        val clearanceContent = object : MainDestinationContent by TaggedMainDestinationContent {
            @Composable
            override fun home(profile: UserProfile, actions: MainNavActions) {
                DestinationWithBottomControl(HOME_TAG, HOME_BOTTOM_CONTROL_TAG)
            }

            @Composable
            override fun list(actions: MainNavActions) {
                DestinationWithBottomControl(LIST_TAG, LIST_BOTTOM_CONTROL_TAG)
            }

            @Composable
            override fun regions(actions: MainNavActions) {
                DestinationWithBottomControl(REGIONS_TAG, REGIONS_BOTTOM_CONTROL_TAG)
            }

            @Composable
            override fun settings(actions: MainNavActions) {
                DestinationWithBottomControl(SETTINGS_TAG, SETTINGS_BOTTOM_CONTROL_TAG)
            }

            @Composable
            override fun more(actions: MainNavActions) {
                DestinationWithBottomControl(MORE_TAG, MORE_BOTTOM_CONTROL_TAG)
            }

            @Composable
            override fun map(actions: MainNavActions) {
                Box(Modifier.fillMaxSize().testTag(MAP_FULL_BLEED_TAG))
            }
        }

        composeRule.setContent {
            navController = testNavController()
            KINDDTheme {
                KINDDMainNavHost(
                    profile = completeProfile(),
                    navController = navController,
                    destinationContent = clearanceContent
                )
            }
        }

        fun assertControlClearsFloatingNavigation(tag: String) {
            val controlBottom = composeRule.onNodeWithTag(tag)
                .fetchSemanticsNode().boundsInRoot.bottom
            val capsuleTop = composeRule.onNodeWithTag(FLOATING_NAV_CAPSULE_TAG)
                .fetchSemanticsNode().boundsInRoot.top
            assertTrue("$tag must finish above floating navigation", controlBottom <= capsuleTop)
        }

        val rootBottom = composeRule.onNodeWithTag(MAIN_NAV_ROOT_TAG)
            .fetchSemanticsNode().boundsInRoot.bottom
        val homeBottom = composeRule.onNodeWithTag(HOME_TAG)
            .fetchSemanticsNode().boundsInRoot.bottom
        assertEquals(rootBottom, homeBottom, 0.5f)

        composeRule.onNodeWithTag(BOTTOM_LIST_TAG).performClick()
        assertControlClearsFloatingNavigation(LIST_BOTTOM_CONTROL_TAG)
        composeRule.onNodeWithTag(BOTTOM_REGIONS_TAG).performClick()
        assertControlClearsFloatingNavigation(REGIONS_BOTTOM_CONTROL_TAG)
        composeRule.onNodeWithTag(BOTTOM_MORE_TAG).performClick()
        assertControlClearsFloatingNavigation(MORE_BOTTOM_CONTROL_TAG)
        composeRule.runOnIdle { navController.navigate(Screen.Settings.route) }
        composeRule.waitForIdle()
        assertControlClearsFloatingNavigation(SETTINGS_BOTTOM_CONTROL_TAG)

        composeRule.onNodeWithTag(BOTTOM_MAP_TAG).performClick()
        val mapBottom = composeRule.onNodeWithTag(MAP_FULL_BLEED_TAG)
            .fetchSemanticsNode().boundsInRoot.bottom
        assertEquals(rootBottom, mapBottom, 0.5f)
    }

    @Test
    fun regionsAndMore_useFirstClassRoutes_whilePrimaryDestinationsPreserveGraphState() {
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

        composeRule.onNodeWithTag(BOTTOM_REGIONS_TAG).performClick()
        composeRule.onNodeWithTag(REGIONS_TAG).assertExists()
        composeRule.runOnIdle {
            assertEquals(Screen.RegionalCenters.route, navController.currentDestination?.route)
        }

        composeRule.onNodeWithTag(BOTTOM_HOME_TAG).performClick()
        composeRule.onNodeWithTag(HOME_TAG).assertExists()
        composeRule.onNodeWithTag(BOTTOM_MAP_TAG).performClick()
        composeRule.onNodeWithTag(MAP_TAG).assertExists()
        composeRule.onNodeWithTag(BOTTOM_LIST_TAG).performClick()
        composeRule.onNodeWithTag(LIST_TAG).assertExists()

        composeRule.onNodeWithTag(BOTTOM_MORE_TAG).performClick()
        composeRule.onNodeWithTag(SETTINGS_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(MORE_TAG).assertExists()
        composeRule.runOnIdle {
            assertEquals("more", navController.currentDestination?.route)
            assertNull(navController.graph.findNode("onboarding"))
        }
    }

    @Test
    fun homeSettingsAction_usesTheExistingSettingsDestination() {
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

        composeRule.onNodeWithTag(HOME_SETTINGS_LAUNCH_TAG).performClick()
        composeRule.onNodeWithTag(SETTINGS_TAG).assertExists()
        composeRule.runOnIdle {
            assertEquals(Screen.Settings.route, navController.currentDestination?.route)
        }

        composeRule.onNodeWithTag(BOTTOM_MORE_TAG).assertIsSelected()
        pressBack()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(MORE_TAG).assertExists()
        composeRule.onNodeWithTag(BOTTOM_MORE_TAG).assertIsSelected()
    }

    @Test
    fun moreSelection_coversFaqAboutAndSettings_butRegionsKeepsItsOwnSelection() {
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

        listOf(Screen.FAQ.route, Screen.About.route, Screen.Settings.route).forEach { route ->
            composeRule.runOnIdle { navController.navigate(route) }
            composeRule.waitForIdle()
            composeRule.onNodeWithTag(BOTTOM_MORE_TAG).assertIsSelected()
        }

        composeRule.runOnIdle { navController.navigate(Screen.RegionalCenters.route) }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(BOTTOM_MORE_TAG).assertIsNotSelected()
        composeRule.onNodeWithTag(BOTTOM_REGIONS_TAG).assertIsSelected()
    }

    @Test
    fun ask_opensDismissibleChatSheet_overCurrentDestination() {
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

        composeRule.onNodeWithTag(BOTTOM_MAP_TAG).performClick()
        composeRule.onNodeWithTag(MAP_TAG).assertExists()
        composeRule.onNodeWithTag(BOTTOM_ASK_TAG).performClick()

        composeRule.onNodeWithTag(CHAT_SHEET_TAG).assertExists()
        composeRule.onNodeWithTag(CHAT_TAG).assertTextEquals(NO_PROMPT_TEXT)
        composeRule.onNodeWithTag(MAP_TAG).assertExists()
        val rootBounds = composeRule.onNodeWithTag(MAIN_NAV_ROOT_TAG)
            .fetchSemanticsNode().boundsInRoot
        val sheetBounds = composeRule.onNodeWithTag(CHAT_SHEET_TAG)
            .fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Ask should open near full height instead of stopping at the half-expanded anchor",
            sheetBounds.top <= rootBounds.height * 0.20f
        )
        composeRule.runOnIdle {
            assertEquals(Screen.Map.route, navController.currentDestination?.route)
        }

        pressBack()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(CHAT_SHEET_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(CHAT_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(MAP_TAG).assertExists()
    }

    @Test
    fun typedChatLaunchPrompt_opensSheet_deliversOnce_withoutPuttingTextInRoute() {
        lateinit var navController: TestNavHostController
        val deliveredPrompts = mutableListOf<ChatLaunchPrompt?>()
        val recordingContent = object : MainDestinationContent by TaggedMainDestinationContent {
            @Composable
            override fun chat(prompt: ChatLaunchPrompt?, actions: MainNavActions) {
                DisposableEffect(prompt) {
                    deliveredPrompts += prompt
                    onDispose { }
                }
                Text(
                    text = prompt?.routeValue ?: NO_PROMPT_TEXT,
                    modifier = Modifier.testTag(CHAT_TAG)
                )
            }
        }

        composeRule.setContent {
            navController = testNavController()
            KINDDTheme {
                KINDDMainNavHost(
                    profile = completeProfile(),
                    navController = navController,
                    destinationContent = recordingContent
                )
            }
        }

        composeRule.onNodeWithTag(TYPED_CHAT_LAUNCH_TAG).performClick()
        composeRule.onNodeWithTag(CHAT_SHEET_TAG).assertExists()
        composeRule.onNodeWithTag(CHAT_TAG)
            .assertTextEquals(ChatLaunchPrompt.JUST_DIAGNOSED.routeValue)
        composeRule.runOnIdle {
            assertEquals(listOf(ChatLaunchPrompt.JUST_DIAGNOSED), deliveredPrompts)
            assertEquals(Screen.Home.route, navController.currentDestination?.route)
            assertFalse(
                navController.currentDestination?.route.orEmpty().contains(
                    localizedString(
                        ChatLaunchPrompt.JUST_DIAGNOSED.promptResId,
                        Locale.ENGLISH
                    )
                )
            )
        }
    }

    @Test
    fun promptResourcesResolveToExactEnglish_andNaturalSpanishCounterparts() {
        val expectedEnglish = listOf(
            "We just got a diagnosis. What do I say when I call my regional center to request an intake evaluation for my child?",
            "We just got a diagnosis. What do we do first?",
            "How do we prepare for our regional center intake appointment? What documents and information should we bring?",
            "My child already receives regional center services. How do I prepare for an IPP meeting, and what services can I ask for?",
            "Find ABA therapy near me. Help me understand what to compare and what to ask providers.",
            "What services can my regional center help fund, and how do I ask for them?",
            "Which regional center serves my ZIP code, and what should I do next?"
        )
        val expectedSpanish = listOf(
            "Acabamos de recibir un diagnóstico. ¿Qué debo decir cuando llame a mi centro regional para solicitar una evaluación inicial para mi hijo?",
            "Acabamos de recibir un diagnóstico. ¿Qué hacemos primero?",
            "¿Cómo nos preparamos para la cita de evaluación inicial del centro regional? ¿Qué documentos e información debemos llevar?",
            "Mi hijo ya recibe servicios del centro regional. ¿Cómo me preparo para una reunión del IPP y qué servicios puedo solicitar?",
            "Encuentra terapia ABA cerca de mí. Ayúdame a saber qué comparar y qué preguntar a los proveedores.",
            "¿Qué servicios puede financiar mi centro regional y cómo los solicito?",
            "¿Qué centro regional atiende mi código postal y qué debo hacer después?"
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
                R.string.chat_prompt_first_steps,
                R.string.chat_prompt_waiting_intake,
                R.string.chat_prompt_receiving_services,
                R.string.chat_prompt_find_aba_nearby,
                R.string.chat_prompt_center_funding,
                R.string.chat_prompt_find_regional_center
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
            Column {
                TaggedDestination(HOME_TAG)
                Button(
                    onClick = { actions.navigateToChat(ChatLaunchPrompt.JUST_DIAGNOSED) },
                    modifier = Modifier.testTag(TYPED_CHAT_LAUNCH_TAG)
                ) {
                    Text("Open typed chat")
                }
                Button(
                    onClick = actions.navigateToSettings,
                    modifier = Modifier.testTag(HOME_SETTINGS_LAUNCH_TAG)
                ) {
                    Text("Open settings from Home")
                }
                Text(
                    text = listOf(
                        profile.zipCode.orEmpty(),
                        profile.regionalCenter?.shortName.orEmpty(),
                        profile.journeyStage?.name.orEmpty()
                    ).joinToString("|"),
                    modifier = Modifier.testTag(READY_PROFILE_TAG)
                )
            }
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
        override fun more(actions: MainNavActions) {
            TaggedDestination(MORE_TAG)
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
        const val MORE_TAG = "fake_more_destination"
        const val REGIONS_TAG = "fake_regions_destination"
        const val FAQ_TAG = "fake_faq_destination"
        const val ABOUT_TAG = "fake_about_destination"
        const val EDIT_PROFILE_TAG = "fake_edit_profile_destination"
        const val BOTTOM_HOME_TAG = "bottom_nav_home"
        const val BOTTOM_MAP_TAG = "bottom_nav_map"
        const val BOTTOM_LIST_TAG = "bottom_nav_list"
        const val BOTTOM_ASK_TAG = "bottom_nav_ask"
        const val BOTTOM_REGIONS_TAG = "bottom_nav_regions"
        const val BOTTOM_MORE_TAG = "bottom_nav_more"
        const val FLOATING_NAV_CAPSULE_TAG = "floating_nav_capsule"
        const val MAIN_NAV_ROOT_TAG = "main_nav_root"
        const val CHAT_SHEET_TAG = "chat_modal_sheet"
        const val TYPED_CHAT_LAUNCH_TAG = "launch_typed_chat"
        const val HOME_SETTINGS_LAUNCH_TAG = "launch_home_settings"
        const val NO_PROMPT_TEXT = "no_prompt"
        const val HOME_BOTTOM_CONTROL_TAG = "home_bottom_control"
        const val LIST_BOTTOM_CONTROL_TAG = "list_bottom_control"
        const val REGIONS_BOTTOM_CONTROL_TAG = "regions_bottom_control"
        const val SETTINGS_BOTTOM_CONTROL_TAG = "settings_bottom_control"
        const val MORE_BOTTOM_CONTROL_TAG = "more_bottom_control"
        const val MAP_FULL_BLEED_TAG = "map_full_bleed"
    }
}

@Composable
private fun TaggedDestination(tag: String) {
    Text(text = tag, modifier = Modifier.testTag(tag))
}

@Composable
private fun DestinationWithBottomControl(destinationTag: String, controlTag: String) {
    Box(Modifier.fillMaxSize().testTag(destinationTag)) {
        Button(
            onClick = {},
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .testTag(controlTag)
        ) {
            Text(controlTag)
        }
    }
}
