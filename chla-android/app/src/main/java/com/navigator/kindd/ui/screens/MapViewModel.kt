package com.navigator.kindd.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.navigator.kindd.data.discovery.DiscoveryController
import com.navigator.kindd.data.discovery.DiscoveryState
import com.navigator.kindd.data.discovery.TherapyType
import com.navigator.kindd.data.models.Provider
import com.navigator.kindd.data.profile.AgeGroup
import com.navigator.kindd.data.source.UserLocationSource
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MapLocationStatus {
    IDLE,
    LOCATING,
    PERMISSION_DENIED,
    FAILED
}

data class MapLocationState(
    val hasPermission: Boolean = false,
    val status: MapLocationStatus = MapLocationStatus.IDLE
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val discoveryController: DiscoveryController,
    private val userLocationSource: UserLocationSource
) : ViewModel() {

    val state: StateFlow<DiscoveryState> = discoveryController.state

    private val mutableLocationState = MutableStateFlow(
        MapLocationState(hasPermission = userLocationSource.hasLocationPermission())
    )
    val locationState: StateFlow<MapLocationState> = mutableLocationState.asStateFlow()

    val mapProviders: List<Provider>
        get() = state.value.mapProviders

    private val locationGeneration = AtomicLong(0)
    private var locationJob: Job? = null

    fun onFirstAppearance() = discoveryController.ensureLoaded()

    fun setQuery(query: String) = discoveryController.setQuery(query)

    fun applyFilters(
        therapyTypes: Set<TherapyType>,
        ageGroup: AgeGroup?,
        diagnosis: String?,
        insurance: String?,
        radiusMiles: Int
    ) = discoveryController.applyFilters(
        therapyTypes = therapyTypes,
        ageGroup = ageGroup,
        diagnosis = diagnosis,
        insurance = insurance,
        radiusMiles = radiusMiles
    )

    fun removeTherapy(therapyType: TherapyType) = updateFilters {
        copy(therapyTypes = therapyTypes - therapyType)
    }

    fun removeAge() = updateFilters { copy(ageGroup = null) }

    fun removeDiagnosis() = updateFilters { copy(diagnosis = null) }

    fun removeInsurance() = updateFilters { copy(insurance = null) }

    fun removeRadius() = discoveryController.useLosAngelesCatalog()

    fun clearAllFilters() = discoveryController.clearAllFilters()

    fun retry() = discoveryController.retry()

    fun refresh() = discoveryController.refresh()

    fun refreshLocationPermission() {
        val hasPermission = userLocationSource.hasLocationPermission()
        if (hasPermission) {
            mutableLocationState.update { current ->
                current.copy(
                    hasPermission = true,
                    status = if (current.status == MapLocationStatus.PERMISSION_DENIED) {
                        MapLocationStatus.IDLE
                    } else {
                        current.status
                    }
                )
            }
        } else if (mutableLocationState.value.hasPermission) {
            locationGeneration.incrementAndGet()
            locationJob?.cancel()
            locationJob = null
            mutableLocationState.value = MapLocationState(
                hasPermission = false,
                status = MapLocationStatus.PERMISSION_DENIED
            )
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        val requestGeneration = locationGeneration.incrementAndGet()
        locationJob?.cancel()
        locationJob = null

        if (!granted) {
            mutableLocationState.value = MapLocationState(
                hasPermission = false,
                status = MapLocationStatus.PERMISSION_DENIED
            )
            return
        }

        mutableLocationState.value = MapLocationState(
            hasPermission = true,
            status = MapLocationStatus.LOCATING
        )
        locationJob = viewModelScope.launch {
            try {
                val coordinates = userLocationSource.currentCoordinates()
                if (coordinates == null) {
                    updateLocationIfCurrent(requestGeneration) {
                        it.copy(status = MapLocationStatus.FAILED)
                    }
                    return@launch
                }
                if (locationGeneration.get() != requestGeneration) return@launch
                discoveryController.useDeviceLocation(
                    latitude = coordinates.latitude,
                    longitude = coordinates.longitude
                )
                updateLocationIfCurrent(requestGeneration) {
                    it.copy(status = MapLocationStatus.IDLE)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                updateLocationIfCurrent(requestGeneration) {
                    it.copy(status = MapLocationStatus.FAILED)
                }
            }
        }
    }

    private inline fun updateLocationIfCurrent(
        requestGeneration: Long,
        transform: (MapLocationState) -> MapLocationState
    ) {
        if (locationGeneration.get() == requestGeneration) {
            mutableLocationState.update(transform)
        }
    }

    private inline fun updateFilters(
        transform: com.navigator.kindd.data.discovery.DiscoveryCriteria.() ->
            com.navigator.kindd.data.discovery.DiscoveryCriteria
    ) {
        val criteria = state.value.criteria.transform()
        applyFilters(
            therapyTypes = criteria.therapyTypes,
            ageGroup = criteria.ageGroup,
            diagnosis = criteria.diagnosis,
            insurance = criteria.insurance,
            radiusMiles = criteria.radiusMiles
        )
    }
}
