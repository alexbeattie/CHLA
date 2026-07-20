package com.chla.kindd.data.source

import com.chla.kindd.data.models.RegionalCenter

class FakeRegionalCenterDataSource(
    var lookupResult: RegionalCenterLookup = RegionalCenterLookup.Unmatched
) : RegionalCenterDataSource {
    val lookedUpZipCodes = mutableListOf<String>()

    override suspend fun getRegionalCenters(): Result<List<RegionalCenter>> =
        Result.success(emptyList())

    override suspend fun getRegionalCentersNearby(
        latitude: Double,
        longitude: Double
    ): Result<List<RegionalCenter>> = Result.success(emptyList())

    override suspend fun lookupRegionalCenter(zipCode: String): RegionalCenterLookup {
        lookedUpZipCodes += zipCode
        return lookupResult
    }
}
