package com.navigator.kindd.data.discovery

import com.navigator.kindd.data.models.Provider
import com.navigator.kindd.data.profile.AgeGroup
import com.navigator.kindd.data.profile.UserProfile

sealed interface DiscoveryOrigin {
    data class ProfileZip(val zipCode: String) : DiscoveryOrigin

    data class DeviceLocation(
        val latitude: Double,
        val longitude: Double
    ) : DiscoveryOrigin

    data object LosAngelesCatalog : DiscoveryOrigin
}

data class DiscoveryCriteria(
    val query: String = "",
    val therapyTypes: Set<TherapyType> = emptySet(),
    val ageGroup: AgeGroup? = null,
    val diagnosis: String? = null,
    val insurance: String? = null,
    val radiusMiles: Int = 15,
    val origin: DiscoveryOrigin = DiscoveryOrigin.LosAngelesCatalog
)

data class DiscoveryState(
    val profile: UserProfile = UserProfile(),
    val criteria: DiscoveryCriteria = DiscoveryCriteria(),
    val providers: List<Provider> = emptyList(),
    val isLoading: Boolean = false,
    val error: DiscoveryError? = null,
    val hasLoadedOnce: Boolean = false,
    val lastSuccessfulRequestKey: String? = null
) {
    val mapProviders: List<Provider>
        get() = providers.filter(Provider::hasValidCoordinates)
}
