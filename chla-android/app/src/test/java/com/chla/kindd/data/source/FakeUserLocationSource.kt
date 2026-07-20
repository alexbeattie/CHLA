package com.chla.kindd.data.source

class FakeUserLocationSource(
    var permissionGranted: Boolean = false,
    var coordinates: UserCoordinates? = null,
    var zipCode: String? = null
) : UserLocationSource {
    var coordinatesFailure: Throwable? = null
    var zipCodeFailure: Throwable? = null
    var currentCoordinatesCalls = 0
    val geocodedCoordinates = mutableListOf<UserCoordinates>()

    override fun hasLocationPermission(): Boolean = permissionGranted

    override suspend fun currentCoordinates(): UserCoordinates? {
        currentCoordinatesCalls += 1
        coordinatesFailure?.let { throw it }
        return coordinates
    }

    override suspend fun zipCodeFor(coordinates: UserCoordinates): String? {
        geocodedCoordinates += coordinates
        zipCodeFailure?.let { throw it }
        return zipCode
    }
}
