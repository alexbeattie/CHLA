package com.chla.kindd.data.profile

import com.chla.kindd.data.models.RegionalCenter

enum class AudienceType(val storageValue: String) {
    FAMILY("family"),
    CLINICIAN("clinician");

    companion object {
        fun fromStorageValue(value: String?): AudienceType? = when (value) {
            "family" -> FAMILY
            "clinician" -> CLINICIAN
            else -> null
        }
    }
}

enum class JourneyStage(val storageValue: String) {
    JUST_DIAGNOSED("justDiagnosed"),
    WAITING_FOR_INTAKE("waitingIntake"),
    RECEIVING_SERVICES("receivingServices"),
    EXPLORING("exploring");

    companion object {
        fun fromStorageValue(value: String?): JourneyStage? = when (value) {
            "justDiagnosed" -> JUST_DIAGNOSED
            "waitingIntake" -> WAITING_FOR_INTAKE
            "receivingServices" -> RECEIVING_SERVICES
            "exploring" -> EXPLORING
            else -> null
        }
    }
}

enum class AgeGroup(val apiValue: String) {
    EARLY_INTERVENTION("0-5"),
    SCHOOL_AGE("6-12"),
    ADOLESCENT("13-18"),
    ADULT("19+"),
    ALL_AGES("All Ages");

    companion object {
        fun fromStorageValue(value: String?): AgeGroup? = when (value) {
            "0-5" -> EARLY_INTERVENTION
            "6-12" -> SCHOOL_AGE
            "13-18" -> ADOLESCENT
            "19+" -> ADULT
            "All Ages" -> ALL_AGES
            else -> null
        }
    }
}

data class RegionalCenterIdentity(
    val id: Int,
    val name: String,
    val shortName: String
) {
    companion object {
        fun from(center: RegionalCenter): RegionalCenterIdentity = RegionalCenterIdentity(
            id = center.id,
            name = center.name,
            shortName = canonicalShortName(center.name)
        )

        private fun canonicalShortName(name: String): String = when {
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
}

data class UserProfile(
    val onboardingCompleted: Boolean = false,
    val audienceType: AudienceType? = null,
    val zipCode: String? = null,
    val regionalCenter: RegionalCenterIdentity? = null,
    val journeyStage: JourneyStage? = null,
    val ageGroup: AgeGroup? = null
) {
    val isComplete: Boolean
        get() = onboardingCompleted &&
            audienceType != null &&
            zipCode?.matches(Regex("[0-9]{5}")) == true &&
            journeyStage != null
}
