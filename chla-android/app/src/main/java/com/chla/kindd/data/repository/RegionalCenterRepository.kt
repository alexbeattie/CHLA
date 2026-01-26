package com.chla.kindd.data.repository

import com.chla.kindd.data.api.KINDDApi
import com.chla.kindd.data.models.RegionalCenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
class RegionalCenterRepository(
    private val api: KINDDApi
) {
    suspend fun getRegionalCenters(): Result<List<RegionalCenter>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getRegionalCenters()
                Result.success(response.results)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getRegionalCenterByZip(zipCode: String): Result<RegionalCenter> {
        return withContext(Dispatchers.IO) {
            try {
                val center = api.getRegionalCenterByZip(zipCode)
                Result.success(center)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun getRegionalCentersNearby(
        latitude: Double,
        longitude: Double
    ): Result<List<RegionalCenter>> {
        return withContext(Dispatchers.IO) {
            try {
                val centers = api.getRegionalCentersNearby(latitude, longitude)
                Result.success(centers)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
