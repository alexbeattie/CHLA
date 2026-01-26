package com.chla.kindd.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chla.kindd.data.models.Provider
import com.chla.kindd.data.repository.ProviderRepository
import com.chla.kindd.services.LocationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "MapViewModel"

data class MapUiState(
    val providers: List<Provider> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val hasLocationPermission: Boolean = false,
    val userLatitude: Double? = null,
    val userLongitude: Double? = null,
    val searchQuery: String = ""
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val providerRepository: ProviderRepository,
    private val locationService: LocationService
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        checkLocationPermission()
        loadProviders()
    }

    private fun checkLocationPermission() {
        _uiState.update { it.copy(hasLocationPermission = locationService.hasLocationPermission()) }
    }

    fun loadProviders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = if (_uiState.value.userLatitude != null && _uiState.value.userLongitude != null) {
                providerRepository.getProvidersNearby(
                    latitude = _uiState.value.userLatitude!!,
                    longitude = _uiState.value.userLongitude!!
                )
            } else {
                providerRepository.getProviders()
            }

            result.fold(
                onSuccess = { providers ->
                    Log.d(TAG, "Loaded ${providers.size} providers")
                    val withCoords = providers.filter { it.hasCoordinates }
                    Log.d(TAG, "Providers with coordinates: ${withCoords.size}")
                    // Log first 3 providers for debugging
                    providers.take(3).forEach { p ->
                        Log.d(TAG, "Provider: ${p.name}, lat=${p.latitude}, lng=${p.longitude}, hasCoords=${p.hasCoordinates}")
                    }
                    _uiState.update { it.copy(providers = providers, isLoading = false) }
                },
                onFailure = { error ->
                    Log.e(TAG, "Failed to load providers: ${error.message}", error)
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                }
            )
        }
    }

    fun search(query: String) {
        if (query.length < 2) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, searchQuery = query) }

            providerRepository.searchProviders(query).fold(
                onSuccess = { providers ->
                    _uiState.update { it.copy(providers = providers, isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                }
            )
        }
    }

    fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "") }
        loadProviders()
    }

    fun updateLocation(latitude: Double, longitude: Double) {
        _uiState.update { it.copy(userLatitude = latitude, userLongitude = longitude) }
        loadProviders()
    }

    fun requestLocationUpdate() {
        viewModelScope.launch {
            locationService.getCurrentLocation()?.let { location ->
                updateLocation(location.latitude, location.longitude)
            }
        }
    }
}
