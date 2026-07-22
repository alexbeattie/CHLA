package com.navigator.kindd.data.source

import com.navigator.kindd.data.models.RegionalCenter

interface RegionalCenterDataSource {
    suspend fun getRegionalCenters(): Result<List<RegionalCenter>>

    suspend fun getRegionalCentersNearby(
        latitude: Double,
        longitude: Double
    ): Result<List<RegionalCenter>>

    suspend fun lookupRegionalCenter(zipCode: String): RegionalCenterLookup
}

sealed interface RegionalCenterLookup {
    data class Matched(val center: RegionalCenter) : RegionalCenterLookup
    data object Unmatched : RegionalCenterLookup
    data class Unavailable(val reason: LookupFailure) : RegionalCenterLookup
}

enum class LookupFailure {
    NETWORK,
    TIMEOUT,
    SERVER,
    UNKNOWN
}
