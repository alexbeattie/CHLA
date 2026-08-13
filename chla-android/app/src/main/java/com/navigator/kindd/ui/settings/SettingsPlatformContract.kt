package com.navigator.kindd.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal fun supportsPerAppLanguageSettings(sdkInt: Int): Boolean = sdkInt >= 33

internal class SettingsLocationPermissionStatus(
    private val permissionCheck: () -> Boolean
) {
    var isAllowed by mutableStateOf(permissionCheck())
        private set

    fun refresh() {
        isAllowed = permissionCheck()
    }
}

internal fun formatSettingsAppVersion(versionName: String, versionCode: Int): String =
    "$versionName ($versionCode)"
