package com.chla.kindd.ui.map

import androidx.compose.ui.graphics.Color
import com.chla.kindd.data.servicearea.ServiceAreaCoordinate
import com.chla.kindd.data.servicearea.ServiceAreaFeature
import com.chla.kindd.ui.theme.EasternRC
import com.chla.kindd.ui.theme.HarborRC
import com.chla.kindd.ui.theme.KiNDDIndigo
import com.chla.kindd.ui.theme.LantermanRC
import com.chla.kindd.ui.theme.NorthLARC
import com.chla.kindd.ui.theme.SanGabrielRC
import com.chla.kindd.ui.theme.SouthCentralRC
import com.chla.kindd.ui.theme.WestsideRC

/** Stable, tile-independent presentation input for the regional-center map. */
data class RegionalCenterMapRenderModel(
    val camera: RegionalCenterMapCamera,
    val areas: List<RegionalCenterMapAreaRenderModel>
)

data class RegionalCenterMapCamera(
    val latitude: Double,
    val longitude: Double,
    val zoom: Float
)

data class RegionalCenterMapAreaRenderModel(
    val featureId: Int,
    val name: String,
    /** Original service-area acronym, retained for display and click callbacks. */
    val sourceAcronym: String,
    /** Normalized acronym used only for matching and canonical color selection. */
    val canonicalAcronym: String,
    val polygons: List<List<ServiceAreaCoordinate>>,
    val fillColor: Color,
    val strokeColor: Color,
    val strokeWidth: Float,
    val highlighted: Boolean
)

fun buildRegionalCenterMapRenderModel(
    areas: List<ServiceAreaFeature>,
    highlightedAcronym: String?
): RegionalCenterMapRenderModel {
    val canonicalHighlight = highlightedAcronym?.toCanonicalRegionalCenterAcronym()
    val renderedAreas = areas.mapNotNull { area ->
        val polygons = area.polygons.filter(::isValidPolygonRing)
        if (polygons.isEmpty()) return@mapNotNull null

        val canonicalAcronym = area.acronym.toCanonicalRegionalCenterAcronym()
        val highlighted = canonicalAcronym == canonicalHighlight
        val regionColor = canonicalRegionColor(canonicalAcronym)
        RegionalCenterMapAreaRenderModel(
            featureId = area.id,
            name = area.name,
            sourceAcronym = area.acronym,
            canonicalAcronym = canonicalAcronym,
            polygons = polygons,
            fillColor = regionColor.copy(alpha = if (highlighted) 0.34f else 0.15f),
            strokeColor = regionColor,
            strokeWidth = if (highlighted) 3f else 1.5f,
            highlighted = highlighted
        )
    }

    return RegionalCenterMapRenderModel(
        camera = RegionalCenterMapCamera(
            latitude = 33.87,
            longitude = -118.26,
            zoom = 8.75f
        ),
        areas = renderedAreas
    )
}

private fun String.toCanonicalRegionalCenterAcronym(): String =
    trim().uppercase().replace("/", "")

private fun isValidPolygonRing(ring: List<ServiceAreaCoordinate>): Boolean =
    ring.size >= 3 && ring.all { coordinate ->
        coordinate.latitude.isFinite() &&
            coordinate.longitude.isFinite() &&
            coordinate.latitude in -90.0..90.0 &&
            coordinate.longitude in -180.0..180.0
    }

private fun canonicalRegionColor(acronym: String): Color = when (acronym) {
    "WRC" -> WestsideRC
    "HRC" -> HarborRC
    "SCLARC" -> SouthCentralRC
    "ELARC" -> EasternRC
    "NLACRC" -> NorthLARC
    "FDLRC" -> LantermanRC
    "SGPRC" -> SanGabrielRC
    else -> KiNDDIndigo
}
