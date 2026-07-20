package com.chla.kindd.data.source

data class UserCoordinates(
    val latitude: Double,
    val longitude: Double
)

interface UserLocationSource {
    fun hasLocationPermission(): Boolean

    suspend fun currentCoordinates(): UserCoordinates?

    suspend fun zipCodeFor(coordinates: UserCoordinates): String?
}
