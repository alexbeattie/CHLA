package com.chla.kindd.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chla.kindd.data.discovery.DiscoveryState
import com.chla.kindd.data.models.Provider
import com.chla.kindd.ui.discovery.DiscoveryFilterSheet
import com.chla.kindd.ui.discovery.DiscoveryUiActions
import com.chla.kindd.ui.map.ProviderResourceMap
import com.chla.kindd.ui.map.ResourceMapContextBadges
import com.chla.kindd.ui.map.ResourceMapControlRail
import com.chla.kindd.ui.map.ResourceMapRetainedContextOverlays
import com.chla.kindd.ui.map.ResourceMapSearchChrome
import com.chla.kindd.ui.map.activeMapFilterCount

data class MapMarkerModel(
    val providerId: String,
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val snippet: String,
    val role: ProviderMarkerRole
)

enum class ProviderMarkerRole {
    ABA,
    SPEECH,
    OCCUPATIONAL,
    PHYSICAL,
    OTHER
}

internal fun providerMarkerRole(provider: Provider): ProviderMarkerRole =
    (provider.therapyTypes.orEmpty() + listOfNotNull(provider.type))
        .asSequence()
        .map(String::lowercase)
        .map { value ->
            when {
                "aba" in value -> ProviderMarkerRole.ABA
                "speech" in value -> ProviderMarkerRole.SPEECH
                "occupational" in value -> ProviderMarkerRole.OCCUPATIONAL
                "physical" in value -> ProviderMarkerRole.PHYSICAL
                else -> ProviderMarkerRole.OTHER
            }
        }
        .firstOrNull { it != ProviderMarkerRole.OTHER }
        ?: ProviderMarkerRole.OTHER

internal fun canEnableMapMyLocation(
    locationState: MapLocationState,
    hasPlatformPermission: Boolean
): Boolean = locationState.hasPermission && hasPlatformPermission

internal class LocationPermissionLifecycleBinding(
    private val lifecycle: Lifecycle,
    private val refreshPermission: () -> Unit
) {
    private val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_RESUME) refreshPermission()
    }

    fun start() {
        refreshPermission()
        lifecycle.addObserver(observer)
    }

    fun stop() {
        lifecycle.removeObserver(observer)
    }
}

fun providerMarkerModels(providers: List<Provider>): List<MapMarkerModel> = providers.mapNotNull {
    if (!it.hasValidCoordinates) return@mapNotNull null
    val latitude = it.latitude ?: return@mapNotNull null
    val longitude = it.longitude ?: return@mapNotNull null
    MapMarkerModel(
        providerId = it.id,
        title = it.name,
        latitude = latitude,
        longitude = longitude,
        snippet = it.therapyTypes?.firstOrNull() ?: it.type.orEmpty(),
        role = providerMarkerRole(it)
    )
}

@Composable
fun MapScreen(
    onProviderClick: (String) -> Unit,
    onNavigateToList: () -> Unit,
    viewModel: MapViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val locationState by viewModel.locationState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val safeLocationState = locationState.copy(
        hasPermission = canEnableMapMyLocation(
            locationState = locationState,
            hasPlatformPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    )
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
        viewModel::onLocationPermissionResult
    )
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) { viewModel.onFirstAppearance() }
    DisposableEffect(lifecycleOwner, viewModel) {
        val binding = LocationPermissionLifecycleBinding(
            lifecycle = lifecycleOwner.lifecycle,
            refreshPermission = viewModel::refreshLocationPermission
        )
        binding.start()
        onDispose(binding::stop)
    }

    MapContent(
        state = state,
        locationState = safeLocationState,
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
            onRetry = viewModel::retry,
            onRefresh = viewModel::refresh
        ),
        onUseMyLocation = {
            if (safeLocationState.hasPermission) {
                viewModel.onLocationPermissionResult(granted = true)
            } else {
                permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        },
        onProviderClick = onProviderClick,
        onNavigateToList = onNavigateToList
    )
}

@Composable
fun MapContent(
    state: DiscoveryState,
    locationState: MapLocationState,
    actions: DiscoveryUiActions,
    onUseMyLocation: () -> Unit,
    onProviderClick: (String) -> Unit,
    onNavigateToList: () -> Unit,
    markerContent: (@Composable (List<MapMarkerModel>, (String) -> Unit) -> Unit)? = null
) {
    var showFilters by remember { mutableStateOf(false) }
    val markers = providerMarkerModels(state.mapProviders)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("map_immersive_root")
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("map_surface")
        ) {
            if (markerContent == null) {
                ProviderResourceMap(
                    markers = markers,
                    origin = state.criteria.origin,
                    hasLocationPermission = locationState.hasPermission,
                    onProviderClick = onProviderClick,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                markerContent(markers, onProviderClick)
            }
        }

        ResourceMapSearchChrome(
            criteria = state.criteria,
            actions = actions,
            onShowFilters = { showFilters = true },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        ResourceMapControlRail(
            activeFilterCount = state.criteria.activeMapFilterCount(),
            locationState = locationState,
            isRefreshing = state.isLoading,
            onShowFilters = { showFilters = true },
            onUseMyLocation = onUseMyLocation,
            onRefresh = actions.onRefresh,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 18.dp)
        )

        ResourceMapRetainedContextOverlays(
            state = state,
            locationState = locationState,
            onRetry = actions.onRetry,
            onShowFilters = { showFilters = true },
            modifier = Modifier.align(Alignment.Center)
        )

        ResourceMapContextBadges(
            state = state,
            onNavigateToList = onNavigateToList,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
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
