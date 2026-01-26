package com.chla.kindd.data.repository

import com.chla.kindd.data.api.KINDDApi
import com.chla.kindd.data.models.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
                Result.success(providers)
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
                val providers = api.getProvidersByRegionalCenter(
                    zipCode = zipCode,
                    therapyTypes = therapyTypes,
                    insurance = insurance,
                    ageGroup = ageGroup
                )
                Result.success(providers)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun searchProviders(query: String, limit: Int = 50): Result<List<Provider>> {
        return withContext(Dispatchers.IO) {
            try {
                val providers = api.searchProviders(query, limit)
                Result.success(providers)
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
