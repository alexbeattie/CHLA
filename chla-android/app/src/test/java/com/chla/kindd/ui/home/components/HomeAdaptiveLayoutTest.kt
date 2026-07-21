package com.chla.kindd.ui.home.components

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeAdaptiveLayoutTest {

    @Test
    fun matchedCenterActionsStackAcrossTheReviewedNarrowWidthRange() {
        assertTrue(shouldStackMatchedCenterActions(availableWidthDp = 320, fontScale = 1f))
        assertTrue(shouldStackMatchedCenterActions(availableWidthDp = 340, fontScale = 1f))
        assertFalse(shouldStackMatchedCenterActions(availableWidthDp = 360, fontScale = 1f))
    }

    @Test
    fun matchedCenterActionsStackAtLargeTextEvenOnWiderScreens() {
        assertTrue(shouldStackMatchedCenterActions(availableWidthDp = 400, fontScale = 1.3f))
    }

    @Test
    fun headerUsesCompactTreatmentForNarrowOrLargeTextLayouts() {
        assertTrue(shouldUseCompactHomeHeader(availableWidthDp = 320, fontScale = 1f))
        assertTrue(shouldUseCompactHomeHeader(availableWidthDp = 340, fontScale = 1.3f))
        assertFalse(shouldUseCompactHomeHeader(availableWidthDp = 360, fontScale = 1f))
    }

    @Test
    fun mapHeroExpandsOnlyForNarrowLargeTextLayouts() {
        assertEquals(420, homeMapHeroHeightDp(availableWidthDp = 320, fontScale = 1.3f))
        assertEquals(420, homeMapHeroHeightDp(availableWidthDp = 340, fontScale = 1.3f))
        assertEquals(340, homeMapHeroHeightDp(availableWidthDp = 341, fontScale = 1.3f))
        assertEquals(340, homeMapHeroHeightDp(availableWidthDp = 320, fontScale = 1f))
    }
}
