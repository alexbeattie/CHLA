package com.chla.kindd.ui.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.chla.kindd.data.servicearea.ServiceAreaFeature
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
    modifier: Modifier = Modifier,
    mapContent: (@Composable (RegionalCenterMapRenderModel, (String) -> Unit) -> Unit)? = null
) {
    val renderModel = remember(areas, highlightedAcronym) {
        buildRegionalCenterMapRenderModel(areas, highlightedAcronym)
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
                interactive = interactive,
                onAreaClick = guardedAreaClick
            )
        }
    }
}

@Composable
private fun RegionalCenterGoogleMap(
    renderModel: RegionalCenterMapRenderModel,
    interactive: Boolean,
    onAreaClick: (String) -> Unit
) {
    val camera = renderModel.camera
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(camera.latitude, camera.longitude),
            camera.zoom
        )
    }
    val uiSettings = remember(interactive) {
        MapUiSettings(
            compassEnabled = false,
            indoorLevelPickerEnabled = false,
            mapToolbarEnabled = false,
            myLocationButtonEnabled = false,
            rotationGesturesEnabled = interactive,
            scrollGesturesEnabled = interactive,
            tiltGesturesEnabled = interactive,
            zoomControlsEnabled = false,
            zoomGesturesEnabled = interactive
        )
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        uiSettings = uiSettings
    ) {
        renderModel.areas.forEach { area ->
            area.polygons.forEach { ring ->
                Polygon(
                    points = ring.map { coordinate ->
                        LatLng(coordinate.latitude, coordinate.longitude)
                    },
                    clickable = interactive,
                    fillColor = area.fillColor,
                    strokeColor = area.strokeColor,
                    strokeWidth = area.strokeWidth,
                    tag = area.sourceAcronym,
                    onClick = { onAreaClick(area.sourceAcronym) }
                )
            }
        }
    }
}
