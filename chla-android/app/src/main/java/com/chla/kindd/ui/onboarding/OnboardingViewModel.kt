package com.chla.kindd.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chla.kindd.data.profile.AgeGroup
import com.chla.kindd.data.profile.AudienceType
import com.chla.kindd.data.profile.JourneyStage
import com.chla.kindd.data.profile.RegionalCenterIdentity
import com.chla.kindd.data.profile.UserProfile
import com.chla.kindd.data.profile.UserProfileRepository
import com.chla.kindd.data.source.RegionalCenterDataSource
import com.chla.kindd.data.source.RegionalCenterLookup
import com.chla.kindd.data.source.UserLocationSource
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.CancellationException
import javax.inject.Inject
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

    fun initialize(mode: OnboardingMode, initialProfile: UserProfile) {
        if (initialized) return
        initialized = true
        val draft = when (mode) {
            OnboardingMode.FIRST_RUN -> initialProfile.copy(
                audienceType = initialProfile.audienceType ?: AudienceType.FAMILY
            )
            OnboardingMode.EDIT -> initialProfile
        }
        mutableUiState.value = OnboardingUiState(mode = mode, draft = draft)
    }

    fun selectAudience(audienceType: AudienceType) {
        mutableUiState.update { state ->
            state.copy(draft = state.draft.copy(audienceType = audienceType))
        }
    }

    fun onZipChanged(input: String) {
        val zipCode = input.filter { it in '0'..'9' }.take(ZIP_LENGTH)
        mutableUiState.update { state ->
            if (zipCode == state.draft.zipCode) {
                state
            } else {
                state.copy(
                    draft = state.draft.copy(zipCode = zipCode, regionalCenter = null),
                    centerLookupState = CenterLookupState.IDLE,
                    locationState = LocationState.IDLE,
                    saveError = null
                )
            }
        }
    }

    fun continueFromCurrentStep() {
        val state = mutableUiState.value
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
        mutableUiState.update { state ->
            state.copy(
                step = when (state.step) {
                    OnboardingStep.AUDIENCE -> OnboardingStep.AUDIENCE
                    OnboardingStep.ZIP -> OnboardingStep.AUDIENCE
                    OnboardingStep.REGIONAL_CENTER -> OnboardingStep.ZIP
                    OnboardingStep.JOURNEY -> OnboardingStep.REGIONAL_CENTER
                    OnboardingStep.AGE -> OnboardingStep.JOURNEY
                },
                saveError = null
            )
        }
    }

    fun retryCenterLookup() {
        if (mutableUiState.value.canRetryCenterLookup) lookupCurrentZip()
    }

    fun hasLocationPermission(): Boolean = userLocationSource.hasLocationPermission()

    fun onLocationPermissionResult(granted: Boolean) {
        if (granted) {
            useCurrentLocation()
        } else {
            mutableUiState.update {
                it.copy(step = OnboardingStep.ZIP, locationState = LocationState.DENIED)
            }
        }
    }

    fun useCurrentLocation() {
        viewModelScope.launch {
            mutableUiState.update {
                it.copy(
                    step = OnboardingStep.ZIP,
                    locationState = LocationState.LOCATING,
                    centerLookupState = CenterLookupState.IDLE
                )
            }
            try {
                val coordinates = userLocationSource.currentCoordinates()
                    ?: return@launch locationFailed()
                val zipCode = userLocationSource.zipCodeFor(coordinates)
                    ?.filter { it in '0'..'9' }
                    ?.take(ZIP_LENGTH)
                    ?.takeIf { it.length == ZIP_LENGTH }
                    ?: return@launch locationFailed()
                mutableUiState.update { state ->
                    state.copy(
                        draft = state.draft.copy(zipCode = zipCode, regionalCenter = null),
                        locationState = LocationState.IDLE
                    )
                }
                lookupZip(zipCode)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                locationFailed()
            }
        }
    }

    fun selectJourney(journeyStage: JourneyStage) {
        mutableUiState.update { state ->
            state.copy(draft = state.draft.copy(journeyStage = journeyStage))
        }
    }

    fun selectAgeGroup(ageGroup: AgeGroup) {
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
        if (state.isSaving || !state.canContinue || state.step != OnboardingStep.AGE) return
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSaving = true, saveError = null) }
            try {
                profileRepository.replaceProfile(
                    mutableUiState.value.draft.copy(onboardingCompleted = true)
                )
                mutableUiState.update { it.copy(isSaving = false) }
                eventChannel.send(OnboardingEvent.Saved)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                mutableUiState.update {
                    it.copy(isSaving = false, saveError = SaveError.RETRY)
                }
            }
        }
    }

    fun cancel() {
        eventChannel.trySend(OnboardingEvent.Close)
    }

    private fun lookupCurrentZip() {
        val zipCode = mutableUiState.value.draft.zipCode ?: return
        viewModelScope.launch { lookupZip(zipCode) }
    }

    private suspend fun lookupZip(zipCode: String) {
        mutableUiState.update {
            it.copy(centerLookupState = CenterLookupState.LOADING, locationState = LocationState.IDLE)
        }
        val lookup = try {
            regionalCenterDataSource.lookupRegionalCenter(zipCode)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            RegionalCenterLookup.Unavailable(com.chla.kindd.data.source.LookupFailure.UNKNOWN)
        }
        if (mutableUiState.value.draft.zipCode != zipCode) return
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
                    draft = state.draft.copy(regionalCenter = null),
                    centerLookupState = CenterLookupState.UNAVAILABLE
                )
            }
        }
    }

    private fun locationFailed() {
        mutableUiState.update {
            it.copy(step = OnboardingStep.ZIP, locationState = LocationState.FAILED)
        }
    }

    private companion object {
        const val ZIP_LENGTH = 5
    }
}
