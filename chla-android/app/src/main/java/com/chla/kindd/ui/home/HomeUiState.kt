package com.chla.kindd.ui.home

import com.chla.kindd.data.models.RegionalCenter
import com.chla.kindd.data.profile.UserProfile
import com.chla.kindd.ui.chat.ChatLaunchPrompt

enum class HomeLookupState {
    IDLE,
    LOADING,
    MATCHED,
    UNMATCHED,
    UNAVAILABLE
}

enum class HomeMessage {
    INVALID_ZIP,
    NO_MATCH,
    LOOKUP_UNAVAILABLE
}

data class HomeUiState(
    val profile: UserProfile = UserProfile(),
    val hydratedCenter: RegionalCenter? = null,
    val zipDraft: String = "",
    val lookupState: HomeLookupState = HomeLookupState.IDLE,
    val message: HomeMessage? = null
) {
    val dialDigits: String?
        get() = hydratedCenter?.telephone
            ?.filter { character -> character in '0'..'9' }
            ?.takeIf(String::isNotEmpty)
}

sealed interface HomeEvent {
    data object NavigateToMap : HomeEvent
    data object NavigateToList : HomeEvent
    data object NavigateToRegionalCenters : HomeEvent
    data class NavigateToChat(val prompt: ChatLaunchPrompt) : HomeEvent
    data class Dial(val digits: String) : HomeEvent
}
