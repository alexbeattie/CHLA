package com.chla.kindd.ui.home.components

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeMapRenderContractTest {

    @Test
    fun baseMapIsMountedIndependentlyOfServiceAreaOverlayReadiness() {
        val source = File(
            "src/main/java/com/chla/kindd/ui/home/components/HomeMapHero.kt"
        ).readText()

        assertTrue(source.contains("RegionalCenterMapSurface("))
        assertTrue(source.contains("areas = uiState.serviceAreas"))
        assertFalse(source.contains("if (uiState.serviceAreas.isNotEmpty())"))
    }
}
