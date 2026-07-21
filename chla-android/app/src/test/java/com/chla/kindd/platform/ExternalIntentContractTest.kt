package com.chla.kindd.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExternalIntentContractTest {

    @Test
    fun `dial request keeps only phone digits`() {
        assertEquals(
            ExternalIntentRequest(
                action = ExternalIntentAction.DIAL,
                uri = "tel:18185551212"
            ),
            dialRequest("+1 (818) 555-1212")
        )
    }

    @Test
    fun `dial request rejects values without phone digits`() {
        assertNull(dialRequest("not available"))
    }

    @Test
    fun `website request defaults a bare host to https`() {
        assertEquals(
            ExternalIntentRequest(
                action = ExternalIntentAction.VIEW,
                uri = "https://kinddhelp.com/resources"
            ),
            websiteRequest("kinddhelp.com/resources")
        )
    }

    @Test
    fun `website request accepts only valid http and https destinations`() {
        assertEquals(
            "http://kinddhelp.com/help?q=regional",
            websiteRequest("http://kinddhelp.com/help?q=regional")?.uri
        )
        assertNull(websiteRequest("javascript:alert(1)"))
        assertNull(websiteRequest("ftp://kinddhelp.com/file"))
        assertNull(websiteRequest("https://"))
        assertNull(websiteRequest("https://kinddhelp.com/bad path"))
        assertNull(websiteRequest("   "))
    }

    @Test
    fun `external request is not launched when no activity can resolve it`() {
        var launched = false

        val result = launchExternalIntent(
            request = websiteRequest("kinddhelp.com"),
            canResolve = { false },
            launch = { launched = true }
        )

        assertFalse(result)
        assertFalse(launched)
    }

    @Test
    fun `external request returns false when resolution or launch throws`() {
        val request = websiteRequest("kinddhelp.com")

        assertFalse(
            launchExternalIntent(
                request = request,
                canResolve = { error("resolver unavailable") },
                launch = {}
            )
        )
        assertFalse(
            launchExternalIntent(
                request = request,
                canResolve = { true },
                launch = { error("activity disappeared") }
            )
        )
    }

    @Test
    fun `resolvable external request is launched once`() {
        var launches = 0

        val result = launchExternalIntent(
            request = dialRequest("818-555-1212"),
            canResolve = { true },
            launch = { launches += 1 }
        )

        assertTrue(result)
        assertEquals(1, launches)
    }
}
