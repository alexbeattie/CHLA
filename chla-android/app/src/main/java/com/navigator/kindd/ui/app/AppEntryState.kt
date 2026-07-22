package com.navigator.kindd.ui.app

import com.navigator.kindd.data.profile.UserProfile

sealed interface AppEntryState {
    data object Loading : AppEntryState
    data class NeedsOnboarding(val draft: UserProfile) : AppEntryState
    data class Ready(val profile: UserProfile) : AppEntryState
}
