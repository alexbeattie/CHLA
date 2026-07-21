package com.chla.kindd.data.models

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class ProviderJsonTest {
    private val gson = Gson()

    @Test
    fun `decodes provider type and normalized display address`() {
        val provider = gson.fromJson(
            """
            {
              "id": "00000000-0000-0000-0000-000000000005",
              "name": "Typed Provider",
              "type": "Speech Therapy",
              "address": "100 Hope St",
              "city": "Los Angeles",
              "state": "CA",
              "zip_code": "90001"
            }
            """.trimIndent(),
            Provider::class.java
        )

        assertEquals("Speech Therapy", provider.type)
        assertEquals(listOf("100 Hope St", "Los Angeles, CA 90001"), provider.displayAddressLines)
        assertEquals("100 Hope St, Los Angeles, CA 90001", provider.fullAddress)
    }

    @Test
    fun `uses normalized insurance carriers when legacy insurance is text`() {
        val provider = gson.fromJson(
            """
            {
              "id": "00000000-0000-0000-0000-000000000001",
              "name": "Test Provider",
              "insurance_accepted": "Legacy Plan",
              "insurance_carriers": ["Medi-Cal", "Blue Cross", "Medi-Cal", "BLUE CROSS"]
            }
            """.trimIndent(),
            Provider::class.java
        )

        assertEquals(listOf("Medi-Cal", "Blue Cross"), provider.insuranceAccepted)
    }

    @Test
    fun `falls back to comma separated legacy insurance`() {
        val provider = gson.fromJson(
            """
            {
              "id": "00000000-0000-0000-0000-000000000002",
              "name": "Legacy Provider",
              "insurance_accepted": "Medi-Cal, Blue Cross",
              "insurance_carriers": []
            }
            """.trimIndent(),
            Provider::class.java
        )

        assertEquals(listOf("Medi-Cal", "Blue Cross"), provider.insuranceAccepted)
    }

    @Test
    fun `preserves legacy insurance arrays`() {
        val provider = gson.fromJson(
            """
            {
              "id": "00000000-0000-0000-0000-000000000004",
              "name": "Older API Provider",
              "insurance_accepted": ["Medi-Cal", "Blue Cross"]
            }
            """.trimIndent(),
            Provider::class.java
        )

        assertEquals(listOf("Medi-Cal", "Blue Cross"), provider.insuranceAccepted)
    }
}
