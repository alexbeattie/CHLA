package com.navigator.kindd.ui.screens

import androidx.lifecycle.ViewModel
import com.navigator.kindd.data.discovery.DiscoveryController
import com.navigator.kindd.data.discovery.DiscoveryCriteria
import com.navigator.kindd.data.discovery.DiscoveryState
import com.navigator.kindd.data.discovery.TherapyType
import com.navigator.kindd.data.models.Provider
import com.navigator.kindd.data.profile.AgeGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ProviderListSort {
    NAME,
    DISTANCE
}

@HiltViewModel
class ProviderListViewModel @Inject constructor(
    private val discoveryController: DiscoveryController
) : ViewModel() {

    val state: StateFlow<DiscoveryState> = discoveryController.state

    private val mutableSort = MutableStateFlow(ProviderListSort.NAME)
    val sort: StateFlow<ProviderListSort> = mutableSort.asStateFlow()

    val providers: List<Provider>
        get() = sortedProviders(state.value.providers)

    fun onFirstAppearance() = discoveryController.ensureLoaded()

    fun setQuery(query: String) = discoveryController.setQuery(query)

    fun setSort(sort: ProviderListSort) {
        mutableSort.value = sort
    }

    fun sortedProviders(providers: List<Provider>): List<Provider> = when (sort.value) {
        ProviderListSort.NAME -> providers.sortedWith(
            compareBy(String.CASE_INSENSITIVE_ORDER, Provider::name)
        )
        ProviderListSort.DISTANCE -> providers.sortedWith(
            compareBy<Provider> { it.distance == null }
                .thenBy { it.distance }
                .thenBy(String.CASE_INSENSITIVE_ORDER, Provider::name)
        )
    }

    fun applyFilters(
        therapyTypes: Set<TherapyType>,
        ageGroup: AgeGroup?,
        diagnosis: String?,
        insurance: String?,
        radiusMiles: Int
    ) = discoveryController.applyFilters(
        therapyTypes = therapyTypes,
        ageGroup = ageGroup,
        diagnosis = diagnosis,
        insurance = insurance,
        radiusMiles = radiusMiles
    )

    fun removeTherapy(therapyType: TherapyType) = updateFilters {
        copy(therapyTypes = therapyTypes - therapyType)
    }

    fun removeAge() = updateFilters { copy(ageGroup = null) }

    fun removeDiagnosis() = updateFilters { copy(diagnosis = null) }

    fun removeInsurance() = updateFilters { copy(insurance = null) }

    fun removeRadius() = discoveryController.useLosAngelesCatalog()

    fun clearAllFilters() = discoveryController.clearAllFilters()

    fun retry() = discoveryController.retry()

    private inline fun updateFilters(
        transform: DiscoveryCriteria.() -> DiscoveryCriteria
    ) {
        val criteria = state.value.criteria.transform()
        applyFilters(
            therapyTypes = criteria.therapyTypes,
            ageGroup = criteria.ageGroup,
            diagnosis = criteria.diagnosis,
            insurance = criteria.insurance,
            radiusMiles = criteria.radiusMiles
        )
    }
}
