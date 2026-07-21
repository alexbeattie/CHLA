package com.chla.kindd.data.servicearea

/** Android-neutral location in a GeoJSON service-area boundary. */
data class ServiceAreaCoordinate(
    val latitude: Double,
    val longitude: Double
)

/** A regional-center catchment area, including every renderable outer polygon ring. */
data class ServiceAreaFeature(
    val id: Int,
    val name: String,
    val acronym: String,
    val description: String,
    val polygons: List<List<ServiceAreaCoordinate>>
)

interface ServiceAreaDataSource {
    suspend fun getServiceAreas(): Result<List<ServiceAreaFeature>>
}
