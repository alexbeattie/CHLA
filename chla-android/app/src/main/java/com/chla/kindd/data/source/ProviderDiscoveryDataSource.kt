package com.chla.kindd.data.source

import com.chla.kindd.data.discovery.ComprehensiveProviderRequest
import com.chla.kindd.data.discovery.RegionalCenterProviderRequest
import com.chla.kindd.data.models.Provider

interface ProviderDiscoveryDataSource {
    suspend fun getProviderCatalog(limit: Int, page: Int = 1): Result<List<Provider>>

    suspend fun searchProviders(
        request: ComprehensiveProviderRequest,
        limit: Int
    ): Result<List<Provider>>

    suspend fun getProvidersByRegionalCenter(
        request: RegionalCenterProviderRequest,
        limit: Int
    ): Result<List<Provider>>
}
