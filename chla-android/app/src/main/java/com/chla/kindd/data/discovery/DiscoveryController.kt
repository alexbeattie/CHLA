package com.chla.kindd.data.discovery

import com.chla.kindd.data.profile.AgeGroup
import kotlinx.coroutines.flow.StateFlow

interface DiscoveryController {
    val state: StateFlow<DiscoveryState>

    fun ensureLoaded()

    fun setQuery(query: String)

    fun applyFilters(
        therapyTypes: Set<TherapyType>,
        ageGroup: AgeGroup?,
        diagnosis: String?,
        insurance: String?,
        radiusMiles: Int
    )

    fun setSingleTherapyAndRefresh(therapyType: TherapyType)

    fun useDeviceLocation(latitude: Double, longitude: Double)

    fun useLosAngelesCatalog()

    fun refresh()

    fun retry()

    fun clearAllFilters()
}
