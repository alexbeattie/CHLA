package com.chla.kindd.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chla.kindd.data.profile.AgeGroup
import com.chla.kindd.data.profile.AudienceType
import com.chla.kindd.data.profile.JourneyStage
import com.chla.kindd.data.profile.RegionalCenterIdentity
import com.chla.kindd.data.profile.UserProfile
import com.chla.kindd.data.profile.UserProfileRepository
import com.chla.kindd.data.source.LookupFailure
import com.chla.kindd.data.source.RegionalCenterDataSource
import com.chla.kindd.data.source.RegionalCenterLookup
import com.chla.kindd.data.source.UserLocationSource
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.CancellationException
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profileRepository: UserProfileRepository,
    private val regionalCenterDataSource: RegionalCenterDataSource,
    private val userLocationSource: UserLocationSource
) : ViewModel() {
    private val mutableUiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = mutableUiState.asStateFlow()

    private val eventChannel = Channel<OnboardingEvent>(Channel.BUFFERED)
    val events: Flow<OnboardingEvent> = eventChannel.receiveAsFlow()

    private var initialized = false
    private var operationGeneration = 0L
    private var lookupJob: Job? = null
    private var locationJob: Job? = null
    private var saveJob: Job? = null
    private var saveCompleted = false

    fun initialize(mode: OnboardingMode, initialProfile: UserProfile) {
        if (initialized) return
        initialized = true
        saveCompleted = false
        val draft = when (mode) {
            OnboardingMode.FIRST_RUN -> initialProfile.copy(
                audienceType = initialProfile.audienceType ?: AudienceType.FAMILY
            )
            OnboardingMode.EDIT -> initialProfile
        }
        mutableUiState.value = OnboardingUiState(mode = mode, draft = draft)
    }

    fun endSession() {
        invalidateAsyncWork()
        saveJob?.cancel()
        saveJob = null
        saveCompleted = false
        initialized = false
        mutableUiState.value = OnboardingUiState()
        while (eventChannel.tryReceive().isSuccess) {
            // Events belong to the session that just ended.
        }
    }

    fun selectAudience(audienceType: AudienceType) {
        if (interactionsLocked()) return
        mutableUiState.update { state ->
            state.copy(draft = state.draft.copy(audienceType = audienceType))
        }
    }

    fun onZipChanged(input: String) {
        val zipCode = input.filter { it in '0'..'9' }.take(ZIP_LENGTH)
        val currentState = mutableUiState.value
        if (interactionsLocked() || zipCode == currentState.draft.zipCode) return
        invalidateAsyncWork()
        mutableUiState.update { state ->
            state.copy(
                draft = state.draft.copy(zipCode = zipCode, regionalCenter = null),
                centerLookupState = CenterLookupState.IDLE,
                locationState = LocationState.IDLE,
                saveError = null
            )
        }
    }

    fun continueFromCurrentStep() {
        val state = mutableUiState.value
        if (interactionsLocked()) return
        when (state.step) {
            OnboardingStep.AUDIENCE -> if (state.canContinue) {
                mutableUiState.update { it.copy(step = OnboardingStep.ZIP) }
            }
            OnboardingStep.ZIP -> if (state.canContinue) {
                lookupCurrentZip()
            }
            OnboardingStep.REGIONAL_CENTER -> if (state.canContinue) {
                mutableUiState.update { it.copy(step = OnboardingStep.JOURNEY) }
            }
            OnboardingStep.JOURNEY -> if (state.canContinue) {
                mutableUiState.update { it.copy(step = OnboardingStep.AGE) }
            }
            OnboardingStep.AGE -> Unit
        }
    }

    fun goBack() {
        if (interactionsLocked()) return
        invalidateAsyncWork()
        mutableUiState.update { state ->
            val leavingAsyncStep = state.step == OnboardingStep.ZIP ||
                state.step == OnboardingStep.REGIONAL_CENTER
            state.copy(
                step = when (state.step) {
                    OnboardingStep.AUDIENCE -> OnboardingStep.AUDIENCE
                    OnboardingStep.ZIP -> OnboardingStep.AUDIENCE
                    OnboardingStep.REGIONAL_CENTER -> OnboardingStep.ZIP
                    OnboardingStep.JOURNEY -> OnboardingStep.REGIONAL_CENTER
                    OnboardingStep.AGE -> OnboardingStep.JOURNEY
                },
                centerLookupState = if (leavingAsyncStep) {
                    CenterLookupState.IDLE
                } else {
                    state.centerLookupState
                },
                locationState = if (leavingAsyncStep) {
                    LocationState.IDLE
                } else {
                    state.locationState
                },
                saveError = null
            )
        }
    }

    fun retryCenterLookup() {
        if (!interactionsLocked() && mutableUiState.value.canRetryCenterLookup) {
            lookupCurrentZip()
        }
    }

    fun hasLocationPermission(): Boolean = userLocationSource.hasLocationPermission()

    fun onLocationPermissionResult(granted: Boolean) {
        if (interactionsLocked()) return
        if (granted) {
            useCurrentLocation()
        } else {
            invalidateAsyncWork()
            mutableUiState.update {
                it.copy(
                    step = OnboardingStep.ZIP,
                    locationState = LocationState.DENIED,
                    centerLookupState = CenterLookupState.IDLE
                )
            }
        }
    }

    fun useCurrentLocation() {
        if (interactionsLocked()) return
        val generation = beginAsyncWork()
        mutableUiState.update {
            it.copy(
                step = OnboardingStep.ZIP,
                locationState = LocationState.LOCATING,
                centerLookupState = CenterLookupState.IDLE
            )
        }
        locationJob = viewModelScope.launch {
            try {
                val coordinates = userLocationSource.currentCoordinates()
                    ?: return@launch locationFailed(generation)
                if (!isCurrent(generation)) return@launch
                val zipCode = userLocationSource.zipCodeFor(coordinates)
                    ?.filter { it in '0'..'9' }
                    ?.take(ZIP_LENGTH)
                    ?.takeIf { it.length == ZIP_LENGTH }
                    ?: return@launch locationFailed(generation)
                if (!isCurrent(generation)) return@launch
                mutableUiState.update { state ->
                    state.copy(
                        draft = state.draft.copy(zipCode = zipCode, regionalCenter = null),
                        locationState = LocationState.IDLE,
                        centerLookupState = CenterLookupState.LOADING
                    )
                }
                performLookup(zipCode, generation)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                locationFailed(generation)
            }
        }
    }

    fun selectJourney(journeyStage: JourneyStage) {
        if (interactionsLocked()) return
        mutableUiState.update { state ->
            state.copy(draft = state.draft.copy(journeyStage = journeyStage))
        }
    }

    fun selectAgeGroup(ageGroup: AgeGroup) {
        if (interactionsLocked()) return
        mutableUiState.update { state ->
            state.copy(
                draft = state.draft.copy(
                    ageGroup = ageGroup.takeUnless { it == state.draft.ageGroup }
                )
            )
        }
    }

    fun finish() {
        val state = mutableUiState.value
        if (interactionsLocked() || !state.canContinue || state.step != OnboardingStep.AGE) return
        val generation = beginAsyncWork()
        val completedProfile = state.draft.copy(onboardingCompleted = true)
        mutableUiState.update { it.copy(isSaving = true, saveError = null) }
        saveJob = viewModelScope.launch {
            try {
                profileRepository.replaceProfile(completedProfile)
                if (!isCurrent(generation)) return@launch
                saveCompleted = true
                eventChannel.send(OnboardingEvent.Saved)
                completeSessionPresentation()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                if (!isCurrent(generation)) return@launch
                saveCompleted = false
                mutableUiState.update {
                    it.copy(isSaving = false, saveError = SaveError.RETRY)
                }
            }
        }
    }

    fun cancel() {
        if (interactionsLocked()) return
        if (eventChannel.trySend(OnboardingEvent.Close).isSuccess) {
            completeSessionPresentation()
        }
    }

    private fun lookupCurrentZip() {
        val zipCode = mutableUiState.value.draft.zipCode ?: return
        val generation = beginAsyncWork()
        mutableUiState.update {
            it.copy(centerLookupState = CenterLookupState.LOADING, locationState = LocationState.IDLE)
        }
        lookupJob = viewModelScope.launch { performLookup(zipCode, generation) }
    }

    private suspend fun performLookup(zipCode: String, generation: Long) {
        val lookup = try {
            regionalCenterDataSource.lookupRegionalCenter(zipCode)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            RegionalCenterLookup.Unavailable(LookupFailure.UNKNOWN)
        }
        if (!isCurrent(generation) || mutableUiState.value.draft.zipCode != zipCode) return
        mutableUiState.update { state ->
            when (lookup) {
                is RegionalCenterLookup.Matched -> state.copy(
                    step = OnboardingStep.REGIONAL_CENTER,
                    draft = state.draft.copy(
                        regionalCenter = RegionalCenterIdentity.from(lookup.center)
                    ),
                    centerLookupState = CenterLookupState.MATCHED
                )
                RegionalCenterLookup.Unmatched -> state.copy(
                    step = OnboardingStep.REGIONAL_CENTER,
                    draft = state.draft.copy(regionalCenter = null),
                    centerLookupState = CenterLookupState.UNMATCHED
                )
                is RegionalCenterLookup.Unavailable -> state.copy(
                    step = OnboardingStep.REGIONAL_CENTER,
                    centerLookupState = CenterLookupState.UNAVAILABLE
                )
            }
        }
    }

    private fun locationFailed(generation: Long) {
        if (!isCurrent(generation)) return
        mutableUiState.update {
            it.copy(step = OnboardingStep.ZIP, locationState = LocationState.FAILED)
        }
    }

    private fun beginAsyncWork(): Long {
        invalidateAsyncWork()
        return operationGeneration
    }

    private fun invalidateAsyncWork() {
        operationGeneration += 1
        lookupJob?.cancel()
        lookupJob = null
        locationJob?.cancel()
        locationJob = null
    }

    private fun isCurrent(generation: Long): Boolean =
        initialized && generation == operationGeneration

    private fun interactionsLocked(): Boolean =
        mutableUiState.value.isSaving || saveCompleted

    private fun completeSessionPresentation() {
        invalidateAsyncWork()
        saveJob = null
        initialized = false
        saveCompleted = true
        mutableUiState.value = OnboardingUiState()
    }

    private companion object {
        const val ZIP_LENGTH = 5
    }
}
