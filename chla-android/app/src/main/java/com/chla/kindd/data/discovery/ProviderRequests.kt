package com.chla.kindd.data.discovery

data class ComprehensiveProviderRequest(
    val query: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMiles: Int = 15,
    val therapyTypes: List<String> = emptyList(),
    val ageGroup: String? = null,
    val diagnosis: String? = null,
    val insurance: String? = null
) {
    init {
        require((latitude == null) == (longitude == null))
    }
}

data class RegionalCenterProviderRequest(
    val zipCode: String,
    val ageGroup: String? = null,
    val diagnosis: String? = null,
    val insurance: String? = null
)
