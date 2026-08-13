package com.navigator.kindd.ui.map

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.navigator.kindd.data.servicearea.ServiceAreaCoordinate
import com.navigator.kindd.data.servicearea.ServiceAreaFeature
import com.navigator.kindd.ui.theme.EasternRC
import com.navigator.kindd.ui.theme.HarborRC
import com.navigator.kindd.ui.theme.KiNDDIndigo
import com.navigator.kindd.ui.theme.LantermanRC
import com.navigator.kindd.ui.theme.NorthLARC
import com.navigator.kindd.ui.theme.SanGabrielRC
import com.navigator.kindd.ui.theme.SouthCentralRC
import com.navigator.kindd.ui.theme.WestsideRC

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
    /** Logical stroke width; convert to physical pixels only at the Google Maps boundary. */
    val strokeWidth: Dp,
    val highlighted: Boolean
)

internal data class RegionalCenterMapGeometry(
    val areas: List<RegionalCenterMapGeometryArea>
)

internal data class RegionalCenterMapGeometryArea(
    val featureId: Int,
    val name: String,
    val sourceAcronym: String,
    val canonicalAcronym: String,
    val polygons: List<List<ServiceAreaCoordinate>>
)

internal data class RegionalCenterMapInteractionContract(
    val compassEnabled: Boolean,
    val indoorLevelPickerEnabled: Boolean,
    val mapToolbarEnabled: Boolean,
    val myLocationButtonEnabled: Boolean,
    val rotationGesturesEnabled: Boolean,
    val scrollGesturesEnabled: Boolean,
    val scrollGesturesEnabledDuringRotateOrZoom: Boolean,
    val tiltGesturesEnabled: Boolean,
    val zoomControlsEnabled: Boolean,
    val zoomGesturesEnabled: Boolean
)

internal data class RegionalCenterMapPolygonContract(
    val featureId: Int,
    val sourceAcronym: String,
    val canonicalAcronym: String,
    val coordinates: List<ServiceAreaCoordinate>,
    val fillColor: Color,
    val strokeColor: Color,
    val strokeWidth: Dp,
    val clickable: Boolean
)

fun buildRegionalCenterMapRenderModel(
    areas: List<ServiceAreaFeature>,
    highlightedAcronym: String?
): RegionalCenterMapRenderModel = buildRegionalCenterMapRenderModel(
    geometry = buildRegionalCenterMapGeometry(areas),
    highlightedAcronym = highlightedAcronym
)

internal fun buildRegionalCenterMapGeometry(
    areas: List<ServiceAreaFeature>
): RegionalCenterMapGeometry = RegionalCenterMapGeometry(
    areas = areas.mapNotNull { area ->
        val polygons = area.polygons.filter(::isValidPolygonRing)
        if (polygons.isEmpty()) return@mapNotNull null

        RegionalCenterMapGeometryArea(
            featureId = area.id,
            name = area.name,
            sourceAcronym = area.acronym,
            canonicalAcronym = area.acronym.toCanonicalRegionalCenterAcronym(),
            polygons = polygons
        )
    }
)

internal fun buildRegionalCenterMapRenderModel(
    geometry: RegionalCenterMapGeometry,
    highlightedAcronym: String?
): RegionalCenterMapRenderModel {
    val canonicalHighlight = highlightedAcronym?.toCanonicalRegionalCenterAcronym()
    val renderedAreas = geometry.areas.map { area ->
        val highlighted = area.canonicalAcronym == canonicalHighlight
        val regionColor = canonicalRegionColor(area.canonicalAcronym)
        RegionalCenterMapAreaRenderModel(
            featureId = area.featureId,
            name = area.name,
            sourceAcronym = area.sourceAcronym,
            canonicalAcronym = area.canonicalAcronym,
            polygons = area.polygons,
            fillColor = regionColor.copy(alpha = if (highlighted) 0.34f else 0.15f),
            strokeColor = regionColor.copy(alpha = if (highlighted) 1f else 0.70f),
            strokeWidth = if (highlighted) 3.dp else 1.5.dp,
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

internal fun buildRegionalCenterMapInteractionContract(
    interactive: Boolean
) = RegionalCenterMapInteractionContract(
    compassEnabled = false,
    indoorLevelPickerEnabled = false,
    mapToolbarEnabled = false,
    myLocationButtonEnabled = false,
    rotationGesturesEnabled = interactive,
    scrollGesturesEnabled = interactive,
    scrollGesturesEnabledDuringRotateOrZoom = interactive,
    tiltGesturesEnabled = interactive,
    zoomControlsEnabled = false,
    zoomGesturesEnabled = interactive
)

internal fun buildRegionalCenterMapPolygonContracts(
    renderModel: RegionalCenterMapRenderModel,
    interactive: Boolean
): List<RegionalCenterMapPolygonContract> = renderModel.areas.flatMap { area ->
    area.polygons.map { ring ->
        RegionalCenterMapPolygonContract(
            featureId = area.featureId,
            sourceAcronym = area.sourceAcronym,
            canonicalAcronym = area.canonicalAcronym,
            coordinates = ring,
            fillColor = area.fillColor,
            strokeColor = area.strokeColor,
            strokeWidth = area.strokeWidth,
            clickable = interactive
        )
    }
}

internal fun RegionalCenterMapPolygonContract.strokeWidthPx(density: Density): Float =
    with(density) { strokeWidth.toPx() }

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
