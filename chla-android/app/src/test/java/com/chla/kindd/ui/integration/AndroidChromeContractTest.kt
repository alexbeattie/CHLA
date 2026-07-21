package com.chla.kindd.ui.integration

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidChromeContractTest {

    @Test
    fun activityProvidesOpaqueThemeBackgroundWhileSystemBarsStayTransparent() {
        val activity = source("com/chla/kindd/MainActivity.kt")
        val theme = source("com/chla/kindd/ui/theme/Theme.kt")

        assertTrue(activity.contains("Surface("))
        assertTrue(activity.contains("modifier = Modifier.fillMaxSize()"))
        assertTrue(activity.contains("color = MaterialTheme.colorScheme.background"))
        assertTrue(theme.contains("window.statusBarColor = Color.Transparent.toArgb()"))
        assertTrue(theme.contains("window.navigationBarColor = Color.Transparent.toArgb()"))
        assertTrue(theme.contains("isAppearanceLightStatusBars = !darkTheme"))
        assertTrue(theme.contains("isAppearanceLightNavigationBars = !darkTheme"))
    }

    @Test
    fun hostIsTheOnlyOwnerOfFloatingNavigationClearance() {
        val navHost = source("com/chla/kindd/ui/navigation/KINDDNavHost.kt")
        val settings = source("com/chla/kindd/ui/screens/SettingsScreen.kt")
        val regions = source("com/chla/kindd/ui/regions/RegionalCentersContent.kt")

        assertTrue(navHost.contains("FloatingNavigationContentClearance"))
        assertTrue(navHost.contains("navigationBarsPadding()"))
        assertFalse(settings.contains("settings_bottom_navigation_clearance"))
        assertFalse(settings.contains(".height(112.dp)"))
        assertFalse(regions.contains(".padding(horizontal = 18.dp, vertical = 112.dp)"))
        assertFalse(regions.contains("Spacer(modifier = Modifier.height(120.dp))"))
        assertFalse(regions.contains("contentPadding = PaddingValues(bottom = 120.dp)"))
    }

    @Test
    fun onboardingTopChromeOwnsTheStatusBarInset() {
        val onboarding = source("com/chla/kindd/ui/onboarding/OnboardingRoute.kt")
        val progressStart = onboarding.indexOf("private fun OnboardingProgress(")
        val actionsStart = onboarding.indexOf("private fun OnboardingActions(")

        assertTrue(progressStart >= 0)
        assertTrue(actionsStart > progressStart)
        assertTrue(
            onboarding.substring(progressStart, actionsStart).contains(".statusBarsPadding()")
        )
    }

    private fun source(relativePath: String): String {
        val file = File("src/main/java/$relativePath")
        assertTrue("Missing production source: ${file.path}", file.isFile)
        return file.readText()
    }
}
