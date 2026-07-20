package com.chla.kindd.ui.discovery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import com.chla.kindd.data.discovery.DiscoveryController
import com.chla.kindd.data.discovery.DiscoveryCriteria
import com.chla.kindd.data.discovery.DiscoveryOrigin
import com.chla.kindd.data.discovery.DiscoveryState
import com.chla.kindd.data.discovery.TherapyType
import com.chla.kindd.data.models.Provider
import com.chla.kindd.data.profile.AgeGroup
import com.chla.kindd.data.profile.AudienceType
import com.chla.kindd.data.profile.JourneyStage
import com.chla.kindd.data.profile.UserProfile
import com.chla.kindd.ui.chat.ChatLaunchPrompt
import com.chla.kindd.ui.navigation.KINDDMainNavHost
import com.chla.kindd.ui.navigation.MainDestinationContent
import com.chla.kindd.ui.navigation.MainNavActions
import com.chla.kindd.ui.navigation.Screen
import com.chla.kindd.ui.screens.MapContent
import com.chla.kindd.ui.screens.MapLocationState
import com.chla.kindd.ui.screens.MapMarkerModel
import com.chla.kindd.ui.screens.ProviderListContent
import com.chla.kindd.ui.screens.ProviderListSort
import com.chla.kindd.ui.theme.KINDDTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
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

        composeRule.onNodeWithTag("bottom_nav_list").performClick()
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
            onRetry = controller::retry
        )
    }
}
