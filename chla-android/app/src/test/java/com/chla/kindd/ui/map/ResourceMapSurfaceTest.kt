package com.chla.kindd.ui.map

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
}
