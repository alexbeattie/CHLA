package com.chla.kindd.data.profile

import com.chla.kindd.data.models.RegionalCenter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileModelsTest {

    @Test
    fun `audience types use explicit stable storage values`() {
        assertEquals("family", AudienceType.FAMILY.storageValue)
        assertEquals("clinician", AudienceType.CLINICIAN.storageValue)
        assertEquals(AudienceType.FAMILY, AudienceType.fromStorageValue("family"))
        assertEquals(AudienceType.CLINICIAN, AudienceType.fromStorageValue("clinician"))
        assertNull(AudienceType.fromStorageValue("unknown"))
    }

    @Test
    fun `journey stages use explicit stable storage values`() {
        assertEquals("justDiagnosed", JourneyStage.JUST_DIAGNOSED.storageValue)
        assertEquals("waitingIntake", JourneyStage.WAITING_FOR_INTAKE.storageValue)
        assertEquals("receivingServices", JourneyStage.RECEIVING_SERVICES.storageValue)
        assertEquals("exploring", JourneyStage.EXPLORING.storageValue)

        JourneyStage.entries.forEach { stage ->
            assertEquals(stage, JourneyStage.fromStorageValue(stage.storageValue))
        }
        assertNull(JourneyStage.fromStorageValue("unknown"))
    }

    @Test
    fun `age groups use exact API values`() {
        assertEquals("0-5", AgeGroup.EARLY_INTERVENTION.apiValue)
        assertEquals("6-12", AgeGroup.SCHOOL_AGE.apiValue)
        assertEquals("13-18", AgeGroup.ADOLESCENT.apiValue)
        assertEquals("19+", AgeGroup.ADULT.apiValue)
        assertEquals("All Ages", AgeGroup.ALL_AGES.apiValue)

        AgeGroup.entries.forEach { ageGroup ->
            assertEquals(ageGroup, AgeGroup.fromStorageValue(ageGroup.apiValue))
        }
        assertNull(AgeGroup.fromStorageValue("unknown"))
    }

    @Test
    fun `profile is complete only with completion audience ASCII ZIP and journey`() {
        val complete = UserProfile(
            onboardingCompleted = true,
            audienceType = AudienceType.FAMILY,
            zipCode = "90001",
            journeyStage = JourneyStage.EXPLORING
        )

        assertTrue(complete.isComplete)
        assertFalse(complete.copy(onboardingCompleted = false).isComplete)
        assertFalse(complete.copy(audienceType = null).isComplete)
        assertFalse(complete.copy(zipCode = null).isComplete)
        assertFalse(complete.copy(zipCode = "9000").isComplete)
        assertFalse(complete.copy(zipCode = "900001").isComplete)
        assertFalse(complete.copy(zipCode = "9000A").isComplete)
        assertFalse(complete.copy(zipCode = "٩٠٠٠١").isComplete)
        assertFalse(complete.copy(journeyStage = null).isComplete)
    }

    @Test
    fun `regional center identity preserves deployed fields and derives canonical short name`() {
        val center = RegionalCenter(
            id = 42,
            name = "North Los Angeles County Regional Center"
        )

        assertEquals(
            RegionalCenterIdentity(
                id = 42,
                name = "North Los Angeles County Regional Center",
                shortName = "NLACRC"
            ),
            RegionalCenterIdentity.from(center)
        )
    }

    @Test
    fun `regional center identity derives all canonical short names`() {
        val expected = mapOf(
            "North Los Angeles County Regional Center" to "NLACRC",
            "Westside Regional Center" to "WRC",
            "South Central Los Angeles Regional Center" to "SCLARC",
            "Eastern Los Angeles Regional Center" to "ELARC",
            "Harbor Regional Center" to "HRC",
            "Frank D. Lanterman Regional Center" to "FDLRC",
            "San Gabriel/Pomona Regional Center" to "SGPRC",
            "SG/PRC" to "SGPRC"
        )

        expected.forEach { (name, shortName) ->
            assertEquals(
                shortName,
                RegionalCenterIdentity.from(RegionalCenter(id = 1, name = name)).shortName
            )
        }
    }
}
