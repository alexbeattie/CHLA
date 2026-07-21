package com.chla.kindd.data.discovery

import androidx.annotation.StringRes
import com.chla.kindd.R
import com.chla.kindd.data.profile.AgeGroup

enum class TherapyType(
    val apiValue: String,
    @StringRes val displayResId: Int
) {
    ABA("ABA therapy", R.string.aba_therapy),
    SPEECH("Speech therapy", R.string.speech_therapy),
    OCCUPATIONAL("Occupational therapy", R.string.occupational_therapy),
    PHYSICAL("Physical therapy", R.string.physical_therapy),
    FEEDING("Feeding therapy", R.string.feeding_therapy),
    PARENT_TRAINING(
        "Parent child interaction therapy/parent training behavior management",
        R.string.parent_child_interaction_therapy
    )
}

object DiscoveryCatalog {
    val ageGroups: List<AgeGroup> = AgeGroup.entries

    val diagnoses: List<String> = listOf(
        "Autism Spectrum Disorder",
        "Global Development Delay",
        "Intellectual Disability",
        "Speech and Language Disorder",
        "Other"
    )

    val insurances: List<String> = listOf(
        "Regional Center",
        "Private Pay",
        "Medi-Cal",
        "Medicare",
        "Blue Cross",
        "Blue Shield",
        "Anthem",
        "Aetna",
        "Cigna",
        "Kaiser Permanente",
        "United Healthcare",
        "Health Net",
        "Molina",
        "L.A. Care",
        "Covered California"
    )

    val therapyTypes: List<TherapyType> = TherapyType.entries
}
