package com.navigator.kindd.data.source

import com.navigator.kindd.data.discovery.ComprehensiveProviderRequest
import com.navigator.kindd.data.discovery.RegionalCenterProviderRequest
import com.navigator.kindd.data.models.Provider

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
