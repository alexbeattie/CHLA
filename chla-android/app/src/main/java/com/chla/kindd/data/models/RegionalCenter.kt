package com.chla.kindd.data.models

import androidx.compose.ui.graphics.Color
import com.chla.kindd.ui.theme.*
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

data class RegionalCenter(
    val id: Int,
    @SerializedName("regional_center")
    val name: String,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    @SerializedName("zip_code")
    val zipCode: String? = null,
    val telephone: String? = null,
    val website: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerializedName("zip_codes")
    @JsonAdapter(StringListJsonAdapter::class)
    val zipCodes: List<String>? = null,
    @SerializedName("service_areas")
    val serviceAreas: List<String>? = null,
    @SerializedName("county_served")
    val countyServed: String? = null,
    val distance: Double? = null
) {
    val formattedPhone: String
        get() {
            val cleaned = telephone?.replace(Regex("[^0-9]"), "") ?: return ""
            return if (cleaned.length == 10) {
                "(${cleaned.substring(0, 3)}) ${cleaned.substring(3, 6)}-${cleaned.substring(6)}"
            } else {
                telephone ?: ""
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

    val color: Color
        get() = when {
            name.contains("Westside", ignoreCase = true) -> WestsideRC
            name.contains("Harbor", ignoreCase = true) -> HarborRC
            name.contains("South Central", ignoreCase = true) -> SouthCentralRC
            name.contains("Eastern", ignoreCase = true) -> EasternRC
            name.contains("North", ignoreCase = true) -> NorthLARC
            name.contains("Lanterman", ignoreCase = true) -> LantermanRC
            name.contains("San Gabriel", ignoreCase = true) || name.contains("Pomona", ignoreCase = true) -> SanGabrielRC
            else -> CHLABlue
        }

    val shortName: String
        get() = when {
            name.contains("North Los Angeles", ignoreCase = true) -> "NLACRC"
            name.contains("Westside", ignoreCase = true) -> "WRC"
            name.contains("South Central", ignoreCase = true) -> "SCLARC"
            name.contains("Eastern Los Angeles", ignoreCase = true) -> "ELARC"
            name.contains("Harbor", ignoreCase = true) -> "HRC"
            name.contains("Lanterman", ignoreCase = true) -> "FDLRC"
            name.equals("SG/PRC", ignoreCase = true) ||
                name.contains("San Gabriel", ignoreCase = true) ||
                name.contains("Pomona", ignoreCase = true) -> "SGPRC"
            else -> name
        }
}
