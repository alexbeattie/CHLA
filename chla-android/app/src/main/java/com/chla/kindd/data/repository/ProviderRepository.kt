package com.chla.kindd.data.repository

import com.chla.kindd.data.api.KINDDApi
import com.chla.kindd.data.models.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class ProviderRepository(
    private val api: KINDDApi
) {
    suspend fun getProviders(page: Int = 1, pageSize: Int = 50): Result<List<Provider>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getProviders(page, pageSize)
                Result.success(response.results)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getProvidersNearby(
        latitude: Double,
        longitude: Double,
        radiusMiles: Int = 25,
        limit: Int = 50
    ): Result<List<Provider>> {
        return withContext(Dispatchers.IO) {
            try {
                val providers = api.getProvidersNearby(latitude, longitude, radiusMiles, limit)
                val nearestProviders = providers
                    .map { provider -> provider.withDistanceFrom(latitude, longitude) }
                    .sortedBy { provider -> provider.distance ?: Double.POSITIVE_INFINITY }
                    .take(limit.coerceAtLeast(0))
                Result.success(nearestProviders)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getProvidersByRegionalCenter(
        zipCode: String,
        therapyTypes: List<String>? = null,
        insurance: String? = null,
        ageGroup: String? = null
    ): Result<List<Provider>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getProvidersByRegionalCenter(
                    zipCode = zipCode,
                    therapyTypes = therapyTypes,
                    insurance = insurance,
                    ageGroup = ageGroup
                )
                Result.success(response.results)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun searchProviders(query: String, limit: Int = 50): Result<List<Provider>> {
        return withContext(Dispatchers.IO) {
            try {
                val providers = api.searchProviders(query, limit)
                Result.success(providers.take(limit.coerceAtLeast(0)))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getProvider(id: String): Result<Provider> {
        return withContext(Dispatchers.IO) {
            try {
                val provider = api.getProvider(id)
                Result.success(provider)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

private fun Provider.withDistanceFrom(latitude: Double, longitude: Double): Provider {
    val providerLatitude = this.latitude ?: return this
    val providerLongitude = this.longitude ?: return this
    val latitudeDelta = Math.toRadians(providerLatitude - latitude)
    val longitudeDelta = Math.toRadians(providerLongitude - longitude)
    val originLatitude = Math.toRadians(latitude)
    val destinationLatitude = Math.toRadians(providerLatitude)

    val haversine = (
        sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
            cos(originLatitude) * cos(destinationLatitude) *
            sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
        ).coerceIn(0.0, 1.0)
    val angularDistance = 2 * atan2(sqrt(haversine), sqrt(1 - haversine))

    return copy(distance = EARTH_RADIUS_MILES * angularDistance)
}

private const val EARTH_RADIUS_MILES = 3_958.8
