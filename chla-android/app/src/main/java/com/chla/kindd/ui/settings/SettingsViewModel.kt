package com.chla.kindd.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chla.kindd.data.profile.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface SettingsEvent {
    data object NavigateToEditProfile : SettingsEvent
    data object ClearFailed : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val profileRepository: UserProfileRepository
) : ViewModel() {

    private val eventChannel = Channel<SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsEvent> = eventChannel.receiveAsFlow()
    private var clearJob: Job? = null

    fun editProfile() {
        eventChannel.trySend(SettingsEvent.NavigateToEditProfile)
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
