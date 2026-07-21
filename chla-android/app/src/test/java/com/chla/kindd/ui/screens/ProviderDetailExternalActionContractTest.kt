package com.chla.kindd.ui.screens

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderDetailExternalActionContractTest {

    @Test
    fun `provider actions use guarded external intent helpers`() {
        val source = File(
            "src/main/java/com/chla/kindd/ui/screens/ProviderDetailScreen.kt"
        ).readText()

        assertFalse(
            "Provider details must not launch external activities directly",
            source.contains("startActivity(")
        )
        assertEquals(
            "Both phone actions must use the guarded dialer helper",
            2,
            Regex("context\\.launchDialer\\(provider\\.phone\\)").findAll(source).count()
        )
        assertTrue(
            "Directions must use a resolvable HTTPS destination",
            source.contains("https://www.google.com/maps/dir/?api=1&destination=")
        )
        assertTrue(
            "Provider websites must use the guarded website helper",
            source.contains("context.launchWebsite(provider.website)")
        )
    }
}
