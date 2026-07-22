package com.navigator.kindd.data.discovery

import com.navigator.kindd.data.models.Provider
import com.navigator.kindd.data.profile.AgeGroup
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryRequestPlannerTest {
    private val planner = DiscoveryRequestPlanner()

    @Test
    fun `ProfileZip always plans a regional center request`() {
        val planned = planner.plan(
            DiscoveryCriteria(origin = DiscoveryOrigin.ProfileZip("90001"))
        )

        assertTrue(planned is PlannedDiscoveryRequest.ProfileZip)
    }

    @Test
    fun `ProfileZip sends normalized ZIP age diagnosis and insurance but no therapy`() {
        val planned = planner.plan(
            DiscoveryCriteria(
                query = "  family   support  ",
                therapyTypes = setOf(TherapyType.ABA, TherapyType.SPEECH),
                ageGroup = AgeGroup.ALL_AGES,
                diagnosis = "  Autism   Spectrum Disorder ",
                insurance = "  Blue   Cross ",
                origin = DiscoveryOrigin.ProfileZip(" 90001 ")
            )
        ) as PlannedDiscoveryRequest.ProfileZip

        assertEquals("90001", planned.remote.zipCode)
        assertEquals("All Ages", planned.remote.ageGroup)
        assertEquals("Autism Spectrum Disorder", planned.remote.diagnosis)
        assertEquals("Blue Cross", planned.remote.insurance)
        assertEquals("family support", planned.query)
        assertEquals(setOf(TherapyType.ABA, TherapyType.SPEECH), planned.therapies)
        assertTrue(
            planned.remote.javaClass.declaredFields.none { field ->
                field.name.contains("therap", ignoreCase = true)
            }
        )
    }

    @Test
    fun `ProfileZip preserves absent age separately from All Ages`() {
        val absent = planner.plan(
            DiscoveryCriteria(origin = DiscoveryOrigin.ProfileZip("90001"))
        ) as PlannedDiscoveryRequest.ProfileZip
        val allAges = planner.plan(
            DiscoveryCriteria(
                ageGroup = AgeGroup.ALL_AGES,
                origin = DiscoveryOrigin.ProfileZip("90001")
            )
        ) as PlannedDiscoveryRequest.ProfileZip

        assertNull(absent.remote.ageGroup)
        assertEquals("All Ages", allAges.remote.ageGroup)
    }

    @Test
    fun `ProfileZip local therapy filtering requires every selected therapy`() {
        val allSelected = provider(
            id = "all",
            therapyTypes = listOf(" ABA   THERAPY ", "speech therapy")
        )
        val abaOnly = provider(id = "aba", therapyTypes = listOf("ABA therapy"))
        val speechOnly = provider(id = "speech", therapyTypes = listOf("Speech therapy"))
        val planned = planner.plan(
            DiscoveryCriteria(
                therapyTypes = linkedSetOf(TherapyType.SPEECH, TherapyType.ABA),
                origin = DiscoveryOrigin.ProfileZip("90001")
            )
        ) as PlannedDiscoveryRequest.ProfileZip

        val filtered = planner.applyLocalFilters(
            providers = listOf(allSelected, abaOnly, speechOnly),
            request = planned
        )

        assertEquals(listOf("all"), filtered.map(Provider::id))
    }

    @Test
    fun `ProfileZip local query is case insensitive across every searchable field`() {
        val provider = provider(
            id = "searchable",
            name = "Bright Path Clinic",
            address = "123 North   Main Street",
            city = "Pasadena",
            state = "CA",
            zipCode = "91101",
            description = "Caregiver Coaching",
            therapyTypes = listOf("Speech   Therapy"),
            insuranceCarriers = listOf(" Blue   Cross ")
        )
        val unrelated = provider(id = "unrelated")

        listOf(
            "BRIGHT path",
            "north main STREET",
            "pAsAdEnA",
            "CAREGIVER coaching",
            "speech therapy",
            "blue cross"
        ).forEach { query ->
            val request = planner.plan(
                DiscoveryCriteria(
                    query = query,
                    origin = DiscoveryOrigin.ProfileZip("90001")
                )
            ) as PlannedDiscoveryRequest.ProfileZip

            assertEquals(
                "Expected query to match: $query",
                listOf("searchable"),
                planner.applyLocalFilters(listOf(provider, unrelated), request).map(Provider::id)
            )
        }
    }

    @Test
    fun `DeviceLocation sends paired coordinates radius and every normalized filter`() {
        val planned = planner.plan(
            DiscoveryCriteria(
                query = "  speech   support ",
                therapyTypes = linkedSetOf(TherapyType.SPEECH, TherapyType.ABA),
                ageGroup = AgeGroup.SCHOOL_AGE,
                diagnosis = " Autism   Spectrum Disorder ",
                insurance = " Medi-Cal ",
                radiusMiles = 25,
                origin = DiscoveryOrigin.DeviceLocation(34.0522, -118.2437)
            )
        ) as PlannedDiscoveryRequest.Comprehensive

        assertEquals("speech support", planned.remote.query)
        assertEquals(34.0522, planned.remote.latitude!!, 0.0)
        assertEquals(-118.2437, planned.remote.longitude!!, 0.0)
        assertEquals(25, planned.remote.radiusMiles)
        assertEquals(
            listOf("ABA therapy", "Speech therapy"),
            planned.remote.therapyTypes
        )
        assertEquals("6-12", planned.remote.ageGroup)
        assertEquals("Autism Spectrum Disorder", planned.remote.diagnosis)
        assertEquals("Medi-Cal", planned.remote.insurance)
    }

    @Test
    fun `Los Angeles with a nonblank query plans comprehensive search`() {
        val planned = planner.plan(DiscoveryCriteria(query = "  ABA  "))

        assertTrue(planned is PlannedDiscoveryRequest.Comprehensive)
        assertEquals(
            "ABA",
            (planned as PlannedDiscoveryRequest.Comprehensive).remote.query
        )
        assertNull(planned.remote.latitude)
        assertNull(planned.remote.longitude)
    }

    @Test
    fun `Los Angeles with any non-radius filter plans comprehensive search`() {
        val filteredCriteria = listOf(
            DiscoveryCriteria(therapyTypes = setOf(TherapyType.FEEDING)),
            DiscoveryCriteria(ageGroup = AgeGroup.EARLY_INTERVENTION),
            DiscoveryCriteria(diagnosis = "Other"),
            DiscoveryCriteria(insurance = "Private Pay")
        )

        filteredCriteria.forEach { criteria ->
            assertTrue(
                "Expected comprehensive request for $criteria",
                planner.plan(criteria) is PlannedDiscoveryRequest.Comprehensive
            )
        }
    }

    @Test
    fun `Los Angeles defaults plan catalog and radius alone is not an active filter`() {
        assertEquals(PlannedDiscoveryRequest.Catalog, planner.plan(DiscoveryCriteria()))
        assertEquals(
            PlannedDiscoveryRequest.Catalog,
            planner.plan(DiscoveryCriteria(radiusMiles = 50))
        )
    }

    @Test
    fun `whitespace-only optional text normalizes to null`() {
        val planned = planner.plan(
            DiscoveryCriteria(
                query = " \t\n ",
                diagnosis = "   ",
                insurance = "\t",
                origin = DiscoveryOrigin.DeviceLocation(34.0, -118.0)
            )
        ) as PlannedDiscoveryRequest.Comprehensive

        assertNull(planned.remote.query)
        assertNull(planned.remote.diagnosis)
        assertNull(planned.remote.insurance)
    }

    @Test
    fun `request keys are deterministic for Sets inserted in different order`() {
        val first = planner.plan(
            DiscoveryCriteria(
                therapyTypes = linkedSetOf(TherapyType.SPEECH, TherapyType.ABA),
                origin = DiscoveryOrigin.DeviceLocation(34.0, -118.0)
            )
        )
        val second = planner.plan(
            DiscoveryCriteria(
                therapyTypes = linkedSetOf(TherapyType.ABA, TherapyType.SPEECH),
                origin = DiscoveryOrigin.DeviceLocation(34.0, -118.0)
            )
        )

        assertEquals(planner.requestKey(first), planner.requestKey(second))
    }

    private fun provider(
        id: String,
        name: String = "Provider $id",
        address: String? = null,
        city: String? = null,
        state: String? = null,
        zipCode: String? = null,
        description: String? = null,
        therapyTypes: List<String>? = null,
        insuranceCarriers: List<String>? = null
    ): Provider {
        if (insuranceCarriers == null) {
            return Provider(
                id = id,
                name = name,
                address = address,
                city = city,
                state = state,
                zipCode = zipCode,
                description = description,
                therapyTypes = therapyTypes
            )
        }

        return Gson().fromJson(
            Gson().toJson(
                mapOf(
                    "id" to id,
                    "name" to name,
                    "address" to address,
                    "city" to city,
                    "state" to state,
                    "zip_code" to zipCode,
                    "description" to description,
                    "therapy_types" to therapyTypes,
                    "insurance_carriers" to insuranceCarriers
                )
            ),
            Provider::class.java
        )
    }
}
