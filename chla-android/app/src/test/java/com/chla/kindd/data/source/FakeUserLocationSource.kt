package com.chla.kindd.data.source

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

class FakeUserLocationSource(
    var permissionGranted: Boolean = false,
    var coordinates: UserCoordinates? = null,
    var zipCode: String? = null
) : UserLocationSource {
    var coordinatesFailure: Throwable? = null
    var zipCodeFailure: Throwable? = null
    var coordinatesGate: CompletableDeferred<Unit>? = null
    var zipCodeGate: CompletableDeferred<Unit>? = null
    var ignoreCancellation: Boolean = false
    var currentCoordinatesCalls = 0
    val geocodedCoordinates = mutableListOf<UserCoordinates>()

    override fun hasLocationPermission(): Boolean = permissionGranted

    override suspend fun currentCoordinates(): UserCoordinates? {
        currentCoordinatesCalls += 1
        awaitGate(coordinatesGate)
        coordinatesFailure?.let { throw it }
        return coordinates
    }

    override suspend fun zipCodeFor(coordinates: UserCoordinates): String? {
        geocodedCoordinates += coordinates
        awaitGate(zipCodeGate)
        zipCodeFailure?.let { throw it }
        return zipCode
    }

    private suspend fun awaitGate(gate: CompletableDeferred<Unit>?) {
        gate ?: return
        if (ignoreCancellation) {
            withContext(NonCancellable) { gate.await() }
        } else {
            gate.await()
        }
    }
}
