package com.chla.kindd.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chla.kindd.data.models.RegionalCenter
import com.chla.kindd.data.profile.RegionalCenterIdentity
import com.chla.kindd.data.profile.UserProfile
import com.chla.kindd.data.profile.UserProfileRepository
import com.chla.kindd.data.source.RegionalCenterDataSource
import com.chla.kindd.data.source.RegionalCenterLookup
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RegionalCentersLookupState {
    IDLE,
    LOADING,
    MATCHED,
    UNMATCHED,
    UNAVAILABLE
}

enum class RegionalCentersMessage {
    INVALID_ZIP,
    NO_MATCH,
    LOOKUP_UNAVAILABLE,
    CATALOG_UNAVAILABLE
}

data class RegionalCentersUiState(
    val centers: List<RegionalCenter> = emptyList(),
    val matchedCenter: RegionalCenter? = null,
    val zipDraft: String = "",
    val isLoading: Boolean = false,
    val lookupState: RegionalCentersLookupState = RegionalCentersLookupState.IDLE,
    val message: RegionalCentersMessage? = null
)

@HiltViewModel
class RegionalCentersViewModel @Inject constructor(
    private val profileRepository: UserProfileRepository,
    private val regionalCenterDataSource: RegionalCenterDataSource
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(RegionalCentersUiState())
    val uiState: StateFlow<RegionalCentersUiState> = mutableUiState.asStateFlow()
    private var currentProfile = UserProfile()

    init {
        viewModelScope.launch {
            profileRepository.profile.collect { profile ->
                currentProfile = profile
                mutableUiState.update { state ->
                    state.copy(zipDraft = profile.zipCode.orEmpty())
                }
            }
        }
        loadCenters()
    }

    fun onZipChanged(value: String) {
        val zip = value.filter { character -> character in '0'..'9' }.take(5)
        mutableUiState.update {
            it.copy(
                zipDraft = zip,
                matchedCenter = null,
                lookupState = RegionalCentersLookupState.IDLE,
                message = null
            )
        }
    }

    fun submitZip() {
        val zipCode = uiState.value.zipDraft
        if (!zipCode.matches(Regex("[0-9]{5}"))) {
            mutableUiState.update { it.copy(message = RegionalCentersMessage.INVALID_ZIP) }
            return
        }
        viewModelScope.launch {
            mutableUiState.update {
                it.copy(
                    lookupState = RegionalCentersLookupState.LOADING,
                    matchedCenter = null,
                    message = null
                )
            }
            when (val lookup = regionalCenterDataSource.lookupRegionalCenter(zipCode)) {
                is RegionalCenterLookup.Matched -> {
                    profileRepository.replaceProfile(
                        currentProfile.copy(
                            zipCode = zipCode,
                            regionalCenter = RegionalCenterIdentity.from(lookup.center)
                        )
                    )
                    mutableUiState.update {
                        it.copy(
                            matchedCenter = lookup.center,
                            lookupState = RegionalCentersLookupState.MATCHED
                        )
                    }
                }
                RegionalCenterLookup.Unmatched -> mutableUiState.update {
                    it.copy(
                        lookupState = RegionalCentersLookupState.UNMATCHED,
                        message = RegionalCentersMessage.NO_MATCH
                    )
                }
                is RegionalCenterLookup.Unavailable -> mutableUiState.update {
                    it.copy(
                        lookupState = RegionalCentersLookupState.UNAVAILABLE,
                        message = RegionalCentersMessage.LOOKUP_UNAVAILABLE
                    )
                }
            }
        }
    }

    private fun loadCenters() {
        viewModelScope.launch {
            mutableUiState.update { it.copy(isLoading = true) }
            try {
                regionalCenterDataSource.getRegionalCenters().fold(
                    onSuccess = { centers ->
                        mutableUiState.update { it.copy(centers = centers, isLoading = false) }
                    },
                    onFailure = {
                        mutableUiState.update {
                            it.copy(
                                isLoading = false,
                                message = RegionalCentersMessage.CATALOG_UNAVAILABLE
                            )
                        }
                    }
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableUiState.update {
                    it.copy(
                        isLoading = false,
                        message = RegionalCentersMessage.CATALOG_UNAVAILABLE
                    )
                }
            }
        }
    }
}
