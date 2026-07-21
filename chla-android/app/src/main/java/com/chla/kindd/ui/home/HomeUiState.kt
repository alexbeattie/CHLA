package com.chla.kindd.ui.home

import com.chla.kindd.data.models.RegionalCenter
import com.chla.kindd.data.models.RegionalCenterContactCatalog
import com.chla.kindd.data.profile.RegionalCenterIdentity
import com.chla.kindd.data.profile.UserProfile
import com.chla.kindd.data.servicearea.ServiceAreaFeature
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

enum class ServiceAreaLoadState {
    LOADING,
    READY,
    FAILED
}

data class HomeUiState(
    val hydratedIdentity: RegionalCenterIdentity? = null,
    val hydratedCenter: RegionalCenter? = null,
    val zipDraft: String = "",
    val isZipDraftDirty: Boolean = false,
    val lookupState: HomeLookupState = HomeLookupState.IDLE,
    val message: HomeMessage? = null,
    val serviceAreas: List<ServiceAreaFeature> = emptyList(),
    val serviceAreaLoadState: ServiceAreaLoadState = ServiceAreaLoadState.LOADING
) {
    fun displayedZip(authoritativeProfile: UserProfile): String =
        if (isZipDraftDirty) zipDraft else authoritativeProfile.zipCode.orEmpty()

    fun centerDetailsFor(authoritativeProfile: UserProfile): RegionalCenter? =
        hydratedCenter.takeIf {
            hydratedIdentity != null && hydratedIdentity == authoritativeProfile.regionalCenter
        }

    fun formattedPhoneFor(authoritativeProfile: UserProfile): String? {
        val hydratedDetails = centerDetailsFor(authoritativeProfile)
        if (hydratedDetails != null && !hydratedDetails.telephone.isNullOrBlank()) {
            return hydratedDetails.formattedPhone
        }
        return RegionalCenterContactCatalog.phoneFor(
            authoritativeProfile.regionalCenter?.shortName
        )
    }

    fun dialDigitsFor(authoritativeProfile: UserProfile): String? =
        resolvedPhoneFor(authoritativeProfile)
            ?.filter { character -> character in '0'..'9' }
            ?.takeIf(String::isNotEmpty)

    private fun resolvedPhoneFor(authoritativeProfile: UserProfile): String? =
        centerDetailsFor(authoritativeProfile)
            ?.telephone
            ?.takeIf(String::isNotBlank)
            ?: RegionalCenterContactCatalog.phoneFor(
                authoritativeProfile.regionalCenter?.shortName
            )
}

sealed interface HomeEvent {
    data object NavigateToMap : HomeEvent
    data object NavigateToList : HomeEvent
    data object NavigateToRegionalCenters : HomeEvent
    data class NavigateToChat(val prompt: ChatLaunchPrompt) : HomeEvent
    data class Dial(val digits: String) : HomeEvent
}
