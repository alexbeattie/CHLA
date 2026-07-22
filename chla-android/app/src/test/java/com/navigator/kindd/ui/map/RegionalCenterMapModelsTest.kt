package com.navigator.kindd.ui.map

import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.navigator.kindd.data.servicearea.ServiceAreaCoordinate
import com.navigator.kindd.data.servicearea.ServiceAreaFeature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class RegionalCenterMapModelsTest {

    @Test
    fun renderModel_usesExactApprovedRegionalCenterPalette() {
        val colors = buildRegionalCenterMapRenderModel(sevenAreas(), null).areas
            .associate { area -> area.canonicalAcronym to area.strokeColor.copy(alpha = 1f).toArgb() }

        assertEquals(
            mapOf(
                "NLACRC" to 0xFFD9A621.toInt(),
                "FDLRC" to 0xFF9966B3.toInt(),
                "HRC" to 0xFF3399DB.toInt(),
                "SCLARC" to 0xFFF28C33.toInt(),
                "ELARC" to 0xFF4CBF73.toInt(),
                "WRC" to 0xFFE64D80.toInt(),
                "SGPRC" to 0xFF338C59.toInt()
            ),
            colors
        )
    }

    @Test
    fun renderModel_encodesAlphaAndStrokeWidthInDpSemantics() {
        val model = buildRegionalCenterMapRenderModel(
            areas = listOf(area("WRC"), area("HRC")),
            highlightedAcronym = "HRC"
        )

        val normal = model.areas.first { it.canonicalAcronym == "WRC" }
        val highlighted = model.areas.first { it.canonicalAcronym == "HRC" }

        assertEquals(0x26E64D80, normal.fillColor.toArgb())
        assertEquals(0xB3E64D80.toInt(), normal.strokeColor.toArgb())
        assertEquals(1.5.dp, normal.strokeWidth)
        assertEquals(0x573399DB, highlighted.fillColor.toArgb())
        assertEquals(0xFF3399DB.toInt(), highlighted.strokeColor.toArgb())
        assertEquals(3.dp, highlighted.strokeWidth)
    }

    @Test
    fun interactionContract_disablesEveryGoogleMapControlAndGestureForStaticMaps() {
        val contract = buildRegionalCenterMapInteractionContract(interactive = false)

        assertEquals(false, contract.compassEnabled)
        assertEquals(false, contract.indoorLevelPickerEnabled)
        assertEquals(false, contract.mapToolbarEnabled)
        assertEquals(false, contract.myLocationButtonEnabled)
        assertEquals(false, contract.rotationGesturesEnabled)
        assertEquals(false, contract.scrollGesturesEnabled)
        assertEquals(false, contract.scrollGesturesEnabledDuringRotateOrZoom)
        assertEquals(false, contract.tiltGesturesEnabled)
        assertEquals(false, contract.zoomControlsEnabled)
        assertEquals(false, contract.zoomGesturesEnabled)
    }

    @Test
    fun interactionContract_enablesOnlyGesturesForInteractiveMaps() {
        val contract = buildRegionalCenterMapInteractionContract(interactive = true)

        assertEquals(false, contract.compassEnabled)
        assertEquals(false, contract.indoorLevelPickerEnabled)
        assertEquals(false, contract.mapToolbarEnabled)
        assertEquals(false, contract.myLocationButtonEnabled)
        assertEquals(true, contract.rotationGesturesEnabled)
        assertEquals(true, contract.scrollGesturesEnabled)
        assertEquals(true, contract.scrollGesturesEnabledDuringRotateOrZoom)
        assertEquals(true, contract.tiltGesturesEnabled)
        assertEquals(false, contract.zoomControlsEnabled)
        assertEquals(true, contract.zoomGesturesEnabled)
    }

    @Test
    fun polygonContracts_mapEveryRingAndPreserveSourceIdentityAndInteraction() {
        val slashArea = area("SG/PRC").copy(
            polygons = listOf(ring(0.0), ring(1.0))
        )
        val model = buildRegionalCenterMapRenderModel(
            areas = listOf(slashArea),
            highlightedAcronym = "sgprc"
        )

        val contracts = buildRegionalCenterMapPolygonContracts(model, interactive = false)

        assertEquals(2, contracts.size)
        assertEquals(listOf("SG/PRC", "SG/PRC"), contracts.map { it.sourceAcronym })
        assertEquals(listOf("SGPRC", "SGPRC"), contracts.map { it.canonicalAcronym })
        assertEquals(listOf(ring(0.0), ring(1.0)), contracts.map { it.coordinates })
        assertEquals(listOf(false, false), contracts.map { it.clickable })
        assertEquals(listOf(3.dp, 3.dp), contracts.map { it.strokeWidth })
    }

    @Test
    fun strokeWidthConversion_usesDisplayDensityAtGoogleMapsBoundary() {
        val normal = buildRegionalCenterMapPolygonContracts(
            renderModel = buildRegionalCenterMapRenderModel(
                areas = listOf(area("WRC")),
                highlightedAcronym = null
            ),
            interactive = false
        ).single()

        assertEquals(4.5f, normal.strokeWidthPx(Density(density = 3f)), 0.001f)
    }

    @Test
    fun googleMapGeometryCache_reusesLatLngObjectsUntilGeometryChanges() {
        val firstGeometry = buildRegionalCenterMapGeometry(
            listOf(area("SG/PRC").copy(polygons = listOf(ring(0.0), ring(1.0))))
        )
        val cache = RegionalCenterGoogleMapGeometryCache()

        val first = cache.get(firstGeometry)
        val second = cache.get(firstGeometry)
        val replacement = cache.get(buildRegionalCenterMapGeometry(listOf(area("WRC"))))

        assertSame(first, second)
        assertSame(first.first().points.first(), second.first().points.first())
        assertNotSame(first, replacement)
    }

    private fun sevenAreas(): List<ServiceAreaFeature> =
        listOf("NLACRC", "FDLRC", "HRC", "SCLARC", "ELARC", "WRC", "SG/PRC")
            .mapIndexed { index, acronym -> area(acronym, index) }

    private fun area(acronym: String, id: Int = 1) = ServiceAreaFeature(
        id = id,
        name = "$acronym Regional Center",
        acronym = acronym,
        description = "$acronym service area",
        polygons = listOf(ring(0.0))
    )

    private fun ring(offset: Double) = listOf(
        ServiceAreaCoordinate(33.0 + offset / 100, -118.0),
        ServiceAreaCoordinate(33.1 + offset / 100, -118.1),
        ServiceAreaCoordinate(33.2 + offset / 100, -118.0)
    )
}
