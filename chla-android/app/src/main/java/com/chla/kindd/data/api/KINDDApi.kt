package com.chla.kindd.data.api

import com.chla.kindd.data.models.PaginatedResponse
import com.chla.kindd.data.models.Provider
import com.chla.kindd.data.models.RegionalCenter
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface KINDDApi {

    // Health Check
    @GET("health/")
    suspend fun healthCheck(): HealthCheckResponse

    // Providers
    @GET("providers-v2/")
    suspend fun getProviders(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50
    ): PaginatedResponse<Provider>

    @GET("providers-v2/nearby/")
    suspend fun getProvidersNearby(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("radius") radiusMiles: Int = 25,
        @Query("limit") limit: Int = 50
    ): List<Provider>

    @GET("providers-v2/by_regional_center/")
    suspend fun getProvidersByRegionalCenter(
        @Query("zip_code") zipCode: String,
        @Query("therapy") therapyTypes: List<String>? = null,
        @Query("insurance") insurance: String? = null,
        @Query("age_group") ageGroup: String? = null
    ): List<Provider>

    @GET("providers-v2/search/")
    suspend fun searchProviders(
        @Query("q") query: String,
        @Query("limit") limit: Int = 50
    ): List<Provider>

    @GET("providers-v2/{id}/")
    suspend fun getProvider(
        @retrofit2.http.Path("id") id: String
    ): Provider

    // Regional Centers
    @GET("regional-centers/")
    suspend fun getRegionalCenters(): PaginatedResponse<RegionalCenter>

    @GET("regional-centers/by_zip/")
    suspend fun getRegionalCenterByZip(
        @Query("zip_code") zipCode: String
    ): RegionalCenter

    @GET("regional-centers/nearby/")
    suspend fun getRegionalCentersNearby(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double
    ): List<RegionalCenter>

    // LLM
    @POST("llm/ask/")
    suspend fun askLLM(
        @Body request: LLMRequest
    ): LLMResponse

    @POST("llm/stream/")
    suspend fun streamLLM(
        @Body request: LLMRequest
    ): okhttp3.ResponseBody
}

data class HealthCheckResponse(
    val status: String,
    val database: String? = null,
    val version: String? = null
)

data class LLMRequest(
    val query: String,
    val context: Map<String, Any?>? = null,
    val locale: String = "en"
)

data class LLMResponse(
    val query: String,
    val answer: String,
    @com.google.gson.annotations.SerializedName("providers_referenced")
    val providersReferenced: List<String>? = null,  // UUIDs from backend
    @com.google.gson.annotations.SerializedName("regional_center")
    val regionalCenter: String? = null
)
