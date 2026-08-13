package com.navigator.kindd.data.source

import com.navigator.kindd.data.models.RegionalCenter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class FakeRegionalCenterDataSource(
    var lookupResult: RegionalCenterLookup = RegionalCenterLookup.Unmatched
) : RegionalCenterDataSource {
    val lookedUpZipCodes = mutableListOf<String>()
    var lookupGate: CompletableDeferred<Unit>? = null
    var ignoreLookupCancellation: Boolean = false

    override suspend fun getRegionalCenters(): Result<List<RegionalCenter>> =
        Result.success(emptyList())

    override suspend fun getRegionalCentersNearby(
        latitude: Double,
        longitude: Double
    ): Result<List<RegionalCenter>> = Result.success(emptyList())

    override suspend fun lookupRegionalCenter(zipCode: String): RegionalCenterLookup {
        lookedUpZipCodes += zipCode
        lookupGate?.let { gate ->
            if (ignoreLookupCancellation) {
                withContext(NonCancellable) { gate.await() }
            } else {
                gate.await()
            }
        }
        return lookupResult
    }
}
