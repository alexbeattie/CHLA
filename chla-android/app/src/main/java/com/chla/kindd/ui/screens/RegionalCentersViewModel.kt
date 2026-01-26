package com.chla.kindd.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chla.kindd.data.models.RegionalCenter
import com.chla.kindd.data.repository.RegionalCenterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RegionalCentersUiState(
    val centers: List<RegionalCenter> = emptyList(),
    val matchedCenter: RegionalCenter? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RegionalCentersViewModel @Inject constructor(
    private val repository: RegionalCenterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegionalCentersUiState())
    val uiState: StateFlow<RegionalCentersUiState> = _uiState.asStateFlow()

    init {
        loadCenters()
    }

    private fun loadCenters() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            repository.getRegionalCenters().fold(
                onSuccess = { centers ->
                    _uiState.update { it.copy(centers = centers, isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                }
            )
        }
    }

    fun findByZip(zipCode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, matchedCenter = null) }

            repository.getRegionalCenterByZip(zipCode).fold(
                onSuccess = { center ->
                    _uiState.update { it.copy(matchedCenter = center, isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                }
            )
        }
    }
}
