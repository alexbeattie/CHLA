package com.chla.kindd.ui.home

import com.chla.kindd.data.models.RegionalCenter
import com.chla.kindd.data.profile.RegionalCenterIdentity
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
    val hydratedIdentity: RegionalCenterIdentity? = null,
    val hydratedCenter: RegionalCenter? = null,
    val zipDraft: String = "",
    val isZipDraftDirty: Boolean = false,
    val lookupState: HomeLookupState = HomeLookupState.IDLE,
    val message: HomeMessage? = null
) {
    fun displayedZip(authoritativeProfile: UserProfile): String =
        if (isZipDraftDirty) zipDraft else authoritativeProfile.zipCode.orEmpty()

    fun centerDetailsFor(authoritativeProfile: UserProfile): RegionalCenter? =
        hydratedCenter.takeIf {
            hydratedIdentity != null && hydratedIdentity == authoritativeProfile.regionalCenter
        }

    fun dialDigitsFor(authoritativeProfile: UserProfile): String? =
        centerDetailsFor(authoritativeProfile)?.telephone
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
