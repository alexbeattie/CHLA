package com.navigator.kindd.data.repository

import com.navigator.kindd.data.api.KINDDApi
import com.navigator.kindd.data.discovery.ComprehensiveProviderRequest
import com.navigator.kindd.data.discovery.RegionalCenterProviderRequest
import com.navigator.kindd.data.models.Provider
import com.navigator.kindd.data.source.ProviderDiscoveryDataSource
import com.navigator.kindd.di.IoDispatcher
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class ProviderRepository(
    private val api: KINDDApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ProviderDiscoveryDataSource {
    override suspend fun getProviderCatalog(
        limit: Int,
        page: Int
    ): Result<List<Provider>> = providerResult {
        api.getProviders(
            page = page,
            pageSize = limit.coerceAtLeast(1)
        ).results.take(limit.coerceAtLeast(0))
    }

    override suspend fun searchProviders(
        request: ComprehensiveProviderRequest,
        limit: Int
    ): Result<List<Provider>> {
        require((request.latitude == null) == (request.longitude == null))
        val hasCoordinates = request.latitude != null && request.longitude != null

        return providerResult {
            val providers = api.searchProviders(
                query = request.query,
                latitude = request.latitude,
                longitude = request.longitude,
                radiusMiles = request.radiusMiles.takeIf { hasCoordinates },
                therapyTypes = request.therapyTypes.takeIf { it.isNotEmpty() },
                ageGroup = request.ageGroup,
                diagnosis = request.diagnosis,
                insurance = request.insurance
            )

            if (hasCoordinates) {
                providers
                    .map { provider ->
                        provider.withDistanceFrom(
                            latitude = requireNotNull(request.latitude),
                            longitude = requireNotNull(request.longitude)
                        )
                    }
                    .sortedBy { provider -> provider.distance ?: Double.POSITIVE_INFINITY }
                    .take(limit.coerceAtLeast(0))
            } else {
                providers.take(limit.coerceAtLeast(0))
            }
        }
    }

    override suspend fun getProvidersByRegionalCenter(
        request: RegionalCenterProviderRequest,
        limit: Int
    ): Result<List<Provider>> = providerResult {
        api.getProvidersByRegionalCenter(
            zipCode = request.zipCode,
            insurance = request.insurance,
            ageGroup = request.ageGroup,
            diagnosis = request.diagnosis
        ).results.take(limit.coerceAtLeast(0))
    }

    suspend fun getProviders(page: Int = 1, pageSize: Int = 50): Result<List<Provider>> =
        getProviderCatalog(limit = pageSize, page = page)

    suspend fun getProvidersNearby(
        latitude: Double,
        longitude: Double,
        radiusMiles: Int = 25,
        limit: Int = 50
    ): Result<List<Provider>> = searchProviders(
        request = ComprehensiveProviderRequest(
            latitude = latitude,
            longitude = longitude,
            radiusMiles = radiusMiles
        ),
        limit = limit
    )

    suspend fun searchProviders(query: String, limit: Int = 50): Result<List<Provider>> =
        searchProviders(
            request = ComprehensiveProviderRequest(query = query),
            limit = limit
        )

    suspend fun getProvider(id: String): Result<Provider> = providerResult {
        api.getProvider(id)
    }

    private suspend fun <T> providerResult(block: suspend () -> T): Result<T> =
        withContext(ioDispatcher) {
            try {
                Result.success(block())
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                Result.failure(exception)
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
