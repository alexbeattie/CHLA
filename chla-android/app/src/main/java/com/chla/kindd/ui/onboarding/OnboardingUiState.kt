package com.chla.kindd.ui.onboarding

import com.chla.kindd.data.profile.UserProfile

enum class OnboardingMode {
    FIRST_RUN,
    EDIT;

    /** Editing an existing profile skips the welcome intro. */
    val firstStep: OnboardingStep
        get() = when (this) {
            FIRST_RUN -> OnboardingStep.INTRO
            EDIT -> OnboardingStep.AUDIENCE
        }
}

enum class OnboardingStep {
    INTRO,
    AUDIENCE,
    ZIP,
    REGIONAL_CENTER,
    HOW_TO,
    JOURNEY,
    AGE;

    companion object {
        /** Steps counted by the progress capsules; the intro sits in front of them. */
        val questionSteps: List<OnboardingStep> = entries.filter { it != INTRO }
    }
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
    val step: OnboardingStep = mode.firstStep,
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
            OnboardingStep.INTRO -> true
            OnboardingStep.AUDIENCE -> draft.audienceType != null
            OnboardingStep.ZIP ->
                draft.zipCode?.matches(ASCII_ZIP) == true &&
                    centerLookupState != CenterLookupState.LOADING &&
                    locationState != LocationState.LOCATING
            OnboardingStep.REGIONAL_CENTER -> when (centerLookupState) {
                CenterLookupState.MATCHED,
                CenterLookupState.UNMATCHED -> true
                CenterLookupState.UNAVAILABLE ->
                    mode == OnboardingMode.FIRST_RUN || draft.regionalCenter != null
                CenterLookupState.IDLE,
                CenterLookupState.LOADING -> false
            }
            OnboardingStep.HOW_TO,
            OnboardingStep.JOURNEY -> true
            OnboardingStep.AGE ->
                draft.audienceType != null &&
                    draft.zipCode?.matches(ASCII_ZIP) == true
        }

    val isFirstStep: Boolean
        get() = step == mode.firstStep

    /** 1-based position among [OnboardingStep.questionSteps]; 0 on the intro. */
    val progressStep: Int
        get() = OnboardingStep.questionSteps.indexOf(step) + 1

    private companion object {
        val ASCII_ZIP = Regex("[0-9]{5}")
    }
}
