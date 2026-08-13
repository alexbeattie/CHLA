package com.navigator.kindd.ui.discovery

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import com.navigator.kindd.data.discovery.DiscoveryCatalog
import com.navigator.kindd.data.discovery.DiscoveryCriteria
import com.navigator.kindd.data.discovery.DiscoveryError
import com.navigator.kindd.data.discovery.DiscoveryOrigin
import com.navigator.kindd.data.discovery.DiscoveryState
import com.navigator.kindd.data.discovery.TherapyType
import com.navigator.kindd.data.models.Provider
import com.navigator.kindd.data.profile.AgeGroup
import com.navigator.kindd.ui.theme.KINDDTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DiscoveryControlsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun searchField_bindsQueryAndExposesClearAndFilterActions() {
        var query by mutableStateOf("speech")
        var filterClicks = 0
        composeRule.setContent {
            KINDDTheme {
                DiscoverySearchField(
                    query = query,
                    onQueryChange = { query = it },
                    onFilterClick = { filterClicks += 1 }
                )
            }
        }

        composeRule.onNodeWithTag("discovery_search_field")
            .assertTextContains("speech")
            .performTextReplacement("occupational")
        composeRule.runOnIdle { assertEquals("occupational", query) }

        composeRule.onNodeWithTag("discovery_filter_button").performClick()
        composeRule.runOnIdle { assertEquals(1, filterClicks) }
        composeRule.onNodeWithTag("discovery_clear_query").performClick()
        composeRule.runOnIdle { assertEquals("", query) }
    }

    @Test
    fun filterSheet_keepsDraftUntilApply_andDismissDoesNotApply() {
        var applied: DiscoveryFilterSelection? = null
        var dismissed = false
        composeRule.setContent {
            KINDDTheme {
                DiscoveryFilterSheet(
                    criteria = DiscoveryCriteria(
                        therapyTypes = setOf(TherapyType.ABA),
                        origin = DiscoveryOrigin.DeviceLocation(34.0, -118.0)
                    ),
                    onDismissRequest = { dismissed = true },
                    onApply = { applied = it }
                )
            }
        }

        composeRule.onNodeWithText("Speech Therapy").performClick()
        composeRule.onNodeWithTag("discovery_filter_dismiss").performClick()

        composeRule.runOnIdle {
            assertTrue(dismissed)
            assertNull(applied)
        }
    }

    @Test
    fun filterSheet_showsCanonicalCatalogs_andAppliesApiValuesOnly() {
        var applied: DiscoveryFilterSelection? = null
        composeRule.setContent {
            KINDDTheme {
                DiscoveryFilterSheet(
                    criteria = DiscoveryCriteria(
                        origin = DiscoveryOrigin.DeviceLocation(34.0, -118.0)
                    ),
                    onDismissRequest = {},
                    onApply = { applied = it }
                )
            }
        }

        listOf(5, 10, 15, 25, 50).forEach { radius ->
            composeRule.onNodeWithText("$radius miles").assertExists()
        }
        composeRule.onNodeWithText("Any Age").assertExists()
        composeRule.onNodeWithText("Any Diagnosis").assertExists()
        composeRule.onNodeWithText("Any Insurance").assertExists()
        listOf(
            "0-5 years (Early Intervention)",
            "6-12 years (School Age)",
            "13-18 years (Adolescent)",
            "19+ years (Adult)",
            "All Ages"
        ).forEach { composeRule.onNodeWithText(it).assertExists() }
        DiscoveryCatalog.diagnoses.forEach { composeRule.onNodeWithText(it).assertExists() }
        DiscoveryCatalog.insurances.forEach { composeRule.onNodeWithText(it).assertExists() }
        listOf(
            "ABA Therapy",
            "Speech Therapy",
            "Occupational Therapy",
            "Physical Therapy",
            "Feeding Therapy",
            "Parent-Child Interaction and Parent Training"
        ).forEach { composeRule.onNodeWithText(it).assertExists() }

        composeRule.onNodeWithText("Speech Therapy").performScrollTo().performClick()
        composeRule.onNodeWithText("19+ years (Adult)").performScrollTo().performClick()
        composeRule.onNodeWithText("Other").performScrollTo().performClick()
        composeRule.onNodeWithText("L.A. Care").performScrollTo().performClick()
        composeRule.onNodeWithText("25 miles").performScrollTo().performClick()
        composeRule.onNodeWithTag("discovery_filter_apply").performScrollTo().performClick()

        composeRule.runOnIdle {
            assertEquals(setOf(TherapyType.SPEECH), applied?.therapyTypes)
            assertEquals(AgeGroup.ADULT, applied?.ageGroup)
            assertEquals("Other", applied?.diagnosis)
            assertEquals("L.A. Care", applied?.insurance)
            assertEquals(25, applied?.radiusMiles)
        }
    }

    @Test
    fun filterSheet_hidesRadiusWhenOriginDoesNotUseIt() {
        composeRule.setContent {
            KINDDTheme {
                DiscoveryFilterSheet(
                    criteria = DiscoveryCriteria(
                        radiusMiles = 50,
                        origin = DiscoveryOrigin.ProfileZip("90001")
                    ),
                    onDismissRequest = {},
                    onApply = {}
                )
            }
        }

        composeRule.onNodeWithText("50 miles").assertDoesNotExist()
    }

    @Test
    fun filterSheet_resetClearsDraftAndRestoresDefaultRadiusBeforeApply() {
        var applied: DiscoveryFilterSelection? = null
        composeRule.setContent {
            KINDDTheme {
                DiscoveryFilterSheet(
                    criteria = DiscoveryCriteria(
                        therapyTypes = setOf(TherapyType.ABA),
                        ageGroup = AgeGroup.ADULT,
                        diagnosis = "Other",
                        insurance = "L.A. Care",
                        radiusMiles = 50,
                        origin = DiscoveryOrigin.DeviceLocation(34.0, -118.0)
                    ),
                    onDismissRequest = {},
                    onApply = { applied = it }
                )
            }
        }

        composeRule.onNodeWithTag("discovery_filter_reset").performScrollTo().performClick()
        composeRule.onNodeWithTag("discovery_filter_apply").performScrollTo().performClick()

        composeRule.runOnIdle {
            assertEquals(emptySet<TherapyType>(), applied?.therapyTypes)
            assertNull(applied?.ageGroup)
            assertNull(applied?.diagnosis)
            assertNull(applied?.insurance)
            assertEquals(15, applied?.radiusMiles)
        }
    }

    @Test
    fun activeChips_areRemovable_hideIneffectiveRadius_andClearOnce() {
        var removedAge = false
        var removedRadius = false
        var clearCount = 0
        var criteria by mutableStateOf(
            DiscoveryCriteria(
                therapyTypes = setOf(TherapyType.ABA),
                ageGroup = AgeGroup.SCHOOL_AGE,
                diagnosis = "Autism Spectrum Disorder",
                insurance = "Medi-Cal",
                radiusMiles = 25,
                origin = DiscoveryOrigin.ProfileZip("90001")
            )
        )
        composeRule.setContent {
            KINDDTheme {
                ActiveFilterChips(
                    criteria = criteria,
                    onRemoveTherapy = {},
                    onRemoveAge = { removedAge = true },
                    onRemoveDiagnosis = {},
                    onRemoveInsurance = {},
                    onRemoveRadius = { removedRadius = true },
                    onClearAll = { clearCount += 1 },
                    modifier = Modifier.testTag("active_filter_chips")
                )
            }
        }

        composeRule.onNodeWithTag("filter_chip_therapy_ABA").assertExists()
        composeRule.onNodeWithText("ABA Therapy").assertIsDisplayed()
        composeRule.onNodeWithText("Remove ABA Therapy").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Remove ABA Therapy").assertIsDisplayed()
        composeRule.onNodeWithTag("filter_chip_age").performClick()
        composeRule.onNodeWithTag("filter_chip_radius").assertDoesNotExist()
        composeRule.onNodeWithTag("active_filter_chips")
            .performScrollToNode(hasTestTag("discovery_clear_all"))
        composeRule.onNodeWithTag("discovery_clear_all").performClick()
        composeRule.runOnIdle {
            assertTrue(removedAge)
            assertFalse(removedRadius)
            assertEquals(1, clearCount)
            criteria = criteria.copy(
                origin = DiscoveryOrigin.DeviceLocation(34.0, -118.0)
            )
        }
        composeRule.onNodeWithTag("active_filter_chips")
            .performScrollToNode(hasTestTag("filter_chip_radius"))
        composeRule.onNodeWithTag("filter_chip_radius").performClick()
        composeRule.runOnIdle { assertTrue(removedRadius) }
    }

    @Test
    fun stateContent_centralizesEmptyInitialErrorRetryAndRefreshPreservation() {
        var state by mutableStateOf(
            DiscoveryState(
                providers = emptyList(),
                hasLoadedOnce = true
            )
        )
        var retryCount = 0
        composeRule.setContent {
            KINDDTheme {
                DiscoveryStateContent(
                    state = state,
                    onRetry = { retryCount += 1 }
                ) { providers ->
                    providers.forEach { Text(it.id) }
                }
            }
        }

        composeRule.onNodeWithTag("discovery_empty").assertIsDisplayed()
        composeRule.runOnIdle {
            state = DiscoveryState(error = DiscoveryError.NETWORK)
        }
        composeRule.onNodeWithTag("discovery_initial_error").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()
        composeRule.runOnIdle {
            assertEquals(1, retryCount)
            state = DiscoveryState(
                providers = listOf(Provider(id = "kept", name = "Kept")),
                isLoading = true,
                hasLoadedOnce = true
            )
        }
        composeRule.onNodeWithText("kept").assertExists()
        composeRule.onNodeWithTag("discovery_refresh_progress").assertExists()
        composeRule.onNodeWithTag("discovery_result_count")
            .assertTextContains("1", substring = true)
        composeRule.runOnIdle {
            state = state.copy(isLoading = false, error = DiscoveryError.SERVER)
        }
        composeRule.onNodeWithText("kept").assertExists()
        composeRule.onNodeWithTag("discovery_error_banner").assertExists()
        composeRule.onNodeWithText("Retry").performClick()
        composeRule.runOnIdle { assertEquals(2, retryCount) }
    }

    @Test
    fun stateContent_initialLoadingUsesFullLoadingState() {
        composeRule.setContent {
            KINDDTheme {
                DiscoveryStateContent(
                    state = DiscoveryState(isLoading = true),
                    onRetry = {}
                ) {}
            }
        }

        composeRule.onNodeWithTag("discovery_initial_loading").assertIsDisplayed()
        composeRule.onNodeWithTag("discovery_empty").assertDoesNotExist()
    }

    @Test
    fun stateContent_loadedEmptyFailureShowsRecoverableErrorInsteadOfNoResults() {
        var retryCount = 0
        composeRule.setContent {
            KINDDTheme {
                DiscoveryStateContent(
                    state = DiscoveryState(
                        providers = emptyList(),
                        error = DiscoveryError.NETWORK,
                        hasLoadedOnce = true
                    ),
                    onRetry = { retryCount += 1 }
                ) {}
            }
        }

        composeRule.onNodeWithTag("discovery_initial_error").assertIsDisplayed()
        composeRule.onNodeWithTag("discovery_empty").assertDoesNotExist()
        composeRule.onNodeWithText("Retry").performClick()
        composeRule.runOnIdle { assertEquals(1, retryCount) }
    }
}
