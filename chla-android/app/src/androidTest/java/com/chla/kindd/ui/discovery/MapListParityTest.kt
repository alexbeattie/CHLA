package com.chla.kindd.ui.discovery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import com.chla.kindd.data.discovery.DiscoveryController
import com.chla.kindd.data.discovery.DiscoveryCriteria
import com.chla.kindd.data.discovery.DiscoveryError
import com.chla.kindd.data.discovery.DiscoveryOrigin
import com.chla.kindd.data.discovery.DiscoveryState
import com.chla.kindd.data.discovery.TherapyType
import com.chla.kindd.data.models.Provider
import com.chla.kindd.data.profile.AgeGroup
import com.chla.kindd.data.profile.AudienceType
import com.chla.kindd.data.profile.JourneyStage
import com.chla.kindd.data.profile.RegionalCenterIdentity
import com.chla.kindd.data.profile.UserProfile
import com.chla.kindd.ui.chat.ChatLaunchPrompt
import com.chla.kindd.ui.navigation.KINDDMainNavHost
import com.chla.kindd.ui.navigation.MainDestinationContent
import com.chla.kindd.ui.navigation.MainNavActions
import com.chla.kindd.ui.navigation.Screen
import com.chla.kindd.ui.screens.MapContent
import com.chla.kindd.ui.screens.MapLocationState
import com.chla.kindd.ui.screens.MapLocationStatus
import com.chla.kindd.ui.screens.MapMarkerModel
import com.chla.kindd.ui.screens.ProviderListContent
import com.chla.kindd.ui.screens.ProviderListSort
import com.chla.kindd.ui.theme.KINDDTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MapListParityTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun realNavigation_preservesCriteriaAndProviderIdentityAcrossListAndMap() {
        val controller = FakeDiscoveryController(
            DiscoveryState(
                criteria = DiscoveryCriteria(
                    query = "shared query",
                    therapyTypes = setOf(TherapyType.ABA),
                    origin = DiscoveryOrigin.ProfileZip("90001")
                ),
                providers = listOf(
                    provider("one", 34.0, -118.0, listOf("ABA therapy")),
                    provider("two", 34.1, -118.1),
                    provider("three", null, null)
                ),
                hasLoadedOnce = true
            )
        )
        lateinit var navController: TestNavHostController
        val destinations = DiscoveryDestinations(controller)

        composeRule.setContent {
            navController = testNavController()
            KINDDTheme {
                KINDDMainNavHost(
                    profile = completeProfile(),
                    navController = navController,
                    destinationContent = destinations
                )
            }
        }

        composeRule.onNodeWithTag("bottom_nav_list").performClick()
        composeRule.onNodeWithTag("discovery_search_field")
            .assertTextContains("shared query")
        composeRule.onNodeWithTag("filter_chip_therapy_ABA").assertExists()
        listOf("one", "two", "three").forEach { id ->
            composeRule.onNodeWithTag("provider_$id").assertExists()
        }
        composeRule.onNodeWithTag(
            "provider_therapy_one_0",
            useUnmergedTree = true
        ).assertHasNoClickAction()
        composeRule.onNodeWithTag("provider_two").performClick()
        composeRule.runOnIdle {
            assertEquals(Screen.Providers.route, navController.currentDestination?.route)
            assertEquals("two", destinations.lastClickedProviderId)
            assertEquals(
                listOf("one", "two", "three"),
                controller.state.value.providers.map(Provider::id)
            )
        }

        composeRule.onNodeWithTag("bottom_nav_map").performClick()
        composeRule.onNodeWithTag("discovery_search_field")
            .assertTextContains("shared query")
        composeRule.onNodeWithTag("filter_chip_therapy_ABA").assertExists()
        composeRule.onNodeWithTag("map_marker_one").assertExists()
        composeRule.onNodeWithTag("map_marker_two").assertExists()
        composeRule.onNodeWithTag("map_marker_three").assertDoesNotExist()
        composeRule.onNodeWithTag("map_marker_one").performClick()
        composeRule.runOnIdle {
            assertEquals(Screen.Map.route, navController.currentDestination?.route)
            assertEquals("one", destinations.lastClickedProviderId)
            assertEquals(
                listOf("one", "two"),
                destinations.lastMarkerIds
            )
            assertEquals("shared query", controller.state.value.criteria.query)
        }

        composeRule.onNodeWithTag("map_result_count").performClick()
        composeRule.onNodeWithTag("filter_chip_therapy_ABA").assertExists()
        composeRule.runOnIdle {
            assertEquals(Screen.Providers.route, navController.currentDestination?.route)
            assertEquals(setOf(TherapyType.ABA), controller.state.value.criteria.therapyTypes)
            assertEquals(
                listOf("one", "two", "three"),
                controller.state.value.providers.map(Provider::id)
            )
        }
    }

    @Test
    fun useMyLocation_isDisabledWhileLocationLookupIsRunning() {
        composeRule.setContent {
            KINDDTheme {
                MapContent(
                    state = DiscoveryState(hasLoadedOnce = true),
                    locationState = MapLocationState(
                        hasPermission = true,
                        status = MapLocationStatus.LOCATING
                    ),
                    actions = discoveryActions(FakeDiscoveryController(DiscoveryState())),
                    onUseMyLocation = {},
                    onProviderClick = {},
                    onNavigateToList = {},
                    markerContent = { _, _ -> Unit }
                )
            }
        }

        composeRule.onNodeWithTag("map_use_location").assertIsNotEnabled()
    }

    @Test
    fun map_retainsImmersiveSurfaceAndCompactChromeAcrossEveryDiscoveryState() {
        val retainedProvider = provider("kept", 34.0, -118.0)
        var state by mutableStateOf(
            DiscoveryState(
                profile = completeProfile().copy(
                    regionalCenter = RegionalCenterIdentity(
                        id = 1,
                        name = "North Los Angeles County Regional Center",
                        shortName = "NLACRC"
                    )
                ),
                isLoading = true
            )
        )
        var locationState by mutableStateOf(MapLocationState())

        composeRule.setContent {
            KINDDTheme {
                MapContent(
                    state = state,
                    locationState = locationState,
                    actions = discoveryActions(FakeDiscoveryController(DiscoveryState())),
                    onUseMyLocation = {},
                    onProviderClick = {},
                    onNavigateToList = {},
                    markerContent = { _, _ ->
                        Box(Modifier.fillMaxSize().testTag("fake_map_surface"))
                    }
                )
            }
        }

        assertImmersiveMapChrome()
        composeRule.onNodeWithTag("map_loading_overlay").assertIsDisplayed()
        composeRule.onNodeWithTag("map_regional_center_badge").assertTextContains("NLACRC")

        composeRule.runOnIdle {
            state = state.copy(isLoading = false, hasLoadedOnce = true)
        }
        assertImmersiveMapChrome()
        composeRule.onNodeWithTag("discovery_empty").assertIsDisplayed()

        composeRule.runOnIdle {
            state = state.copy(
                providers = listOf(retainedProvider),
                isLoading = true,
                error = null
            )
        }
        assertImmersiveMapChrome()
        composeRule.onNodeWithTag("map_refresh_progress").assertIsDisplayed()
        composeRule.onNodeWithTag("map_result_count")
            .assertTextContains("1", substring = true)

        composeRule.runOnIdle {
            state = state.copy(isLoading = false, error = DiscoveryError.NETWORK)
        }
        assertImmersiveMapChrome()
        composeRule.onNodeWithTag("map_error_overlay").assertIsDisplayed()

        composeRule.runOnIdle {
            locationState = MapLocationState(status = MapLocationStatus.PERMISSION_DENIED)
        }
        assertImmersiveMapChrome()
        composeRule.onNodeWithTag("map_location_status").assertIsDisplayed()
    }

    @Test
    fun map_compactControlsFireOnceAndExposeActiveFilterCount() {
        var locationClicks = 0
        var refreshClicks = 0
        val controller = FakeDiscoveryController(DiscoveryState())
        val actions = discoveryActions(controller).copy(onRefresh = { refreshClicks += 1 })

        composeRule.setContent {
            KINDDTheme {
                MapContent(
                    state = DiscoveryState(
                        criteria = DiscoveryCriteria(
                            therapyTypes = setOf(TherapyType.ABA),
                            ageGroup = AgeGroup.SCHOOL_AGE
                        ),
                        hasLoadedOnce = true
                    ),
                    locationState = MapLocationState(),
                    actions = actions,
                    onUseMyLocation = { locationClicks += 1 },
                    onProviderClick = {},
                    onNavigateToList = {},
                    markerContent = { _, _ -> Unit }
                )
            }
        }

        composeRule.onNodeWithTag("map_title").assertDoesNotExist()
        composeRule.onNodeWithTag("map_filter_badge").assertTextContains("2")
        composeRule.onNodeWithTag("map_use_location").performClick()
        composeRule.onNodeWithTag("map_refresh").performClick()
        composeRule.runOnIdle {
            assertEquals(1, locationClicks)
            assertEquals(1, refreshClicks)
        }
    }

    @Test
    fun map_resultCountMatchesPlottedProvidersAndNavigatesOnceAsA48DpButton() {
        var listClicks = 0
        var plottedProviderIds: List<String> = emptyList()
        val valid = provider("mapped", 34.0, -118.0)
        val invalid = provider("list-only", null, null)

        composeRule.setContent {
            KINDDTheme {
                MapContent(
                    state = DiscoveryState(
                        providers = listOf(valid, invalid),
                        hasLoadedOnce = true
                    ),
                    locationState = MapLocationState(),
                    actions = discoveryActions(FakeDiscoveryController(DiscoveryState())),
                    onUseMyLocation = {},
                    onProviderClick = {},
                    onNavigateToList = { listClicks += 1 },
                    markerContent = { markers, _ ->
                        plottedProviderIds = markers.map(MapMarkerModel::providerId)
                    }
                )
            }
        }

        val resultCount = composeRule.onNodeWithTag("map_result_count")
            .assertIsDisplayed()
            .assertTextContains("1", substring = true)
            .assertContentDescriptionEquals(
                "1 resource shown on the map. View matching resources in the list"
            )
            .assertHasClickAction()
        val semanticsNode = resultCount.fetchSemanticsNode()
        val minimumPixels = 48f * composeRule.density.density

        assertEquals(Role.Button, semanticsNode.config.getOrNull(SemanticsProperties.Role))
        assertTrue(semanticsNode.boundsInRoot.height >= minimumPixels)
        assertTrue(semanticsNode.boundsInRoot.width >= minimumPixels)

        resultCount.performClick()
        composeRule.runOnIdle {
            assertEquals(listOf("mapped"), plottedProviderIds)
            assertEquals(1, listClicks)
        }
    }

    @Test
    fun map_listOnlyResultsKeepATruthfulRouteToTheProviderList() {
        var listClicks = 0

        composeRule.setContent {
            KINDDTheme {
                MapContent(
                    state = DiscoveryState(
                        providers = listOf(provider("list-only", null, null)),
                        hasLoadedOnce = true
                    ),
                    locationState = MapLocationState(),
                    actions = discoveryActions(FakeDiscoveryController(DiscoveryState())),
                    onUseMyLocation = {},
                    onProviderClick = {},
                    onNavigateToList = { listClicks += 1 },
                    markerContent = { markers, _ -> assertTrue(markers.isEmpty()) }
                )
            }
        }

        composeRule.onNodeWithTag("map_result_count")
            .assertIsDisplayed()
            .assertTextContains("0", substring = true)
            .assertContentDescriptionEquals(
                "0 resources shown on the map. View matching resources in the list"
            )
            .assertHasClickAction()
            .performClick()
        composeRule.runOnIdle { assertEquals(1, listClicks) }
    }

    @Test
    fun providerList_usesCompactHeaderAndRendersRealProviderMetadata() {
        val provider = Provider(
            id = "rich",
            name = "A very helpful developmental resource with a long name",
            type = "Therapy clinic",
            phone = "3235551212",
            address = "123 Main Street",
            city = "Los Angeles",
            state = "CA",
            zipCode = "90001",
            therapyTypes = listOf(
                "ABA therapy",
                "Speech Therapy",
                "Occupational Therapy",
                "Physical Therapy"
            ),
            regionalCenter = "Eastern Los Angeles Regional Center",
            distance = 2.4
        )

        composeRule.setContent {
            KINDDTheme {
                ProviderListContent(
                    state = DiscoveryState(
                        providers = listOf(provider),
                        hasLoadedOnce = true
                    ),
                    providers = listOf(provider),
                    sort = ProviderListSort.NAME,
                    onSortChange = {},
                    actions = discoveryActions(FakeDiscoveryController(DiscoveryState())),
                    onProviderClick = {}
                )
            }
        }

        composeRule.onNodeWithTag("list_compact_header").assertIsDisplayed()
        composeRule.onNodeWithTag("list_solid_top_app_bar").assertDoesNotExist()
        composeRule.onNodeWithTag("list_title").assertTextContains("Resources")
        composeRule.onNodeWithTag("discovery_search_field").assertIsDisplayed()
        composeRule.onNodeWithTag("list_sort_button").assertIsDisplayed()
        composeRule.onNodeWithTag("list_filter_button").assertIsDisplayed()
        composeRule.onNodeWithTag("provider_type_rich", useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithText("Therapy clinic", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("provider_distance_rich", useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithText("2.4 mi", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("provider_address_rich_0", useUnmergedTree = true)
            .assertTextContains("123 Main Street")
        composeRule.onNodeWithTag("provider_address_rich_1", useUnmergedTree = true)
            .assertTextContains("Los Angeles, CA 90001")
        composeRule.onNodeWithTag("provider_therapy_rich_0", useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithText("ABA Therapy", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("provider_therapy_rich_1", useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithText("Speech Therapy", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("provider_therapy_rich_2", useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithText("Occupational Therapy", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("provider_therapy_rich_3", useUnmergedTree = true)
            .assertDoesNotExist()
        composeRule.onNodeWithTag("provider_therapy_overflow_rich", useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithText("+1 more", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("provider_phone_rich", useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithText("(323) 555-1212", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("provider_regional_center_rich", useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithText(
            "Eastern Los Angeles Regional Center",
            useUnmergedTree = true
        ).assertExists()
    }

    @Test
    fun providerList_hidesPhoneAndRegionalCenterWithoutRealValues() {
        val provider = Provider(
            id = "minimal",
            name = "Community resource",
            phone = "  ",
            regionalCenter = null,
            therapyTypes = listOf("Physical Therapy")
        )

        composeRule.setContent {
            KINDDTheme {
                ProviderListContent(
                    state = DiscoveryState(
                        providers = listOf(provider),
                        hasLoadedOnce = true
                    ),
                    providers = listOf(provider),
                    sort = ProviderListSort.DISTANCE,
                    onSortChange = {},
                    actions = discoveryActions(FakeDiscoveryController(DiscoveryState())),
                    onProviderClick = {}
                )
            }
        }

        composeRule.onNodeWithTag("provider_phone_minimal").assertDoesNotExist()
        composeRule.onNodeWithTag("provider_regional_center_minimal").assertDoesNotExist()
    }

    @Test
    fun providerList_querySortAndFilterActionsFireExactlyOnce() {
        var query by mutableStateOf("")
        var queryChanges = 0
        var sortChanges = 0
        val actions = discoveryActions(FakeDiscoveryController(DiscoveryState())).copy(
            onQueryChange = {
                query = it
                queryChanges += 1
            }
        )

        composeRule.setContent {
            KINDDTheme {
                ProviderListContent(
                    state = DiscoveryState(
                        criteria = DiscoveryCriteria(query = query),
                        hasLoadedOnce = true
                    ),
                    providers = emptyList(),
                    sort = ProviderListSort.NAME,
                    onSortChange = { sortChanges += 1 },
                    actions = actions,
                    onProviderClick = {}
                )
            }
        }

        composeRule.onNodeWithTag("discovery_search_field").performTextReplacement("speech")
        composeRule.onNodeWithTag("list_sort_button").performClick()
        composeRule.onNodeWithTag("list_filter_button").performClick()
        composeRule.onNodeWithTag("discovery_filter_title").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals("speech", query)
            assertEquals(1, queryChanges)
            assertEquals(1, sortChanges)
        }
    }

    @Test
    fun providerList_loadingErrorAndEmptyStatesRetainCompactDiscoveryChrome() {
        var state by mutableStateOf(DiscoveryState(isLoading = true))
        var refreshClicks = 0

        composeRule.setContent {
            KINDDTheme {
                ProviderListContent(
                    state = state,
                    providers = state.providers,
                    sort = ProviderListSort.NAME,
                    onSortChange = {},
                    actions = discoveryActions(FakeDiscoveryController(DiscoveryState())).copy(
                        onRetry = { refreshClicks += 1 }
                    ),
                    onProviderClick = {}
                )
            }
        }

        composeRule.onNodeWithTag("discovery_initial_loading").assertIsDisplayed()
        assertListChromeIsDisplayed()
        composeRule.runOnIdle { state = DiscoveryState(error = DiscoveryError.NETWORK) }
        composeRule.onNodeWithTag("discovery_initial_error").assertIsDisplayed()
        assertListChromeIsDisplayed()
        composeRule.runOnIdle { state = DiscoveryState(hasLoadedOnce = true) }
        composeRule.onNodeWithTag("discovery_empty").assertIsDisplayed()
        composeRule.onNodeWithTag("provider_list_empty_icon").assertIsDisplayed()
        composeRule.onNodeWithText("No Resources Found").assertIsDisplayed()
        composeRule.onNodeWithText(
            "Try adjusting your search filters or expanding your search radius."
        ).assertIsDisplayed()
        val refreshNode = composeRule.onNodeWithTag("provider_list_empty_refresh")
            .assertIsDisplayed()
            .assertHasClickAction()
        val minimumPixels = 48f * composeRule.density.density
        assertTrue(
            "Refresh target is shorter than 48dp",
            refreshNode.fetchSemanticsNode().boundsInRoot.height >= minimumPixels
        )
        refreshNode.performClick()
        composeRule.runOnIdle { assertEquals(1, refreshClicks) }
        assertListChromeIsDisplayed()
    }

    @Test
    fun providerCard_phoneActionIsIndependentAndFiresExactlyOnce() {
        var cardClicks = 0
        var phoneClicks = 0
        val provider = Provider(
            id = "phone",
            name = "Community resource",
            phone = "3235551212"
        )

        composeRule.setContent {
            KINDDTheme {
                com.chla.kindd.ui.providers.ProviderCard(
                    provider = provider,
                    onClick = { cardClicks += 1 },
                    onPhoneClick = { phoneClicks += 1 }
                )
            }
        }

        composeRule.onNodeWithTag("provider_phone_phone", useUnmergedTree = true)
            .assertHasClickAction()
            .performClick()
        composeRule.runOnIdle {
            assertEquals(0, cardClicks)
            assertEquals(1, phoneClicks)
        }
    }

    @Test
    fun providerList_lastCardScrollsFullyIntoViewAboveItsClearanceItem() {
        val providers = (1..18).map { index ->
            Provider(
                id = "provider-$index",
                name = "Provider $index",
                therapyTypes = listOf("ABA therapy")
            )
        }

        composeRule.setContent {
            KINDDTheme {
                ProviderListContent(
                    state = DiscoveryState(providers = providers, hasLoadedOnce = true),
                    providers = providers,
                    sort = ProviderListSort.NAME,
                    onSortChange = {},
                    actions = discoveryActions(FakeDiscoveryController(DiscoveryState())),
                    onProviderClick = {}
                )
            }
        }

        composeRule.onNodeWithTag("provider_list")
            .performScrollToNode(hasTestTag("provider_provider-18"))
        composeRule.onNodeWithTag("provider_provider-18").assertIsDisplayed()
        composeRule.onNodeWithTag("provider_list")
            .performScrollToNode(hasTestTag("provider_list_bottom_clearance"))
        composeRule.onNodeWithTag("provider_list_bottom_clearance").assertIsDisplayed()
    }

    private fun assertListChromeIsDisplayed() {
        composeRule.onNodeWithTag("list_compact_header").assertIsDisplayed()
        composeRule.onNodeWithTag("discovery_search_field").assertIsDisplayed()
        composeRule.onNodeWithText("Search resources, services, or ZIP code")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Sort resources").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Filters").assertIsDisplayed()
    }

    private fun assertImmersiveMapChrome() {
        composeRule.onNodeWithTag("fake_map_surface").assertIsDisplayed()
        composeRule.onNodeWithTag("map_search_chrome").assertIsDisplayed()
        composeRule.onNodeWithTag("discovery_search_field").assertIsDisplayed()
        composeRule.onNodeWithText("Search resources, services, or ZIP code")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("map_top_filter").assertIsDisplayed()
        composeRule.onNodeWithTag("map_control_rail").assertIsDisplayed()
        composeRule.onNodeWithTag("map_title").assertDoesNotExist()
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

    private class DiscoveryDestinations(
        private val controller: FakeDiscoveryController
    ) : MainDestinationContent {
        var lastMarkerIds: List<String> = emptyList()
        var lastClickedProviderId: String? = null

        @Composable
        override fun home(profile: UserProfile, actions: MainNavActions) {
            Text("home", Modifier.testTag("parity_home"))
        }

        @Composable
        override fun map(actions: MainNavActions) {
            val state by controller.state.collectAsState()
            MapContent(
                state = state,
                locationState = MapLocationState(),
                actions = discoveryActions(controller),
                onUseMyLocation = {},
                onProviderClick = { lastClickedProviderId = it },
                onNavigateToList = actions.navigateToList,
                markerContent = { markers, onProviderClick ->
                    lastMarkerIds = markers.map(MapMarkerModel::providerId)
                    Column {
                        markers.forEach { marker ->
                            Text(
                                text = marker.providerId,
                                modifier = Modifier
                                    .testTag("map_marker_${marker.providerId}")
                                    .clickable { onProviderClick(marker.providerId) }
                            )
                        }
                    }
                }
            )
        }

        @Composable
        override fun list(actions: MainNavActions) {
            val state by controller.state.collectAsState()
            ProviderListContent(
                state = state,
                providers = state.providers,
                sort = ProviderListSort.NAME,
                onSortChange = {},
                actions = discoveryActions(controller),
                onProviderClick = { lastClickedProviderId = it }
            )
        }

        @Composable
        override fun chat(prompt: ChatLaunchPrompt?, actions: MainNavActions) = Unit

        @Composable
        override fun settings(actions: MainNavActions) = Unit

        @Composable
        override fun more(actions: MainNavActions) = Unit

        @Composable
        override fun providerDetail(providerId: String, actions: MainNavActions) = Unit

        @Composable
        override fun regions(actions: MainNavActions) = Unit

        @Composable
        override fun faq(actions: MainNavActions) = Unit

        @Composable
        override fun about(actions: MainNavActions) = Unit

        @Composable
        override fun editProfile(profile: UserProfile, actions: MainNavActions) = Unit
    }

    private class FakeDiscoveryController(
        initialState: DiscoveryState
    ) : DiscoveryController {
        private val mutableState = MutableStateFlow(initialState)
        override val state: StateFlow<DiscoveryState> = mutableState

        override fun ensureLoaded() = Unit
        override fun setQuery(query: String) {
            mutableState.value = mutableState.value.copy(
                criteria = mutableState.value.criteria.copy(query = query)
            )
        }
        override fun applyFilters(
            therapyTypes: Set<TherapyType>,
            ageGroup: AgeGroup?,
            diagnosis: String?,
            insurance: String?,
            radiusMiles: Int
        ) {
            mutableState.value = mutableState.value.copy(
                criteria = mutableState.value.criteria.copy(
                    therapyTypes = therapyTypes,
                    ageGroup = ageGroup,
                    diagnosis = diagnosis,
                    insurance = insurance,
                    radiusMiles = radiusMiles
                )
            )
        }
        override fun setSingleTherapyAndRefresh(therapyType: TherapyType) = Unit
        override fun useDeviceLocation(latitude: Double, longitude: Double) = Unit
        override fun useLosAngelesCatalog() = Unit
        override fun refresh() = Unit
        override fun retry() = Unit
        override fun clearAllFilters() = Unit
    }

    private companion object {
        fun provider(
            id: String,
            latitude: Double?,
            longitude: Double?,
            therapyTypes: List<String>? = null
        ) = Provider(
            id = id,
            name = id,
            latitude = latitude,
            longitude = longitude,
            therapyTypes = therapyTypes
        )

        fun completeProfile() = UserProfile(
            onboardingCompleted = true,
            audienceType = AudienceType.FAMILY,
            zipCode = "90001",
            journeyStage = JourneyStage.EXPLORING
        )

        fun discoveryActions(controller: DiscoveryController) = DiscoveryUiActions(
            onQueryChange = controller::setQuery,
            onApplyFilters = { selection ->
                controller.applyFilters(
                    therapyTypes = selection.therapyTypes,
                    ageGroup = selection.ageGroup,
                    diagnosis = selection.diagnosis,
                    insurance = selection.insurance,
                    radiusMiles = selection.radiusMiles
                )
            },
            onRemoveTherapy = {},
            onRemoveAge = {},
            onRemoveDiagnosis = {},
            onRemoveInsurance = {},
            onRemoveRadius = {},
            onClearAll = controller::clearAllFilters,
            onRetry = controller::retry,
            onRefresh = controller::refresh
        )
    }
}
