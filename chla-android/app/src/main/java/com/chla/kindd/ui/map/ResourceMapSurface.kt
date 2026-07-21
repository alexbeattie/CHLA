package com.chla.kindd.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.chla.kindd.data.discovery.DiscoveryOrigin
import com.chla.kindd.ui.screens.MapMarkerModel
import com.chla.kindd.ui.screens.ProviderMarkerRole
import com.chla.kindd.ui.theme.KiNDDIndigo
import com.chla.kindd.ui.theme.KiNDDPink
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

private const val LOS_ANGELES_LATITUDE = 34.0522
private const val LOS_ANGELES_LONGITUDE = -118.2437
private const val LOS_ANGELES_ZOOM = 10f
private const val DEVICE_LOCATION_ZOOM = 12f

internal fun kinddMapUiSettings(): MapUiSettings = MapUiSettings(
    compassEnabled = false,
    indoorLevelPickerEnabled = false,
    mapToolbarEnabled = false,
    myLocationButtonEnabled = false,
    rotationGesturesEnabled = true,
    scrollGesturesEnabled = true,
    scrollGesturesEnabledDuringRotateOrZoom = true,
    tiltGesturesEnabled = true,
    zoomControlsEnabled = false,
    zoomGesturesEnabled = true
)

@Composable
@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
internal fun ProviderResourceMap(
    markers: List<MapMarkerModel>,
    origin: DiscoveryOrigin,
    hasLocationPermission: Boolean,
    onProviderClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(LOS_ANGELES_LATITUDE, LOS_ANGELES_LONGITUDE),
            LOS_ANGELES_ZOOM
        )
    }
    var selectedProviderId by rememberSaveable { mutableStateOf<String?>(null) }
    var centeredDeviceOriginKey by rememberSaveable { mutableStateOf<String?>(null) }
    val deviceOrigin = origin as? DiscoveryOrigin.DeviceLocation
    val deviceOriginKey = deviceOrigin?.let { "${it.latitude},${it.longitude}" }

    LaunchedEffect(deviceOriginKey) {
        if (deviceOrigin != null && centeredDeviceOriginKey != deviceOriginKey) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(deviceOrigin.latitude, deviceOrigin.longitude),
                    DEVICE_LOCATION_ZOOM
                )
            )
            centeredDeviceOriginKey = deviceOriginKey
        }
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
        uiSettings = kinddMapUiSettings()
    ) {
        markers.forEach { marker ->
            val markerState = remember(marker.providerId, marker.latitude, marker.longitude) {
                MarkerState(LatLng(marker.latitude, marker.longitude))
            }
            val selected = selectedProviderId == marker.providerId
            MarkerComposable(
                keys = arrayOf(marker.providerId, marker.role, selected),
                state = markerState,
                anchor = Offset(0.5f, 1f),
                title = marker.title,
                snippet = marker.snippet,
                zIndex = if (selected) 2f else 1f,
                onClick = {
                    selectedProviderId = marker.providerId
                    onProviderClick(marker.providerId)
                    true
                }
            ) {
                KiNDDProviderPin(role = marker.role, selected = selected)
            }
        }
    }
}

internal fun providerMarkerColor(role: ProviderMarkerRole): Color = when (role) {
    ProviderMarkerRole.ABA,
    ProviderMarkerRole.OTHER -> Color(red = 0.24f, green = 0.47f, blue = 0.85f)
    ProviderMarkerRole.SPEECH -> Color(red = 0.55f, green = 0.35f, blue = 0.85f)
    ProviderMarkerRole.OCCUPATIONAL -> Color(red = 0.25f, green = 0.75f, blue = 0.45f)
    ProviderMarkerRole.PHYSICAL -> Color(red = 0.95f, green = 0.60f, blue = 0.20f)
}

@Composable
private fun KiNDDProviderPin(role: ProviderMarkerRole, selected: Boolean) {
    val width = if (selected) 42.dp else 36.dp
    val height = if (selected) 50.dp else 44.dp
    val pinColor = providerMarkerColor(role)
    Canvas(Modifier.size(width = width, height = height)) {
        val radius = size.width * 0.36f
        val center = Offset(size.width / 2f, radius + size.width * 0.08f)
        val pointTop = center.y + radius * 0.55f
        val point = Path().apply {
            moveTo(center.x - radius * 0.62f, pointTop)
            lineTo(center.x, size.height)
            lineTo(center.x + radius * 0.62f, pointTop)
            close()
        }
        drawPath(point, pinColor)
        drawCircle(pinColor, radius = radius, center = center)
        if (selected) {
            drawCircle(
                color = KiNDDPink,
                radius = radius + size.width * 0.055f,
                center = center,
                style = Stroke(width = size.width * 0.055f)
            )
        }
        drawCircle(
            color = Color.White,
            radius = radius * 0.46f,
            center = center
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.75f),
            radius = radius * 0.13f,
            center = Offset(center.x - radius * 0.38f, center.y - radius * 0.38f)
        )
    }
}
