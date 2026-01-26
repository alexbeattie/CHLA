package com.chla.kindd.data.models

import com.google.gson.annotations.SerializedName

data class Provider(
    val id: String,  // UUID from backend
    val name: String,
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
    val insuranceAccepted: List<String>? = null,
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

    val fullAddress: String
        get() = buildString {
            address?.let { append(it) }
            if (city != null || state != null || zipCode != null) {
                if (isNotEmpty()) append(", ")
                city?.let { append(it) }
                state?.let {
                    if (city != null) append(", ")
                    append(it)
                }
                zipCode?.let {
                    append(" ")
                    append(it)
                }
            }
        }

    val formattedDistance: String
        get() = distance?.let { String.format("%.1f mi", it) } ?: ""

    val hasCoordinates: Boolean
        get() = latitude != null && longitude != null
}

data class PaginatedResponse<T>(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: List<T>
)
