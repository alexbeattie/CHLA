package com.chla.kindd.ui.onboarding

import com.chla.kindd.data.profile.UserProfile

enum class OnboardingMode {
    FIRST_RUN,
    EDIT
}

enum class OnboardingStep {
    AUDIENCE,
    ZIP,
    REGIONAL_CENTER,
    JOURNEY,
    AGE
}

enum class CenterLookupState {
    IDLE,
    LOADING,
    MATCHED,
    UNMATCHED,
    UNAVAILABLE
}

enum class LocationState {
    IDLE,
    LOCATING,
    DENIED,
    FAILED
}

enum class SaveError {
    RETRY
}

sealed interface OnboardingEvent {
    data object Saved : OnboardingEvent
    data object Close : OnboardingEvent
}

data class OnboardingUiState(
    val mode: OnboardingMode = OnboardingMode.FIRST_RUN,
    val step: OnboardingStep = OnboardingStep.AUDIENCE,
    val draft: UserProfile = UserProfile(),
    val centerLookupState: CenterLookupState = CenterLookupState.IDLE,
    val locationState: LocationState = LocationState.IDLE,
    val isSaving: Boolean = false,
    val saveError: SaveError? = null
) {
    val canRetryCenterLookup: Boolean
        get() = centerLookupState == CenterLookupState.UNAVAILABLE

    val canContinue: Boolean
        get() = when (step) {
            OnboardingStep.AUDIENCE -> draft.audienceType != null
            OnboardingStep.ZIP ->
                draft.zipCode?.matches(ASCII_ZIP) == true &&
                    centerLookupState != CenterLookupState.LOADING &&
                    locationState != LocationState.LOCATING
            OnboardingStep.REGIONAL_CENTER -> when (centerLookupState) {
                CenterLookupState.MATCHED,
                CenterLookupState.UNMATCHED -> true
                CenterLookupState.UNAVAILABLE -> mode == OnboardingMode.FIRST_RUN
                CenterLookupState.IDLE,
                CenterLookupState.LOADING -> false
            }
            OnboardingStep.JOURNEY -> draft.journeyStage != null
            OnboardingStep.AGE ->
                draft.audienceType != null &&
                    draft.zipCode?.matches(ASCII_ZIP) == true &&
                    draft.journeyStage != null
        }

    val progressStep: Int
        get() = step.ordinal + 1

    private companion object {
        val ASCII_ZIP = Regex("[0-9]{5}")
    }
}
