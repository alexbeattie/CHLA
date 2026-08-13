package com.navigator.kindd.ui.regions

import androidx.activity.ComponentActivity
import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.navigator.kindd.data.models.RegionalCenter
import com.navigator.kindd.data.profile.UserProfile
import com.navigator.kindd.data.profile.UserProfileRepository
import com.navigator.kindd.data.servicearea.ServiceAreaCoordinate
import com.navigator.kindd.data.servicearea.ServiceAreaFeature
import com.navigator.kindd.data.source.RegionalCenterDataSource
import com.navigator.kindd.data.source.RegionalCenterLookup
import com.navigator.kindd.ui.screens.RegionalCentersViewModel
import com.navigator.kindd.ui.screens.RegionalCentersScreen
import com.navigator.kindd.ui.theme.KINDDTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
import java.util.Locale

class RegionalCentersContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun regions_defaultsToMapWithToggleAndAllBundledBoundaries() {
        compose()

        composeRule.onNodeWithTag("regions_mode_map").assertIsSelected()
        composeRule.onNodeWithTag("regions_mode_toggle").assertExists()
        composeRule.onNodeWithTag("regions_map").assertExists()
        composeRule.onNodeWithTag("regional_center_map_surface").assertExists()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("regions_boundaries_7")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun listModeKeepsEveryApiCenterAvailable() {
        compose()

        composeRule.onNodeWithTag("regions_mode_list").performClick().assertIsSelected()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(centers().first().name)
                .fetchSemanticsNodes().isNotEmpty()
        }
        centers().forEach { center ->
            composeRule.onNodeWithText(center.name).assertExists()
        }
    }

    @Test
    fun zipResultUsesMatchedApiCenter() {
        compose(lookup = RegionalCenterLookup.Matched(centers().first()))

        composeRule.onNodeWithTag("regions_zip_input").performTextReplacement("90001")
        composeRule.onNodeWithTag("regions_zip_submit").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("regions_zip_result")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(centers().first().name).assertExists()
    }

    @Test
    fun selectedCenterSheetShowsOnlyApiDetailFields() {
        compose()

        composeRule.onNodeWithTag("regions_mode_list").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("regions_center_WRC")
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("regions_center_WRC").performClick()

        composeRule.onNodeWithTag("regions_center_detail").assertExists()
        composeRule.onAllNodesWithText("Westside Regional Center").assertCountEquals(2)
        composeRule.onNodeWithText("5901 Green Valley Circle, Culver City, CA 90230").assertExists()
        composeRule.onNodeWithText("(310) 258-4000").assertExists()
        composeRule.onNodeWithText("westside.org").assertExists()
        composeRule.onNode(
            hasText("Los Angeles County") and
                hasAnyAncestor(hasTestTag("regions_center_detail"))
        ).assertExists()
    }

    @Test
    fun spanishLargeText_listControlsStayInNormalFlowAboveFirstCard() {
        composeContent(fontScale = 1.3f, locale = Locale.forLanguageTag("es"))

        composeRule.onNodeWithTag("regions_mode_list").performClick()
        val controls = composeRule.onNodeWithTag("regions_controls")
            .assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        val firstCard = composeRule.onNodeWithTag("regions_center_WRC")
            .assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot

        assertTrue("First center card is covered by lookup controls", firstCard.top >= controls.bottom)
    }

    @Test
    fun largeText_detailSheetKeepsPhoneWebsiteAndCloseReachable() {
        composeContent(fontScale = 1.5f, locale = Locale.US)

        composeRule.onNodeWithTag("regions_mode_list").performClick()
        composeRule.onNodeWithTag("regions_center_WRC").performClick()

        composeRule.onNodeWithTag("regions_detail_phone").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("regions_detail_website").performScrollTo().assertIsDisplayed()
        val closeBounds = composeRule.onNodeWithTag("regions_detail_close")
            .performScrollTo()
            .assertIsDisplayed()
            .fetchSemanticsNode().boundsInRoot
        val minimumPixels = 48f * composeRule.density.density
        assertTrue("Close action is shorter than 48dp", closeBounds.height >= minimumPixels)
    }

    @Test
    fun loadingServiceAreasShowsFallbackWithoutCreatingGoogleMap() {
        composeContent(serviceAreaState = RegionalCenterServiceAreaState.Loading)
        composeRule.onNodeWithTag("regional_center_service_areas_loading").assertIsDisplayed()
        composeRule.onNodeWithTag("regional_center_map_surface").assertDoesNotExist()
    }

    @Test
    fun errorServiceAreasShowsFallbackWithoutCreatingGoogleMap() {
        composeContent(serviceAreaState = RegionalCenterServiceAreaState.Error)
        composeRule.onNodeWithTag("regional_center_service_areas_error").assertIsDisplayed()
        composeRule.onNodeWithTag("regional_center_map_surface").assertDoesNotExist()
    }

    @Test
    fun mapAreaClickOpensMatchingApiCenterDetail() {
        composeContent(
            mapContent = { _, onAreaClick ->
                Button(
                    onClick = { onAreaClick("WRC") },
                    modifier = Modifier.testTag("fake_region_area")
                ) { Text("WRC area") }
            }
        )

        composeRule.onNodeWithTag("fake_region_area").performClick()
        composeRule.onNodeWithTag("regions_center_detail").assertIsDisplayed()
        composeRule.onNodeWithText("Westside Regional Center").assertExists()
    }

    private fun compose(
        lookup: RegionalCenterLookup = RegionalCenterLookup.Unmatched
    ) {
        val viewModel = RegionalCentersViewModel(
            profileRepository = FakeProfileRepository(),
            regionalCenterDataSource = FakeRegionalCenterDataSource(centers(), lookup)
        )
        composeRule.setContent {
            KINDDTheme {
                RegionalCentersScreen(
                    onBack = {},
                    viewModel = viewModel,
                    serviceAreasOverride = serviceAreas(),
                    mapContent = { _, _ -> Box(Modifier.fillMaxSize()) }
                )
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("regions_mode_toggle")
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun composeContent(
        serviceAreaState: RegionalCenterServiceAreaState =
            RegionalCenterServiceAreaState.Success(serviceAreas()),
        fontScale: Float = 1f,
        locale: Locale = Locale.US,
        mapContent: @androidx.compose.runtime.Composable (
            com.navigator.kindd.ui.map.RegionalCenterMapRenderModel,
            (String) -> Unit
        ) -> Unit = { _, _ -> Box(Modifier.fillMaxSize()) }
    ) {
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(baseContext.resources.configuration).apply {
            setLocale(locale)
            screenWidthDp = 320
            this.fontScale = fontScale
        }
        val localizedContext = baseContext.createConfigurationContext(configuration)
        composeRule.setContent {
            LocalizedSurface(localizedContext, configuration, fontScale) {
                RegionalCentersContent(
                    uiState = com.navigator.kindd.ui.screens.RegionalCentersUiState(centers = centers()),
                    serviceAreaState = serviceAreaState,
                    onBack = {},
                    onZipChanged = {},
                    onSubmitZip = {},
                    mapContent = mapContent
                )
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun LocalizedSurface(
        context: Context,
        configuration: Configuration,
        fontScale: Float,
        content: @androidx.compose.runtime.Composable () -> Unit
    ) {
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalContext provides context,
            LocalConfiguration provides configuration,
            LocalDensity provides Density(density.density, fontScale)
        ) {
            KINDDTheme {
                Box(Modifier.width(320.dp).height(720.dp)) { content() }
            }
        }
    }

    private fun serviceAreas(): List<ServiceAreaFeature> = listOf(
        serviceArea(1, "Westside Regional Center", "WRC"),
        serviceArea(2, "Harbor Regional Center", "HRC"),
        serviceArea(3, "South Central Los Angeles Regional Center", "SCLARC"),
        serviceArea(4, "Eastern Los Angeles Regional Center", "ELARC"),
        serviceArea(5, "North Los Angeles County Regional Center", "NLACRC"),
        serviceArea(6, "Frank D. Lanterman Regional Center", "FDLRC"),
        serviceArea(7, "San Gabriel/Pomona Regional Center", "SG/PRC")
    )

    private fun serviceArea(id: Int, name: String, acronym: String) = ServiceAreaFeature(
        id = id,
        name = name,
        acronym = acronym,
        description = "",
        polygons = listOf(
            listOf(
                ServiceAreaCoordinate(34.0, -118.4),
                ServiceAreaCoordinate(34.1, -118.3),
                ServiceAreaCoordinate(33.9, -118.2)
            )
        )
    )

    private fun centers(): List<RegionalCenter> = listOf(
        center(1, "Westside Regional Center", "5901 Green Valley Circle", "Culver City", "90230", "3102584000", "westside.org"),
        center(2, "Harbor Regional Center"),
        center(3, "South Central Los Angeles Regional Center"),
        center(4, "Eastern Los Angeles Regional Center"),
        center(5, "North Los Angeles County Regional Center"),
        center(6, "Frank D. Lanterman Regional Center"),
        center(7, "San Gabriel/Pomona Regional Center")
    )

    private fun center(
        id: Int,
        name: String,
        address: String? = null,
        city: String? = null,
        zipCode: String? = null,
        telephone: String? = null,
        website: String? = null
    ) = RegionalCenter(
        id = id,
        name = name,
        address = address,
        city = city,
        state = "CA",
        zipCode = zipCode,
        telephone = telephone,
        website = website,
        countyServed = "Los Angeles County"
    )

    private class FakeProfileRepository : UserProfileRepository {
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
    }

    private class FakeRegionalCenterDataSource(
        private val centers: List<RegionalCenter>,
        private val lookup: RegionalCenterLookup
    ) : RegionalCenterDataSource {
        override suspend fun getRegionalCenters() = Result.success(centers)

        override suspend fun getRegionalCentersNearby(latitude: Double, longitude: Double) =
            Result.success(centers)

        override suspend fun lookupRegionalCenter(zipCode: String) = lookup
    }
}
