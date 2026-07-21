package com.chla.kindd.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chla.kindd.data.discovery.DiscoveryController
import com.chla.kindd.data.discovery.TherapyType
import com.chla.kindd.data.models.RegionalCenter
import com.chla.kindd.data.profile.RegionalCenterIdentity
import com.chla.kindd.data.profile.UserProfile
import com.chla.kindd.data.profile.UserProfileRepository
import com.chla.kindd.data.servicearea.ServiceAreaDataSource
import com.chla.kindd.data.servicearea.ServiceAreaFeature
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
    private val discoveryController: DiscoveryController,
    private val serviceAreaDataSource: ServiceAreaDataSource = EmptyHomeServiceAreaDataSource
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()

    private val eventChannel = Channel<HomeEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    private var hydrationGeneration = 0L
    private var hydrationJob: Job? = null
    private var lookupGeneration = 0L
    private var lookupJob: Job? = null
    private var lastSynchronizedProfile: UserProfile? = null
    private var pendingSelfReplacement: PendingSelfReplacement? = null

    init {
        loadServiceAreas()
    }

    fun onReadyProfileChanged(profile: UserProfile) {
        pendingSelfReplacement?.takeIf { pending ->
            pending.replacement == profile
        }?.let { pending ->
            pending.rootObserved = true
            lastSynchronizedProfile = profile
            synchronizeHydratedIdentity(profile.regionalCenter)
            if (pending.casSucceeded) {
                clearPendingSelfReplacement(pending)
            }
            return
        }

        val previousProfile = lastSynchronizedProfile
        if (profile == previousProfile) return
        pendingSelfReplacement = null
        lastSynchronizedProfile = profile

        if (previousProfile != null) {
            invalidateLookup()
            mutableUiState.update { state ->
                state.copy(
                    lookupState = HomeLookupState.IDLE,
                    message = null
                )
            }
        }
        synchronizeHydratedIdentity(profile.regionalCenter)
    }

    fun onZipChanged(value: String) {
        invalidateLookup()
        val normalized = value.filter { character -> character in '0'..'9' }.take(5)
        mutableUiState.update {
            it.copy(
                zipDraft = normalized,
                isZipDraftDirty = true,
                lookupState = HomeLookupState.IDLE,
                message = null
            )
        }
    }

    fun submitZip(expectedProfile: UserProfile, displayedZip: String) {
        val generation = invalidateLookup()
        val zipCode = displayedZip
        if (!zipCode.matches(Regex("[0-9]{5}"))) {
            mutableUiState.update {
                it.copy(lookupState = HomeLookupState.IDLE, message = HomeMessage.INVALID_ZIP)
            }
            return
        }
        lookupJob = viewModelScope.launch {
            var replacementToken: PendingSelfReplacement? = null
            try {
                if (!isCurrentLookup(generation)) return@launch
                mutableUiState.update {
                    it.copy(lookupState = HomeLookupState.LOADING, message = null)
                }
                when (val lookup = regionalCenterDataSource.lookupRegionalCenter(zipCode)) {
                    is RegionalCenterLookup.Matched -> {
                        if (!isCurrentLookup(generation)) return@launch
                        val replacement = expectedProfile.copy(
                            zipCode = zipCode,
                            regionalCenter = RegionalCenterIdentity.from(lookup.center)
                        )
                        val token = PendingSelfReplacement(
                            replacement = replacement,
                            generation = generation
                        )
                        replacementToken = token
                        pendingSelfReplacement = token
                        val replaced = profileRepository.replaceProfileIfCurrent(
                            expected = expectedProfile,
                            replacement = replacement
                        )
                        if (!replaced) {
                            clearPendingSelfReplacement(token)
                            finishSupersededLookup(token.generation)
                            return@launch
                        }
                        token.casSucceeded = true
                        if (token.rootObserved) {
                            clearPendingSelfReplacement(token)
                        }
                        if (!isCurrentLookup(token.generation)) return@launch
                        mutableUiState.update {
                            it.copy(
                                zipDraft = zipCode,
                                isZipDraftDirty = false,
                                lookupState = HomeLookupState.MATCHED,
                                message = null
                            )
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
                        } else {
                            finishSupersededLookup(generation)
                        }
                    }
                    is RegionalCenterLookup.Unavailable -> {
                        if (isCurrentLookup(generation)) {
                            showLookupUnavailable()
                        } else {
                            finishSupersededLookup(generation)
                        }
                    }
                }
            } catch (cancellation: CancellationException) {
                replacementToken?.let(::clearPendingSelfReplacement)
                throw cancellation
            } catch (_: Exception) {
                replacementToken?.let(::clearPendingSelfReplacement)
                if (isCurrentLookup(generation)) {
                    showLookupUnavailable()
                } else {
                    finishSupersededLookup(generation)
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
    fun callCenter(authoritativeProfile: UserProfile, digits: String) {
        if (
            digits.isNotEmpty() &&
            digits.all { character -> character in '0'..'9' } &&
            digits == uiState.value.dialDigitsFor(authoritativeProfile)
        ) {
            eventChannel.trySend(HomeEvent.Dial(digits))
        }
    }

    private fun loadServiceAreas() {
        viewModelScope.launch {
            val result = try {
                serviceAreaDataSource.getServiceAreas()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                Result.failure(IllegalStateException("Service areas unavailable"))
            }
            mutableUiState.update { state ->
                result.fold(
                    onSuccess = { areas ->
                        state.copy(
                            serviceAreas = areas,
                            serviceAreaLoadState = ServiceAreaLoadState.READY
                        )
                    },
                    onFailure = {
                        state.copy(
                            serviceAreas = emptyList(),
                            serviceAreaLoadState = ServiceAreaLoadState.FAILED
                        )
                    }
                )
            }
        }
    }

    private fun invalidateLookup(): Long {
        lookupGeneration += 1
        lookupJob?.cancel()
        lookupJob = null
        return lookupGeneration
    }

    private fun isCurrentLookup(generation: Long): Boolean = generation == lookupGeneration

    private fun finishSupersededLookup(generation: Long) {
        if (isCurrentLookup(generation)) {
            mutableUiState.update {
                it.copy(lookupState = HomeLookupState.IDLE, message = null)
            }
        }
    }

    private fun showLookupUnavailable() {
        mutableUiState.update {
            it.copy(
                lookupState = HomeLookupState.UNAVAILABLE,
                message = HomeMessage.LOOKUP_UNAVAILABLE
            )
        }
    }

    private fun synchronizeHydratedIdentity(identity: RegionalCenterIdentity?) {
        if (identity == uiState.value.hydratedIdentity) return

        hydrationGeneration += 1
        hydrationJob?.cancel()
        mutableUiState.update { state ->
            state.copy(
                hydratedIdentity = identity,
                hydratedCenter = null
            )
        }
        if (identity != null) {
            hydrate(identity, hydrationGeneration)
        }
    }

    private fun clearPendingSelfReplacement(token: PendingSelfReplacement) {
        if (pendingSelfReplacement === token) {
            pendingSelfReplacement = null
        }
    }

    private fun hydrate(identity: RegionalCenterIdentity, generation: Long) {
        hydrationJob = viewModelScope.launch {
            try {
                val centers = regionalCenterDataSource.getRegionalCenters().getOrNull().orEmpty()
                val match = findCenter(identity, centers)
                if (generation == hydrationGeneration && identity == uiState.value.hydratedIdentity) {
                    mutableUiState.update { state -> state.copy(hydratedCenter = match) }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                if (generation == hydrationGeneration && identity == uiState.value.hydratedIdentity) {
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

    private data class PendingSelfReplacement(
        val replacement: UserProfile,
        val generation: Long,
        var rootObserved: Boolean = false,
        var casSucceeded: Boolean = false
    )
}

private object EmptyHomeServiceAreaDataSource : ServiceAreaDataSource {
    override suspend fun getServiceAreas(): Result<List<ServiceAreaFeature>> =
        Result.success(emptyList())
}
