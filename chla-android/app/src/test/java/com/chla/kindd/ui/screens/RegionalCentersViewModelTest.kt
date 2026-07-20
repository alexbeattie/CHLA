package com.chla.kindd.ui.screens

import com.chla.kindd.data.models.RegionalCenter
import com.chla.kindd.data.profile.AgeGroup
import com.chla.kindd.data.profile.AudienceType
import com.chla.kindd.data.profile.JourneyStage
import com.chla.kindd.data.profile.RegionalCenterIdentity
import com.chla.kindd.data.profile.UserProfile
import com.chla.kindd.data.profile.UserProfileRepository
import com.chla.kindd.data.source.LookupFailure
import com.chla.kindd.data.source.RegionalCenterDataSource
import com.chla.kindd.data.source.RegionalCenterLookup
import com.chla.kindd.testing.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegionalCentersViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun savedProfilePrefillsZip_andSevenCenterCatalogStillLoads() =
        runTest(mainDispatcherRule.testDispatcher) {
            val centers = (1..7).map { center(it, "Center $it") }
            val viewModel = RegionalCentersViewModel(
                RecordingProfileRepository(profile()),
                FakeCenterSource(centers = Result.success(centers))
            )
            runCurrent()

            assertEquals("90001", viewModel.uiState.value.zipDraft)
            assertEquals(centers, viewModel.uiState.value.centers)
        }

    @Test
    fun zipDraftKeepsFiveAsciiDigits_andSubmitUsesTypedLookup() =
        runTest(mainDispatcherRule.testDispatcher) {
            val source = FakeCenterSource()
            val viewModel = RegionalCentersViewModel(RecordingProfileRepository(profile()), source)
            runCurrent()

            viewModel.onZipChanged("a1٢2-34567")
            assertEquals("12345", viewModel.uiState.value.zipDraft)
            viewModel.submitZip()
            runCurrent()

            assertEquals(listOf("12345"), source.lookups)
        }

    @Test
    fun matchedLookupReplacesOnlyZipAndCenter_preservingFullProfile() =
        runTest(mainDispatcherRule.testDispatcher) {
            val original = profile()
            val matched = center(9, "Westside Regional Center")
            val repository = RecordingProfileRepository(original)
            val viewModel = RegionalCentersViewModel(
                repository,
                FakeCenterSource(lookup = RegionalCenterLookup.Matched(matched))
            )
            runCurrent()
            viewModel.onZipChanged("90210")

            viewModel.submitZip()
            runCurrent()

            assertEquals(
                original.copy(zipCode = "90210", regionalCenter = RegionalCenterIdentity.from(matched)),
                repository.replacements.single()
            )
            assertEquals(RegionalCentersLookupState.MATCHED, viewModel.uiState.value.lookupState)
        }

    @Test
    fun unmatchedAndUnavailableChangeOnlySanitizedScreenState() =
        runTest(mainDispatcherRule.testDispatcher) {
            listOf(
                RegionalCenterLookup.Unmatched to RegionalCentersLookupState.UNMATCHED,
                RegionalCenterLookup.Unavailable(LookupFailure.SERVER) to RegionalCentersLookupState.UNAVAILABLE
            ).forEach { (lookup, expected) ->
                val original = profile()
                val repository = RecordingProfileRepository(original)
                val viewModel = RegionalCentersViewModel(repository, FakeCenterSource(lookup = lookup))
                runCurrent()
                viewModel.onZipChanged("90210")
                viewModel.submitZip()
                runCurrent()

                assertEquals(original, repository.current)
                assertTrue(repository.replacements.isEmpty())
                assertEquals(expected, viewModel.uiState.value.lookupState)
                assertTrue(viewModel.uiState.value.message.toString().contains("90210").not())
            }
        }

    private class RecordingProfileRepository(initial: UserProfile) : UserProfileRepository {
        private val profiles = MutableStateFlow(initial)
        val replacements = mutableListOf<UserProfile>()
        val current get() = profiles.value
        override val profile: Flow<UserProfile> = profiles
        override suspend fun replaceProfile(profile: UserProfile) {
            replacements += profile
            profiles.value = profile
        }
        override suspend fun clearProfile() { profiles.value = UserProfile() }
    }

    private class FakeCenterSource(
        private val centers: Result<List<RegionalCenter>> = Result.success(emptyList()),
        private val lookup: RegionalCenterLookup = RegionalCenterLookup.Unmatched
    ) : RegionalCenterDataSource {
        val lookups = mutableListOf<String>()
        override suspend fun getRegionalCenters() = centers
        override suspend fun getRegionalCentersNearby(latitude: Double, longitude: Double) = centers
        override suspend fun lookupRegionalCenter(zipCode: String): RegionalCenterLookup {
            lookups += zipCode
            return lookup
        }
    }

    private fun profile() = UserProfile(
        onboardingCompleted = true,
        audienceType = AudienceType.CLINICIAN,
        zipCode = "90001",
        regionalCenter = RegionalCenterIdentity(1, "Old center", "OLD"),
        journeyStage = JourneyStage.RECEIVING_SERVICES,
        ageGroup = AgeGroup.ADULT
    )

    private fun center(id: Int, name: String) = RegionalCenter(
        id = id,
        name = name,
        countyServed = "Los Angeles"
    )
}
