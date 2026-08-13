package com.navigator.kindd.data.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProviderAddressFormatterTest {
    @Test
    fun `decodes a quoted JSON address scalar`() {
        val address = ProviderAddressFormatter.format(
            rawAddress = "\"123 Main St\"",
            city = "Los Angeles",
            state = "CA",
            zipCode = "90001"
        )

        assertEquals("123 Main St", address.streetLine)
        assertEquals("Los Angeles, CA 90001", address.localityLine)
        assertEquals("123 Main St, Los Angeles, CA 90001", address.singleLine)
    }

    @Test
    fun `decodes a nested quoted JSON address scalar`() {
        val address = ProviderAddressFormatter.format(
            rawAddress = "\"\\\"123 Main St\\\"\"",
            city = "Los Angeles",
            state = "CA",
            zipCode = "90001"
        )

        assertEquals("123 Main St", address.streetLine)
        assertEquals("Los Angeles, CA 90001", address.localityLine)
    }

    @Test
    fun `suppresses malformed quoted JSON and keeps structured locality`() {
        val address = ProviderAddressFormatter.format(
            rawAddress = "\"123 Main St",
            city = "Los Angeles",
            state = "CA",
            zipCode = "90001"
        )

        assertNull(address.streetLine)
        assertEquals("Los Angeles, CA 90001", address.localityLine)
        assertEquals("Los Angeles, CA 90001", address.singleLine)
    }

    @Test
    fun `suppresses an unsafe quoted JSON-like scalar`() {
        val address = ProviderAddressFormatter.format(
            rawAddress = "\"{broken\"",
            city = "Glendale",
            state = "CA",
            zipCode = "91203"
        )

        assertNull(address.streetLine)
        assertEquals("Glendale, CA 91203", address.localityLine)
    }

    @Test
    fun `suppresses JSON null and keeps structured locality`() {
        val address = ProviderAddressFormatter.format(
            rawAddress = "null",
            city = "Burbank",
            state = "CA",
            zipCode = "91502"
        )

        assertNull(address.streetLine)
        assertEquals("Burbank, CA 91502", address.localityLine)
        assertEquals("Burbank, CA 91502", address.singleLine)
    }

    @Test
    fun `formats a plain street address with structured locality`() {
        val address = ProviderAddressFormatter.format(
            rawAddress = "  123 Main St  ",
            city = " Los Angeles ",
            state = " CA ",
            zipCode = " 90001 "
        )

        assertEquals("123 Main St", address.streetLine)
        assertEquals("Los Angeles, CA 90001", address.localityLine)
        assertEquals(listOf("123 Main St", "Los Angeles, CA 90001"), address.lines)
        assertEquals("123 Main St, Los Angeles, CA 90001", address.singleLine)
    }

    @Test
    fun `formats a JSON object address`() {
        val address = ProviderAddressFormatter.format(
            rawAddress = """
                {
                  "street_address": "456 Oak Ave",
                  "city": "Pasadena",
                  "state": "CA",
                  "zip_code": "91101"
                }
            """.trimIndent(),
            city = null,
            state = null,
            zipCode = null
        )

        assertEquals("456 Oak Ave", address.streetLine)
        assertEquals("Pasadena, CA 91101", address.localityLine)
        assertEquals("456 Oak Ave, Pasadena, CA 91101", address.singleLine)
    }

    @Test
    fun `formats safe JSON array fragments in order`() {
        val address = ProviderAddressFormatter.format(
            rawAddress = """["789 Pine Rd", {"city":"Burbank","state":"CA","zip":"91502"}, "{broken", "", 42]""",
            city = null,
            state = null,
            zipCode = null
        )

        assertEquals("789 Pine Rd", address.streetLine)
        assertEquals("Burbank, CA 91502", address.localityLine)
        assertEquals(listOf("789 Pine Rd", "Burbank, CA 91502"), address.lines)
    }

    @Test
    fun `suppresses malformed JSON-like input and keeps structured locality`() {
        val address = ProviderAddressFormatter.format(
            rawAddress = """{"street":"broken"""",
            city = "Glendale",
            state = "CA",
            zipCode = "91203"
        )

        assertNull(address.streetLine)
        assertEquals("Glendale, CA 91203", address.localityLine)
        assertEquals("Glendale, CA 91203", address.singleLine)
    }

    @Test
    fun `does not duplicate locality already present in a plain address`() {
        val address = ProviderAddressFormatter.format(
            rawAddress = "123 Main St, Los Angeles, CA 90001",
            city = "los angeles",
            state = "ca",
            zipCode = "90001"
        )

        assertEquals("123 Main St, Los Angeles, CA 90001", address.streetLine)
        assertNull(address.localityLine)
        assertEquals("123 Main St, Los Angeles, CA 90001", address.singleLine)
    }

    @Test
    fun `trims blank components and omits empty lines`() {
        val address = ProviderAddressFormatter.format(
            rawAddress = "  ",
            city = " ",
            state = " CA ",
            zipCode = null
        )

        assertNull(address.streetLine)
        assertEquals("CA", address.localityLine)
        assertEquals(listOf("CA"), address.lines)
        assertEquals("CA", address.singleLine)
    }
}
