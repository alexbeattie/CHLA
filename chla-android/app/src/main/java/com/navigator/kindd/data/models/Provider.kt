package com.navigator.kindd.data.models

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

data class Provider(
    val id: String,  // UUID from backend
    val name: String,
    val type: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val website: String? = null,
    val description: String? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    @SerializedName("zip_code")
    val zipCode: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerializedName("therapy_types")
    val therapyTypes: List<String>? = null,
    @SerializedName("age_groups")
    val ageGroups: List<String>? = null,
    @SerializedName("insurance_accepted")
    @JsonAdapter(StringListJsonAdapter::class)
    private val legacyInsuranceAccepted: List<String>? = null,
    @SerializedName("insurance_carriers")
    private val insuranceCarriers: List<String>? = null,
    @SerializedName("diagnoses_treated")
    val diagnosesTreated: List<String>? = null,
    @SerializedName("service_models")
    val serviceModels: List<String>? = null,
    @SerializedName("regional_center")
    val regionalCenter: String? = null,
    val distance: Double? = null
) {
    val formattedPhone: String
        get() {
            val cleaned = phone?.replace(Regex("[^0-9]"), "") ?: return ""
            return if (cleaned.length == 10) {
                "(${cleaned.substring(0, 3)}) ${cleaned.substring(3, 6)}-${cleaned.substring(6)}"
            } else {
                phone ?: ""
            }
        }

    val displayAddressLines: List<String>
        get() = formattedAddress.lines

    val fullAddress: String
        get() = formattedAddress.singleLine

    private val formattedAddress: ProviderAddress
        get() = ProviderAddressFormatter.format(address, city, state, zipCode)

    val formattedDistance: String
        get() = distance?.let { String.format("%.1f mi", it) } ?: ""

    val insuranceAccepted: List<String>
        get() {
            val normalizedCarriers = insuranceCarriers.orEmpty()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .distinctBy { it.lowercase() }

            if (normalizedCarriers.isNotEmpty()) return normalizedCarriers

            return legacyInsuranceAccepted.orEmpty()
                .map { it.trim().trim('"', '{', '}', '[', ']') }
                .filter(String::isNotEmpty)
                .distinctBy { it.lowercase() }
        }

    val hasValidCoordinates: Boolean
        get() {
            val latitude = latitude ?: return false
            val longitude = longitude ?: return false
            return latitude.isFinite() &&
                longitude.isFinite() &&
                latitude in -90.0..90.0 &&
                longitude in -180.0..180.0 &&
                !(latitude == 0.0 && longitude == 0.0)
        }

    val hasCoordinates: Boolean
        get() = hasValidCoordinates
}

data class PaginatedResponse<T>(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<T>
)
