package com.chla.kindd.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chla.kindd.data.discovery.DiscoveryController
import com.chla.kindd.data.discovery.TherapyType
import com.chla.kindd.data.models.RegionalCenter
import com.chla.kindd.data.profile.RegionalCenterIdentity
import com.chla.kindd.data.profile.UserProfile
import com.chla.kindd.data.profile.UserProfileRepository
import com.chla.kindd.data.source.RegionalCenterDataSource
import com.chla.kindd.data.source.RegionalCenterLookup
import com.chla.kindd.ui.chat.ChatLaunchPrompt
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val profileRepository: UserProfileRepository,
    private val regionalCenterDataSource: RegionalCenterDataSource,
    private val discoveryController: DiscoveryController
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()

    private val eventChannel = Channel<HomeEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private var currentProfile = UserProfile()
    private var hydratedIdentity: RegionalCenterIdentity? = null
    private var hydrationGeneration = 0L
    private var hydrationJob: Job? = null
    private var lookupGeneration = 0L
    private var lookupJob: Job? = null

    init {
        viewModelScope.launch {
            profileRepository.profile.collect { profile ->
                currentProfile = profile
                val identity = profile.regionalCenter
                val identityChanged = identity != hydratedIdentity
                if (identityChanged) {
                    hydratedIdentity = identity
                    hydrationGeneration += 1
                    hydrationJob?.cancel()
                }
                mutableUiState.update { state ->
                    state.copy(
                        profile = profile,
                        zipDraft = profile.zipCode.orEmpty(),
                        hydratedCenter = if (identityChanged) null else state.hydratedCenter
                    )
                }
                if (identityChanged && identity != null) {
                    hydrate(identity, hydrationGeneration)
                }
            }
        }
    }

    fun onZipChanged(value: String) {
        invalidateLookup()
        val normalized = value.filter { character -> character in '0'..'9' }.take(5)
        mutableUiState.update {
            it.copy(zipDraft = normalized, lookupState = HomeLookupState.IDLE, message = null)
        }
    }

    fun submitZip() {
        val generation = invalidateLookup()
        val zipCode = uiState.value.zipDraft
        if (!zipCode.matches(Regex("[0-9]{5}"))) {
            mutableUiState.update {
                it.copy(lookupState = HomeLookupState.IDLE, message = HomeMessage.INVALID_ZIP)
            }
            return
        }

        lookupJob = viewModelScope.launch {
            if (!isCurrentLookup(generation)) return@launch
            mutableUiState.update {
                it.copy(lookupState = HomeLookupState.LOADING, message = null)
            }
            when (val lookup = regionalCenterDataSource.lookupRegionalCenter(zipCode)) {
                is RegionalCenterLookup.Matched -> {
                    if (!isCurrentLookup(generation)) return@launch
                    profileRepository.replaceProfile(
                        currentProfile.copy(
                            zipCode = zipCode,
                            regionalCenter = RegionalCenterIdentity.from(lookup.center)
                        )
                    )
                    if (!isCurrentLookup(generation)) return@launch
                    mutableUiState.update {
                        it.copy(lookupState = HomeLookupState.MATCHED, message = null)
                    }
                }
                RegionalCenterLookup.Unmatched -> {
                    if (isCurrentLookup(generation)) {
                        mutableUiState.update {
                            it.copy(
                                lookupState = HomeLookupState.UNMATCHED,
                                message = HomeMessage.NO_MATCH
                            )
                        }
                    }
                }
                is RegionalCenterLookup.Unavailable -> {
                    if (isCurrentLookup(generation)) {
                        mutableUiState.update {
                            it.copy(
                                lookupState = HomeLookupState.UNAVAILABLE,
                                message = HomeMessage.LOOKUP_UNAVAILABLE
                            )
                        }
                    }
                }
            }
        }
    }

    fun selectTherapy(therapyType: TherapyType) {
        discoveryController.setSingleTherapyAndRefresh(therapyType)
        eventChannel.trySend(HomeEvent.NavigateToList)
    }

    fun openMap() { eventChannel.trySend(HomeEvent.NavigateToMap) }
    fun openList() { eventChannel.trySend(HomeEvent.NavigateToList) }
    fun openRegionalCenters() { eventChannel.trySend(HomeEvent.NavigateToRegionalCenters) }
    fun openChat(prompt: ChatLaunchPrompt) {
        eventChannel.trySend(HomeEvent.NavigateToChat(prompt))
    }
    fun callCenter(digits: String) {
        if (digits.isNotEmpty() && digits.all { character -> character in '0'..'9' }) {
            eventChannel.trySend(HomeEvent.Dial(digits))
        }
    }

    private fun invalidateLookup(): Long {
        lookupGeneration += 1
        lookupJob?.cancel()
        lookupJob = null
        return lookupGeneration
    }

    private fun isCurrentLookup(generation: Long): Boolean = generation == lookupGeneration

    private fun hydrate(identity: RegionalCenterIdentity, generation: Long) {
        hydrationJob = viewModelScope.launch {
            try {
                val centers = regionalCenterDataSource.getRegionalCenters().getOrNull().orEmpty()
                val match = findCenter(identity, centers)
                if (generation == hydrationGeneration && identity == currentProfile.regionalCenter) {
                    mutableUiState.update { state -> state.copy(hydratedCenter = match) }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                if (generation == hydrationGeneration && identity == currentProfile.regionalCenter) {
                    mutableUiState.update { state -> state.copy(hydratedCenter = null) }
                }
            }
        }
    }

    private fun findCenter(
        identity: RegionalCenterIdentity,
        centers: List<RegionalCenter>
    ): RegionalCenter? = centers.firstOrNull { center -> center.id == identity.id }
        ?: centers.firstOrNull { center ->
            center.name.equals(identity.name, ignoreCase = true) ||
                center.shortName.equals(identity.shortName, ignoreCase = true)
        }
}
