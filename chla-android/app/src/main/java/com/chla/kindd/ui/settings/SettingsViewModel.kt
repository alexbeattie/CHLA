package com.chla.kindd.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chla.kindd.data.discovery.DiscoveryController
import com.chla.kindd.data.discovery.DiscoveryState
import com.chla.kindd.data.profile.AudienceType
import com.chla.kindd.data.profile.UserProfile
import com.chla.kindd.data.profile.UserProfileRepository
import com.chla.kindd.ui.discovery.DiscoveryFilterSelection
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SettingsEvent {
    data object NavigateToEditProfile : SettingsEvent
    data object ClearFailed : SettingsEvent
    data object PreferenceUpdateFailed : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profileRepository: UserProfileRepository,
    private val discoveryController: DiscoveryController
) : ViewModel() {

    private val eventChannel = Channel<SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsEvent> = eventChannel.receiveAsFlow()
    val profile: StateFlow<UserProfile> = profileRepository.profile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UserProfile()
    )
    val discoveryState: StateFlow<DiscoveryState> = discoveryController.state
    private var clearJob: Job? = null
    private var appModeJob: Job? = null

    fun editProfile() {
        eventChannel.trySend(SettingsEvent.NavigateToEditProfile)
    }

    fun updateAppMode(audienceType: AudienceType) {
        if (
            appModeJob?.isActive == true ||
            !profile.value.isComplete ||
            profile.value.audienceType == audienceType
        ) return
        appModeJob = viewModelScope.launch {
            val expected = profile.value
            try {
                val replaced = profileRepository.replaceProfileIfCurrent(
                    expected = expected,
                    replacement = expected.copy(audienceType = audienceType)
                )
                if (!replaced) eventChannel.send(SettingsEvent.PreferenceUpdateFailed)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                eventChannel.send(SettingsEvent.PreferenceUpdateFailed)
            }
        }
    }

    fun applySearchFilters(selection: DiscoveryFilterSelection) {
        discoveryController.applyFilters(
            therapyTypes = selection.therapyTypes,
            ageGroup = selection.ageGroup,
            diagnosis = selection.diagnosis,
            insurance = selection.insurance,
            radiusMiles = selection.radiusMiles
        )
    }

    fun updateDefaultRadius(radiusMiles: Int) {
        val criteria = discoveryState.value.criteria
        discoveryController.applyFilters(
            therapyTypes = criteria.therapyTypes,
            ageGroup = criteria.ageGroup,
            diagnosis = criteria.diagnosis,
            insurance = criteria.insurance,
            radiusMiles = radiusMiles
        )
    }

    fun clearProfile() {
        if (clearJob?.isActive == true) return
        clearJob = viewModelScope.launch {
            try {
                profileRepository.clearProfile()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                eventChannel.send(SettingsEvent.ClearFailed)
            }
        }
    }
}
