package com.chla.kindd.services

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.chla.kindd.data.source.UserCoordinates
import com.chla.kindd.data.source.UserLocationSource
import com.chla.kindd.di.IoDispatcher
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import java.util.Locale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

internal object LocationRequestPolicy {
    const val permission = "android.permission.ACCESS_COARSE_LOCATION"
    val priority: Int = Priority.PRIORITY_BALANCED_POWER_ACCURACY

    fun canRequest(coarsePermissionGranted: Boolean): Boolean = coarsePermissionGranted
}

class LocationService(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UserLocationSource {
    override fun hasLocationPermission(): Boolean {
        val coarsePermissionGranted = ContextCompat.checkSelfPermission(
            context,
            LocationRequestPolicy.permission
        ) == PackageManager.PERMISSION_GRANTED
        return LocationRequestPolicy.canRequest(coarsePermissionGranted)
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) return null

        return resolveCachedOrCurrentLocation(
            timeoutMillis = CURRENT_LOCATION_TIMEOUT_MS,
            cachedLocation = { fusedLocationClient.lastLocation.await() },
            currentLocation = { requestCurrentLocation() }
        )
    }

    @SuppressLint("MissingPermission")
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private suspend fun requestCurrentLocation(): Location? {
        val cancellationTokenSource = CancellationTokenSource()
        val request = CurrentLocationRequest.Builder()
            .setPriority(LocationRequestPolicy.priority)
            .setDurationMillis(CURRENT_LOCATION_TIMEOUT_MS)
            .setMaxUpdateAgeMillis(0)
            .build()
        return try {
            fusedLocationClient.getCurrentLocation(request, cancellationTokenSource.token)
                .await(cancellationTokenSource)
        } finally {
            cancellationTokenSource.cancel()
        }
    }

    override suspend fun currentCoordinates(): UserCoordinates? =
        getCurrentLocation()?.let { location ->
            UserCoordinates(location.latitude, location.longitude)
        }

    override suspend fun zipCodeFor(coordinates: UserCoordinates): String? =
        getZipCode(coordinates.latitude, coordinates.longitude)

    @SuppressLint("MissingPermission")
    fun getLocationUpdates(intervalMs: Long = 10000): Flow<Location> = callbackFlow {
        if (!hasLocationPermission()) {
            close()
            return@callbackFlow
        }

        val locationRequest = LocationRequest.Builder(
            LocationRequestPolicy.priority,
            intervalMs
        ).build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    @Suppress("DEPRECATION")
    suspend fun geocodeAddress(address: String): Location? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocationName(address, 1)
            addresses?.firstOrNull()?.let { addr ->
                Location("geocoder").apply {
                    latitude = addr.latitude
                    longitude = addr.longitude
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    suspend fun reverseGeocode(latitude: Double, longitude: Double): String? =
        withContext(ioDispatcher) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                addresses?.firstOrNull()?.let { addr ->
                    buildString {
                        addr.locality?.let { append(it) }
                        addr.adminArea?.let {
                            if (isNotEmpty()) append(", ")
                            append(it)
                        }
                        addr.postalCode?.let {
                            if (isNotEmpty()) append(" ")
                            append(it)
                        }
                    }
                }
            } catch (e: Exception) {
                null
            }
        }

    @Suppress("DEPRECATION")
    suspend fun getZipCode(latitude: Double, longitude: Double): String? =
        withContext(ioDispatcher) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                addresses?.firstOrNull()?.postalCode
            } catch (e: Exception) {
                null
            }
        }

    fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0] * 0.000621371f // Convert meters to miles
    }

    private companion object {
        const val CURRENT_LOCATION_TIMEOUT_MS = 10_000L
    }
}
