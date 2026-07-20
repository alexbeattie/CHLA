package com.chla.kindd.data.models

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
}
