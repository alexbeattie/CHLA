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

    @GET("providers-v2/comprehensive_search/")
    suspend fun searchProviders(
        @Query("q") query: String? = null,
        @Query("lat") latitude: Double? = null,
        @Query("lng") longitude: Double? = null,
        @Query("radius") radiusMiles: Int? = null,
        @Query("therapy") therapyTypes: List<String>? = null,
        @Query("age") ageGroup: String? = null,
        @Query("diagnosis") diagnosis: String? = null,
        @Query("insurance") insurance: String? = null
    ): List<Provider>

    @GET("providers-v2/by_regional_center/")
    suspend fun getProvidersByRegionalCenter(
        @Query("zip_code") zipCode: String,
        @Query("insurance") insurance: String? = null,
        @Query("age") ageGroup: String? = null,
        @Query("diagnosis") diagnosis: String? = null
    ): RegionalCenterProvidersResponse

    @GET("providers-v2/{id}/")
    suspend fun getProvider(
        @retrofit2.http.Path("id") id: String
    ): Provider

    // Regional Centers
    @GET("regional-centers/")
    suspend fun getRegionalCenters(): PaginatedResponse<RegionalCenter>

    @GET("regional-centers/by_zip_code/")
    suspend fun getRegionalCenterByZip(
        @Query("zip_code") zipCode: String
    ): RegionalCenter

    @GET("regional-centers/nearby/")
    suspend fun getRegionalCentersNearby(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double
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

    @POST("llm/response-reports/")
    suspend fun reportAssistantResponse(
        @Body request: AssistantResponseReportRequest
    ): AssistantResponseReportResponse
}

data class RegionalCenterProvidersResponse(
    val count: Int,
    val results: List<Provider>
)

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
    val regionalCenter: String? = null,
    @com.google.gson.annotations.SerializedName("response_fingerprint")
    val responseFingerprint: String? = null
)

enum class AssistantResponseReportReason {
    @com.google.gson.annotations.SerializedName("unsafe_or_inappropriate")
    UNSAFE_OR_INAPPROPRIATE,

    @com.google.gson.annotations.SerializedName("inaccurate_or_misleading")
    INACCURATE_OR_MISLEADING,

    @com.google.gson.annotations.SerializedName("other")
    OTHER
}

data class AssistantResponseReportRequest(
    val reason: AssistantResponseReportReason,
    @com.google.gson.annotations.SerializedName("reported_response")
    val reportedResponse: String,
    val locale: String,
    val platform: String = "android",
    @com.google.gson.annotations.SerializedName("app_version")
    val appVersion: String,
    @com.google.gson.annotations.SerializedName("response_fingerprint")
    val responseFingerprint: String
)

data class AssistantResponseReportResponse(
    val id: Long,
    val status: String
)
