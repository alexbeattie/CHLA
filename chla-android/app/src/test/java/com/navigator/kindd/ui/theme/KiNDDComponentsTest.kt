package com.navigator.kindd.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class KiNDDComponentsTest {

    @Test
    fun pressVisual_contractScalesControlsAndCards_andFadesWhenMotionIsDisabled() {
        assertEquals(
            KiNDDPressVisual(scale = 0.97f, alpha = 1f),
            kinddPressVisual(pressed = true, spatialMotionEnabled = true, card = false)
        )
        assertEquals(
            KiNDDPressVisual(scale = 0.985f, alpha = 1f),
            kinddPressVisual(pressed = true, spatialMotionEnabled = true, card = true)
        )
        assertEquals(
            KiNDDPressVisual(scale = 1f, alpha = 0.66f),
            kinddPressVisual(pressed = true, spatialMotionEnabled = false, card = false)
        )
        assertEquals(
            KiNDDPressVisual(scale = 1f, alpha = 1f),
            kinddPressVisual(pressed = false, spatialMotionEnabled = true, card = false)
        )
    }
}
