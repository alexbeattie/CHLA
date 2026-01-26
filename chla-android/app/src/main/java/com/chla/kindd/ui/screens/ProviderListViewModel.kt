package com.chla.kindd.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chla.kindd.data.models.Provider
import com.chla.kindd.data.repository.ProviderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProviderListUiState(
    val providers: List<Provider> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = ""
)

@HiltViewModel
class ProviderListViewModel @Inject constructor(
    private val providerRepository: ProviderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProviderListUiState())
    val uiState: StateFlow<ProviderListUiState> = _uiState.asStateFlow()

    init {
        loadProviders()
    }

    fun loadProviders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            providerRepository.getProviders().fold(
                onSuccess = { providers ->
                    _uiState.update { it.copy(providers = providers, isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                }
            )
        }
    }

    fun search(query: String) {
        if (query.length < 2) {
            if (query.isEmpty()) {
                loadProviders()
            }
            return
        }

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
}
