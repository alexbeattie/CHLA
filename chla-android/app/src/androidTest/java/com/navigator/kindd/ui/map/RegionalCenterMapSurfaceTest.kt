package com.navigator.kindd.ui.map

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.navigator.kindd.data.servicearea.ServiceAreaCoordinate
import com.navigator.kindd.data.servicearea.ServiceAreaFeature
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RegionalCenterMapSurfaceTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun renderModel_mapsAllSevenRegionalCenterFeatures() {
        val model = buildRegionalCenterMapRenderModel(
            areas = sevenAreas(),
            highlightedAcronym = null
        )

        assertEquals(7, model.areas.size)
        assertEquals(
            listOf("NLACRC", "FDLRC", "HRC", "SCLARC", "ELARC", "WRC", "SGPRC"),
            model.areas.map { it.canonicalAcronym }
        )
        assertEquals("SG/PRC", model.areas.last().sourceAcronym)
    }

    @Test
    fun renderModel_exposesLosAngelesCameraFraming() {
        val camera = buildRegionalCenterMapRenderModel(emptyList(), null).camera

        assertEquals(33.87, camera.latitude, 0.01)
        assertEquals(-118.26, camera.longitude, 0.01)
        assertTrue(camera.zoom in 8f..10f)
    }

    @Test
    fun renderModel_retainsEveryValidMultiPolygonOuterRing() {
        val feature = area("WRC", polygons = listOf(ring(0.0), ring(1.0), ring(2.0)))

        val rendered = buildRegionalCenterMapRenderModel(listOf(feature), null).areas.single()

        assertEquals(3, rendered.polygons.size)
        assertEquals(feature.polygons, rendered.polygons)
    }

    @Test
    fun renderModel_mapsCanonicalRegionColorsByAcronym() {
        val colors = buildRegionalCenterMapRenderModel(sevenAreas(), null).areas
            .associate { it.canonicalAcronym to it.strokeColor.copy(alpha = 1f).toArgb() }

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
    fun renderModel_normalizesSanGabrielAcronymForHighlightWithoutChangingSourceIdentity() {
        val slashSource = buildRegionalCenterMapRenderModel(
            areas = listOf(area("SG/PRC")),
            highlightedAcronym = "sgprc"
        ).areas.single()

        val compactSource = buildRegionalCenterMapRenderModel(
            areas = listOf(area("SGPRC")),
            highlightedAcronym = "SG/PRC"
        ).areas.single()

        assertTrue(slashSource.highlighted)
        assertEquals("SG/PRC", slashSource.sourceAcronym)
        assertEquals("SGPRC", slashSource.canonicalAcronym)
        assertTrue(compactSource.highlighted)
        assertEquals("SGPRC", compactSource.sourceAcronym)
    }

    @Test
    fun renderModel_appliesNormalAndHighlightedAlphaAndStroke() {
        val model = buildRegionalCenterMapRenderModel(
            areas = listOf(area("WRC"), area("HRC")),
            highlightedAcronym = "HRC"
        )

        val normal = model.areas.first { it.canonicalAcronym == "WRC" }
        val highlighted = model.areas.first { it.canonicalAcronym == "HRC" }
        assertEquals(0x26E64D80, normal.fillColor.toArgb())
        assertEquals(0xB3E64D80.toInt(), normal.strokeColor.toArgb())
        assertEquals(1.5.dp, normal.strokeWidth)
        assertFalse(normal.highlighted)
        assertEquals(0x573399DB, highlighted.fillColor.toArgb())
        assertEquals(0xFF3399DB.toInt(), highlighted.strokeColor.toArgb())
        assertEquals(3.dp, highlighted.strokeWidth)
        assertTrue(highlighted.highlighted)
    }

    @Test
    fun googleMapGeometry_preservesPolygonOrderCoordinatesAndCacheIdentity() {
        val geometry = buildRegionalCenterMapGeometry(
            listOf(area("SG/PRC", polygons = listOf(ring(0.0), ring(1.0))))
        )
        val cache = RegionalCenterGoogleMapGeometryCache()

        val first = cache.get(geometry)
        val second = cache.get(geometry)
        val replacement = cache.get(
            buildRegionalCenterMapGeometry(listOf(area("WRC")))
        )

        assertEquals(2, first.size)
        assertEquals(listOf(0, 1), first.map { it.ringIndex })
        assertEquals(ring(0.0).map { it.latitude }, first.first().points.map { it.latitude })
        assertEquals(ring(0.0).map { it.longitude }, first.first().points.map { it.longitude })
        assertSame(first, second)
        assertNotSame(first, replacement)
    }

    @Test
    fun renderModel_omitsShortAndInvalidRingsAndAreasWithoutValidPolygons() {
        val valid = ring(0.0)
        val short = listOf(point(0.0, 0.0), point(0.1, 0.1))
        val nonFinite = listOf(point(0.0, 0.0), point(Double.NaN, 0.1), point(0.2, 0.2))
        val outOfBounds = listOf(point(0.0, 0.0), point(91.0, 0.1), point(0.2, 0.2))

        val model = buildRegionalCenterMapRenderModel(
            areas = listOf(
                area("WRC", polygons = listOf(short, nonFinite, valid, outOfBounds)),
                area("HRC", polygons = listOf(short))
            ),
            highlightedAcronym = null
        )

        assertEquals(1, model.areas.size)
        assertEquals(listOf(valid), model.areas.single().polygons)
    }

    @Test
    fun surface_exposesRootTagAndPassesRenderModelToInjectedContent() {
        var receivedModel: RegionalCenterMapRenderModel? = null

        composeRule.setContent {
            RegionalCenterMapSurface(
                areas = listOf(area("WRC")),
                highlightedAcronym = "WRC",
                interactive = true,
                onAreaClick = {},
                mapContent = { model, _ ->
                    SideEffect { receivedModel = model }
                }
            )
        }

        composeRule.onNodeWithTag("regional_center_map_surface").assertExists()
        composeRule.runOnIdle {
            assertEquals("WRC", receivedModel?.areas?.single()?.sourceAcronym)
            assertTrue(receivedModel?.areas?.single()?.highlighted == true)
        }
    }

    @Test
    fun surface_interactiveInjectedContentCallsBackWithSourceAcronym() {
        val clicks = mutableListOf<String>()

        composeRule.setContent {
            RegionalCenterMapSurface(
                areas = listOf(area("SG/PRC")),
                highlightedAcronym = null,
                interactive = true,
                onAreaClick = clicks::add,
                mapContent = { model, onRenderedAreaClick ->
                    Text(
                        text = model.areas.single().sourceAcronym,
                        modifier = Modifier
                            .testTag("injected_area")
                            .clickable { onRenderedAreaClick(model.areas.single().sourceAcronym) }
                    )
                }
            )
        }

        composeRule.onNodeWithTag("injected_area").performClick()
        composeRule.runOnIdle { assertEquals(listOf("SG/PRC"), clicks) }
    }

    @Test
    fun surface_nonInteractiveInjectedContentSuppressesCallback() {
        val clicks = mutableListOf<String>()

        composeRule.setContent {
            RegionalCenterMapSurface(
                areas = listOf(area("WRC")),
                highlightedAcronym = null,
                interactive = false,
                onAreaClick = clicks::add,
                mapContent = { model, onRenderedAreaClick ->
                    Text(
                        text = model.areas.single().sourceAcronym,
                        modifier = Modifier
                            .testTag("injected_area")
                            .clickable { onRenderedAreaClick(model.areas.single().sourceAcronym) }
                    )
                }
            )
        }

        composeRule.onNodeWithTag("injected_area").performClick()
        composeRule.runOnIdle { assertTrue(clicks.isEmpty()) }
    }

    private fun sevenAreas(): List<ServiceAreaFeature> =
        listOf("NLACRC", "FDLRC", "HRC", "SCLARC", "ELARC", "WRC", "SG/PRC")
            .mapIndexed { index, acronym -> area(acronym, index) }

    private fun area(
        acronym: String,
        id: Int = 1,
        polygons: List<List<ServiceAreaCoordinate>> = listOf(ring(id.toDouble()))
    ) = ServiceAreaFeature(
        id = id,
        name = "$acronym Regional Center",
        acronym = acronym,
        description = "$acronym service area",
        polygons = polygons
    )

    private fun ring(offset: Double): List<ServiceAreaCoordinate> = listOf(
        point(33.0 + offset / 100, -118.0),
        point(33.1 + offset / 100, -118.1),
        point(33.2 + offset / 100, -118.0)
    )

    private fun point(latitude: Double, longitude: Double) =
        ServiceAreaCoordinate(latitude = latitude, longitude = longitude)
}
