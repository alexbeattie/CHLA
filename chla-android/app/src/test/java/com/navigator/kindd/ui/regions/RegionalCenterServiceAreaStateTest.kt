package com.navigator.kindd.ui.regions

import com.navigator.kindd.data.servicearea.ServiceAreaCoordinate
import com.navigator.kindd.data.servicearea.ServiceAreaFeature
import com.navigator.kindd.ui.onboarding.shouldSeparateMatchedCenterCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class RegionalCenterServiceAreaStateTest {

    @Test
    fun successfulNonEmptyLoad_becomesSuccess() {
        val areas = listOf(serviceArea())

        val state = Result.success(areas).toRegionalCenterServiceAreaState()

        assertEquals(RegionalCenterServiceAreaState.Success(areas), state)
    }

    @Test
    fun successfulEmptyLoad_becomesErrorInsteadOfAnEmptyMap() {
        assertSame(
            RegionalCenterServiceAreaState.Error,
            Result.success(emptyList<ServiceAreaFeature>()).toRegionalCenterServiceAreaState()
        )
    }

    @Test
    fun failedLoad_becomesErrorInsteadOfAnEmptyMap() {
        assertSame(
            RegionalCenterServiceAreaState.Error,
            Result.failure<List<ServiceAreaFeature>>(IllegalStateException("missing"))
                .toRegionalCenterServiceAreaState()
        )
    }

    @Test
    fun matchedHeroSeparatesMapAndCardAtAccessibilityFontScale() {
        assertEquals(false, shouldSeparateMatchedCenterCard(fontScale = 1.29f))
        assertEquals(true, shouldSeparateMatchedCenterCard(fontScale = 1.3f))
        assertEquals(true, shouldSeparateMatchedCenterCard(fontScale = 2f))
    }

    private fun serviceArea() = ServiceAreaFeature(
        id = 1,
        name = "Westside Regional Center",
        acronym = "WRC",
        description = "",
        polygons = listOf(
            listOf(
                ServiceAreaCoordinate(34.0, -118.4),
                ServiceAreaCoordinate(34.1, -118.3),
                ServiceAreaCoordinate(33.9, -118.2)
            )
        )
    )
}
