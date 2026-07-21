package com.chla.kindd.ui.map

import org.junit.Assert.assertEquals
import org.junit.Test

class MapAttributionContractTest {

    @Test
    fun overlayHeightReservesCardAndClearanceForMapAttribution() {
        assertEquals(
            140f,
            mapAttributionBottomPaddingDp(
                overlayHeightPx = 240,
                density = 2f,
                overlaysMap = true
            )
        )
    }

    @Test
    fun absentOrSeparateCardDoesNotAddMapAttributionPadding() {
        assertEquals(
            0f,
            mapAttributionBottomPaddingDp(
                overlayHeightPx = 0,
                density = 2f,
                overlaysMap = true
            )
        )
        assertEquals(
            0f,
            mapAttributionBottomPaddingDp(
                overlayHeightPx = 240,
                density = 2f,
                overlaysMap = false
            )
        )
    }
}
