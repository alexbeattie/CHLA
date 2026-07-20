package com.chla.kindd.data.discovery

import com.chla.kindd.R
import com.chla.kindd.data.models.Provider
import com.chla.kindd.data.profile.AgeGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class DiscoveryCatalogTest {

    @Test
    fun `age catalog uses exact API values and order`() {
        assertEquals(
            listOf("0-5", "6-12", "13-18", "19+", "All Ages"),
            DiscoveryCatalog.ageGroups.map(AgeGroup::apiValue)
        )
    }

    @Test
    fun `diagnosis catalog uses exact values and order`() {
        assertEquals(
            listOf(
                "Autism Spectrum Disorder",
                "Global Development Delay",
                "Intellectual Disability",
                "Speech and Language Disorder",
                "Other"
            ),
            DiscoveryCatalog.diagnoses
        )
    }

    @Test
    fun `insurance catalog uses exact values and order`() {
        assertEquals(
            listOf(
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
            ),
            DiscoveryCatalog.insurances
        )
    }

    @Test
    fun `therapy catalog uses exact canonical API values and order`() {
        assertEquals(
            listOf(
                "ABA therapy",
                "Speech therapy",
                "Occupational therapy",
                "Physical therapy",
                "Feeding therapy",
                "Parent child interaction therapy/parent training behavior management"
            ),
            DiscoveryCatalog.therapyTypes.map(TherapyType::apiValue)
        )
    }

    @Test
    fun `therapy display copy is addressed only through localized resources`() {
        assertEquals(
            listOf(
                R.string.aba_therapy,
                R.string.speech_therapy,
                R.string.occupational_therapy,
                R.string.physical_therapy,
                R.string.feeding_therapy,
                R.string.parent_child_interaction_therapy
            ),
            DiscoveryCatalog.therapyTypes.map(TherapyType::displayResId)
        )
    }

    @Test
    fun `discovery state map providers are only the coordinate-bearing subset`() {
        val bothCoordinates = Provider(
            id = "both",
            name = "Both",
            latitude = 34.0,
            longitude = -118.0
        )
        val latitudeOnly = Provider(
            id = "latitude",
            name = "Latitude",
            latitude = 34.0
        )
        val longitudeOnly = Provider(
            id = "longitude",
            name = "Longitude",
            longitude = -118.0
        )

        val state = DiscoveryState(
            providers = listOf(bothCoordinates, latitudeOnly, longitudeOnly)
        )

        assertEquals(listOf("both"), state.mapProviders.map(Provider::id))
    }

    @Test
    fun `discovery errors are closed categories without detail payloads`() {
        assertEquals(
            listOf("NETWORK", "TIMEOUT", "SERVER", "UNKNOWN"),
            DiscoveryError.entries.map(DiscoveryError::name)
        )
        DiscoveryError.entries.forEach { error ->
            assertFalse(error.javaClass.declaredFields.any { field ->
                field.type == String::class.java && field.name != "name"
            })
        }
    }

    @Test
    fun `default age remains null rather than becoming All Ages`() {
        assertNull(DiscoveryCriteria().ageGroup)
        assertEquals(AgeGroup.ALL_AGES, DiscoveryCriteria(ageGroup = AgeGroup.ALL_AGES).ageGroup)
    }
}
