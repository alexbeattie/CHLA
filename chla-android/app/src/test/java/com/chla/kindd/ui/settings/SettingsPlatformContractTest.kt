package com.chla.kindd.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsPlatformContractTest {

    @Test
    fun perAppLanguageSettingsAreOnlyOfferedByAndroid13AndNewer() {
        assertFalse(supportsPerAppLanguageSettings(32))
        assertTrue(supportsPerAppLanguageSettings(33))
        assertTrue(supportsPerAppLanguageSettings(34))
    }

    @Test
    fun locationPermissionStatusRefreshesAfterReturningFromSystemSettings() {
        var permissionAllowed = false
        val status = SettingsLocationPermissionStatus { permissionAllowed }

        assertFalse(status.isAllowed)
        permissionAllowed = true
        status.refresh()

        assertTrue(status.isAllowed)
    }

    @Test
    fun versionLabelIncludesReleaseAndBuild() {
        assertEquals("1.4.1 (42)", formatSettingsAppVersion("1.4.1", 42))
    }
}
