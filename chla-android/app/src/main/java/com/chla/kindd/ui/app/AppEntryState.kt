package com.chla.kindd.ui.app

import com.chla.kindd.data.profile.UserProfile

sealed interface AppEntryState {
    data object Loading : AppEntryState
    data class NeedsOnboarding(val draft: UserProfile) : AppEntryState
    data class Ready(val profile: UserProfile) : AppEntryState
}
