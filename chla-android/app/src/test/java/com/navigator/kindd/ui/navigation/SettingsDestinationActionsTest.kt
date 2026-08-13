package com.navigator.kindd.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsDestinationActionsTest {

    @Test
    fun productionSettingsCallbacksRetainFaqAboutEditAndBackActions() {
        var faq = 0
        var about = 0
        var edit = 0
        var back = 0
        val mainActions = MainNavActions(
            navigateToMap = {},
            navigateToList = {},
            navigateToRegions = {},
            navigateToChat = {},
            navigateToProviderDetail = {},
            navigateToFaq = { faq += 1 },
            navigateToAbout = { about += 1 },
            navigateToEditProfile = { edit += 1 },
            navigateBack = { back += 1 }
        )

        val settingsActions = mainActions.toSettingsDestinationActions()
        settingsActions.navigateToFaq()
        settingsActions.navigateToAbout()
        settingsActions.navigateToEditProfile()
        settingsActions.navigateBackToMore()

        assertEquals(1, faq)
        assertEquals(1, about)
        assertEquals(1, edit)
        assertEquals(1, back)
    }
}
