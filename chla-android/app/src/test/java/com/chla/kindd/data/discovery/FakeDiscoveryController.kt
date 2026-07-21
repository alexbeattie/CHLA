package com.chla.kindd.data.discovery

import com.chla.kindd.data.profile.AgeGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeDiscoveryController(
    initialState: DiscoveryState = DiscoveryState()
) : DiscoveryController {
    private val mutableState = MutableStateFlow(initialState)
    override val state: StateFlow<DiscoveryState> = mutableState

    val calls = mutableListOf<String>()
    val singleTherapies = mutableListOf<TherapyType>()
    var onSingleTherapyAndRefresh: ((TherapyType) -> Unit)? = null

    override fun ensureLoaded() { calls += "ensureLoaded" }
    override fun setQuery(query: String) { calls += "query" }
    override fun applyFilters(
        therapyTypes: Set<TherapyType>,
        ageGroup: AgeGroup?,
        diagnosis: String?,
        insurance: String?,
        radiusMiles: Int
    ) { calls += "filters" }
    override fun setSingleTherapyAndRefresh(therapyType: TherapyType) {
        singleTherapies += therapyType
        calls += "therapy:${therapyType.apiValue}"
        onSingleTherapyAndRefresh?.invoke(therapyType)
    }
    override fun useDeviceLocation(latitude: Double, longitude: Double) { calls += "location" }
    override fun useLosAngelesCatalog() { calls += "catalog" }
    override fun refresh() { calls += "refresh" }
    override fun retry() { calls += "retry" }
    override fun clearAllFilters() { calls += "clear" }
}
