package com.navigator.kindd.data.models

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class RegionalCenterJsonTest {
    private val gson = Gson()

    @Test
    fun `accepts JSON encoded ZIP code lists`() {
        val center = gson.fromJson(
            """
            {
              "id": 33,
              "regional_center": "Example Regional Center",
              "zip_codes": "[\"90001\", \"90002\"]"
            }
            """.trimIndent(),
            RegionalCenter::class.java
        )

        assertEquals(listOf("90001", "90002"), center.zipCodes)
    }

    @Test
    fun `preserves array ZIP code lists`() {
        val center = gson.fromJson(
            """
            {
              "id": 64,
              "regional_center": "South Central Los Angeles Regional Center",
              "zip_codes": ["90001", "90002"]
            }
            """.trimIndent(),
            RegionalCenter::class.java
        )

        assertEquals(listOf("90001", "90002"), center.zipCodes)
    }

    @Test
    fun `derives every canonical Los Angeles regional center short name`() {
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
            assertEquals(shortName, RegionalCenter(id = 1, name = name).shortName)
        }
    }
}
