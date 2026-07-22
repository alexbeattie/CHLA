package com.navigator.kindd.ui.map

import androidx.compose.ui.graphics.Color
import com.navigator.kindd.ui.screens.ProviderMarkerRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResourceMapSurfaceTest {

    @Test
    fun `KiNDD map disables stock Google chrome while preserving direct gestures`() {
        val settings = kinddMapUiSettings()

        assertFalse(settings.compassEnabled)
        assertFalse(settings.indoorLevelPickerEnabled)
        assertFalse(settings.mapToolbarEnabled)
        assertFalse(settings.myLocationButtonEnabled)
        assertFalse(settings.zoomControlsEnabled)
        assertTrue(settings.rotationGesturesEnabled)
        assertTrue(settings.scrollGesturesEnabled)
        assertTrue(settings.tiltGesturesEnabled)
        assertTrue(settings.zoomGesturesEnabled)
    }

    @Test
    fun `provider marker roles use the canonical iPhone role colors`() {
        assertEquals(Color(red = 0.24f, green = 0.47f, blue = 0.85f), providerMarkerColor(ProviderMarkerRole.ABA))
        assertEquals(Color(red = 0.55f, green = 0.35f, blue = 0.85f), providerMarkerColor(ProviderMarkerRole.SPEECH))
        assertEquals(Color(red = 0.25f, green = 0.75f, blue = 0.45f), providerMarkerColor(ProviderMarkerRole.OCCUPATIONAL))
        assertEquals(Color(red = 0.95f, green = 0.60f, blue = 0.20f), providerMarkerColor(ProviderMarkerRole.PHYSICAL))
        assertEquals(Color(red = 0.24f, green = 0.47f, blue = 0.85f), providerMarkerColor(ProviderMarkerRole.OTHER))
    }
}
