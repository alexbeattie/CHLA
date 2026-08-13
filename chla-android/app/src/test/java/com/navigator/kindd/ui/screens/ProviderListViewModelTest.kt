package com.navigator.kindd.ui.screens

import com.navigator.kindd.data.discovery.DiscoveryCriteria
import com.navigator.kindd.data.discovery.DiscoveryOrigin
import com.navigator.kindd.data.discovery.DiscoveryState
import com.navigator.kindd.data.discovery.TherapyType
import com.navigator.kindd.data.models.Provider
import com.navigator.kindd.data.profile.AgeGroup
import org.junit.Assert.assertEquals
import org.junit.Test

class ProviderListViewModelTest {

    @Test
    fun `sorting is presentation only and never mutates shared provider identity`() {
        val sharedProviders = listOf(
            provider("z", name = "Zulu", distance = null),
            provider("a", name = "Alpha", distance = 9.0),
            provider("b", name = "Bravo", distance = 2.0)
        )
        val controller = FakeDiscoveryController(
            DiscoveryState(providers = sharedProviders, hasLoadedOnce = true)
        )
        val viewModel = ProviderListViewModel(controller)

        viewModel.setSort(ProviderListSort.NAME)
        assertEquals(listOf("a", "b", "z"), viewModel.providers.map(Provider::id))
        assertEquals(listOf("z", "a", "b"), controller.state.value.providers.map(Provider::id))

        viewModel.setSort(ProviderListSort.DISTANCE)
        assertEquals(listOf("b", "a", "z"), viewModel.providers.map(Provider::id))
        assertEquals(listOf("z", "a", "b"), controller.state.value.providers.map(Provider::id))
    }

    @Test
    fun `filter apply removal clear and retry delegate to one controller`() {
        val controller = FakeDiscoveryController(
            DiscoveryState(
                criteria = DiscoveryCriteria(
                    therapyTypes = setOf(TherapyType.ABA, TherapyType.SPEECH),
                    ageGroup = AgeGroup.SCHOOL_AGE,
                    diagnosis = "Autism Spectrum Disorder",
                    insurance = "Medi-Cal",
                    radiusMiles = 25,
                    origin = DiscoveryOrigin.DeviceLocation(34.0, -118.0)
                )
            )
        )
        val viewModel = ProviderListViewModel(controller)

        viewModel.applyFilters(
            therapyTypes = setOf(TherapyType.PHYSICAL),
            ageGroup = AgeGroup.ADULT,
            diagnosis = "Other",
            insurance = "L.A. Care",
            radiusMiles = 50
        )
        viewModel.removeTherapy(TherapyType.PHYSICAL)
        viewModel.removeAge()
        viewModel.removeDiagnosis()
        viewModel.removeInsurance()
        viewModel.removeRadius()
        viewModel.clearAllFilters()
        viewModel.retry()

        val filters = controller.calls.filterIsInstance<PresenterCall.ApplyFilters>()
        assertEquals(5, filters.size)
        assertEquals(emptySet<TherapyType>(), filters[1].therapyTypes)
        assertEquals(null, filters[2].ageGroup)
        assertEquals(null, filters[3].diagnosis)
        assertEquals(null, filters[4].insurance)
        assertEquals(PresenterCall.UseLosAngelesCatalog, controller.calls[5])
        assertEquals(PresenterCall.ClearAllFilters, controller.calls[6])
        assertEquals(PresenterCall.Retry, controller.calls[7])
    }
}
