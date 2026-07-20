package com.chla.kindd.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chla.kindd.R
import com.chla.kindd.data.discovery.DiscoveryState
import com.chla.kindd.data.models.Provider
import com.chla.kindd.ui.discovery.ActiveFilterChips
import com.chla.kindd.ui.discovery.DiscoveryFilterSheet
import com.chla.kindd.ui.discovery.DiscoverySearchField
import com.chla.kindd.ui.discovery.DiscoveryStateContent
import com.chla.kindd.ui.discovery.DiscoveryUiActions
import com.chla.kindd.ui.theme.CHLABlue
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

data class MapMarkerModel(
    val providerId: String,
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val snippet: String
)

fun providerMarkerModels(providers: List<Provider>): List<MapMarkerModel> = providers.mapNotNull {
    val latitude = it.latitude ?: return@mapNotNull null
    val longitude = it.longitude ?: return@mapNotNull null
    MapMarkerModel(
        providerId = it.id,
        title = it.name,
        latitude = latitude,
        longitude = longitude,
        snippet = it.therapyTypes?.firstOrNull().orEmpty()
    )
}

@Composable
fun MapScreen(
    onProviderClick: (String) -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val locationState by viewModel.locationState.collectAsStateWithLifecycle()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
        viewModel::onLocationPermissionResult
    )
    LaunchedEffect(Unit) { viewModel.onFirstAppearance() }

    MapContent(
        state = state,
        locationState = locationState,
        actions = DiscoveryUiActions(
            onQueryChange = viewModel::setQuery,
            onApplyFilters = { selection ->
                viewModel.applyFilters(
                    selection.therapyTypes,
                    selection.ageGroup,
                    selection.diagnosis,
                    selection.insurance,
                    selection.radiusMiles
                )
            },
            onRemoveTherapy = viewModel::removeTherapy,
            onRemoveAge = viewModel::removeAge,
            onRemoveDiagnosis = viewModel::removeDiagnosis,
            onRemoveInsurance = viewModel::removeInsurance,
            onRemoveRadius = viewModel::removeRadius,
            onClearAll = viewModel::clearAllFilters,
            onRetry = viewModel::retry
        ),
        onUseMyLocation = {
            if (locationState.hasPermission) {
                viewModel.onLocationPermissionResult(granted = true)
            } else {
                permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        },
        onProviderClick = onProviderClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapContent(
    state: DiscoveryState,
    locationState: MapLocationState,
    actions: DiscoveryUiActions,
    onUseMyLocation: () -> Unit,
    onProviderClick: (String) -> Unit,
    markerContent: (@Composable (List<MapMarkerModel>, (String) -> Unit) -> Unit)? = null
) {
    var showFilters by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.map_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CHLABlue,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            DiscoverySearchField(
                query = state.criteria.query,
                onQueryChange = actions.onQueryChange,
                onFilterClick = { showFilters = true },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
            ActiveFilterChips(
                criteria = state.criteria,
                onRemoveTherapy = actions.onRemoveTherapy,
                onRemoveAge = actions.onRemoveAge,
                onRemoveDiagnosis = actions.onRemoveDiagnosis,
                onRemoveInsurance = actions.onRemoveInsurance,
                onRemoveRadius = actions.onRemoveRadius,
                onClearAll = actions.onClearAll,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Button(
                onClick = onUseMyLocation,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(stringResource(R.string.discovery_use_my_location))
            }
            when (locationState.status) {
                MapLocationStatus.LOCATING -> Text(
                    stringResource(R.string.discovery_location_locating),
                    Modifier.padding(horizontal = 16.dp)
                )
                MapLocationStatus.PERMISSION_DENIED -> Text(
                    stringResource(R.string.discovery_location_denied),
                    Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.error
                )
                MapLocationStatus.FAILED -> Text(
                    stringResource(R.string.discovery_location_failed),
                    Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.error
                )
                MapLocationStatus.IDLE -> Unit
            }
            DiscoveryStateContent(
                state = state,
                onRetry = actions.onRetry,
                modifier = Modifier.weight(1f)
            ) {
                val markers = providerMarkerModels(state.mapProviders)
                if (markerContent == null) {
                    ProviderGoogleMap(
                        markers = markers,
                        hasLocationPermission = locationState.hasPermission,
                        onProviderClick = onProviderClick
                    )
                } else {
                    markerContent(markers, onProviderClick)
                }
            }
        }
    }

    if (showFilters) {
        DiscoveryFilterSheet(
            criteria = state.criteria,
            onDismissRequest = { showFilters = false },
            onApply = {
                actions.onApplyFilters(it)
                showFilters = false
            }
        )
    }
}

@Composable
private fun ProviderGoogleMap(
    markers: List<MapMarkerModel>,
    hasLocationPermission: Boolean,
    onProviderClick: (String) -> Unit
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(34.0522, -118.2437), 10f)
    }
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = true,
            myLocationButtonEnabled = hasLocationPermission
        )
    ) {
        markers.forEach { marker ->
            Marker(
                state = MarkerState(LatLng(marker.latitude, marker.longitude)),
                title = marker.title,
                snippet = marker.snippet,
                onClick = {
                    onProviderClick(marker.providerId)
                    true
                }
            )
        }
    }
}
