package com.navigator.kindd.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import com.navigator.kindd.data.servicearea.ServiceAreaFeature
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun RegionalCenterMapSurface(
    areas: List<ServiceAreaFeature>,
    highlightedAcronym: String?,
    interactive: Boolean,
    onAreaClick: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
    mapContent: (@Composable (RegionalCenterMapRenderModel, (String) -> Unit) -> Unit)? = null
) {
    val geometry = remember(areas) {
        buildRegionalCenterMapGeometry(areas)
    }
    val renderModel = remember(geometry, highlightedAcronym) {
        buildRegionalCenterMapRenderModel(geometry, highlightedAcronym)
    }
    val guardedAreaClick: (String) -> Unit = if (interactive) onAreaClick else ({ _ -> })

    Box(
        modifier = modifier.testTag("regional_center_map_surface")
    ) {
        if (mapContent != null) {
            mapContent(renderModel, guardedAreaClick)
        } else {
            RegionalCenterGoogleMap(
                renderModel = renderModel,
                geometry = geometry,
                interactive = interactive,
                onAreaClick = guardedAreaClick,
                contentPadding = contentPadding
            )
        }
    }
}

@Composable
private fun RegionalCenterGoogleMap(
    renderModel: RegionalCenterMapRenderModel,
    geometry: RegionalCenterMapGeometry,
    interactive: Boolean,
    onAreaClick: (String) -> Unit,
    contentPadding: PaddingValues
) {
    val density = LocalDensity.current
    val camera = renderModel.camera
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(camera.latitude, camera.longitude),
            camera.zoom
        )
    }
    val interaction = remember(interactive) {
        buildRegionalCenterMapInteractionContract(interactive)
    }
    val uiSettings = remember(interaction) {
        MapUiSettings(
            compassEnabled = interaction.compassEnabled,
            indoorLevelPickerEnabled = interaction.indoorLevelPickerEnabled,
            mapToolbarEnabled = interaction.mapToolbarEnabled,
            myLocationButtonEnabled = interaction.myLocationButtonEnabled,
            rotationGesturesEnabled = interaction.rotationGesturesEnabled,
            scrollGesturesEnabled = interaction.scrollGesturesEnabled,
            scrollGesturesEnabledDuringRotateOrZoom =
                interaction.scrollGesturesEnabledDuringRotateOrZoom,
            tiltGesturesEnabled = interaction.tiltGesturesEnabled,
            zoomControlsEnabled = interaction.zoomControlsEnabled,
            zoomGesturesEnabled = interaction.zoomGesturesEnabled
        )
    }
    val polygonContracts = remember(renderModel, interactive) {
        buildRegionalCenterMapPolygonContracts(renderModel, interactive)
    }
    val googleMapGeometryCache = remember { RegionalCenterGoogleMapGeometryCache() }
    // Highlight changes keep the same geometry instance, so the cache reuses every LatLng.
    val googleMapGeometry = googleMapGeometryCache.get(geometry)

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        uiSettings = uiSettings,
        contentPadding = contentPadding
    ) {
        polygonContracts.zip(googleMapGeometry).forEach { (polygon, polygonGeometry) ->
            Polygon(
                points = polygonGeometry.points,
                clickable = polygon.clickable,
                fillColor = polygon.fillColor,
                strokeColor = polygon.strokeColor,
                strokeWidth = polygon.strokeWidthPx(density),
                tag = polygon.sourceAcronym,
                onClick = { onAreaClick(polygon.sourceAcronym) }
            )
        }
    }
}

internal data class RegionalCenterGoogleMapPolygonGeometry(
    val featureId: Int,
    val ringIndex: Int,
    val points: List<LatLng>
)

internal class RegionalCenterGoogleMapGeometryCache {
    private var sourceGeometry: RegionalCenterMapGeometry? = null
    private var cachedPolygons: List<RegionalCenterGoogleMapPolygonGeometry> = emptyList()

    fun get(geometry: RegionalCenterMapGeometry): List<RegionalCenterGoogleMapPolygonGeometry> {
        if (sourceGeometry !== geometry) {
            sourceGeometry = geometry
            cachedPolygons = buildRegionalCenterGoogleMapPolygonGeometry(geometry)
        }
        return cachedPolygons
    }
}

internal fun buildRegionalCenterGoogleMapPolygonGeometry(
    geometry: RegionalCenterMapGeometry
): List<RegionalCenterGoogleMapPolygonGeometry> = geometry.areas.flatMap { area ->
    area.polygons.mapIndexed { ringIndex, ring ->
        RegionalCenterGoogleMapPolygonGeometry(
            featureId = area.featureId,
            ringIndex = ringIndex,
            points = ring.map { coordinate ->
                LatLng(coordinate.latitude, coordinate.longitude)
            }
        )
    }
}
