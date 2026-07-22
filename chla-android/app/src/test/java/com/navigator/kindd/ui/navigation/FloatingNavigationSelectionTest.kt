package com.navigator.kindd.ui.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FloatingNavigationSelectionTest {

    @Test
    fun moreOwnsItsInformationalChildRoutes() {
        assertTrue(isMoreRoute(Screen.More.route))
        assertTrue(isMoreRoute(Screen.Settings.route))
        assertTrue(isMoreRoute(Screen.FAQ.route))
        assertTrue(isMoreRoute(Screen.About.route))
    }

    @Test
    fun regionalCenters_remainsOwnedByTheRegionsDestination() {
        assertFalse(isMoreRoute(Screen.RegionalCenters.route))
    }
}
