package com.chla.kindd.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chla.kindd.data.discovery.DiscoveryController
import com.chla.kindd.data.discovery.DiscoveryState
import com.chla.kindd.data.discovery.TherapyType
import com.chla.kindd.data.models.Provider
import com.chla.kindd.data.profile.AgeGroup
import com.chla.kindd.data.source.UserLocationSource
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.CancellationException
import javax.inject.Inject
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

    fun onLocationPermissionResult(granted: Boolean) {
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
        viewModelScope.launch {
            try {
                val coordinates = userLocationSource.currentCoordinates()
                if (coordinates == null) {
                    mutableLocationState.update { it.copy(status = MapLocationStatus.FAILED) }
                    return@launch
                }
                discoveryController.useDeviceLocation(
                    latitude = coordinates.latitude,
                    longitude = coordinates.longitude
                )
                mutableLocationState.update { it.copy(status = MapLocationStatus.IDLE) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                mutableLocationState.update { it.copy(status = MapLocationStatus.FAILED) }
            }
        }
    }

    private inline fun updateFilters(
        transform: com.chla.kindd.data.discovery.DiscoveryCriteria.() ->
            com.chla.kindd.data.discovery.DiscoveryCriteria
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
