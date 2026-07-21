package com.chla.kindd.ui.accessibility

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.chla.kindd.ui.screens.RegionalCentersTopAppBar
import com.chla.kindd.ui.theme.KINDDTheme
import com.chla.kindd.ui.theme.KiNDDTopAppBarColorContract
import com.chla.kindd.ui.theme.kinddTopAppBarColorContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AdaptiveAppBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun regionalCentersBarConsumesExactReadableProductionContractInLightTheme() =
        assertRegionalCentersAppBar(darkTheme = false)

    @Test
    fun regionalCentersBarConsumesExactReadableProductionContractInDarkTheme() =
        assertRegionalCentersAppBar(darkTheme = true)

    private fun assertRegionalCentersAppBar(darkTheme: Boolean) {
        lateinit var scheme: ColorScheme
        lateinit var contract: KiNDDTopAppBarColorContract
        composeRule.setContent {
            KINDDTheme(darkTheme = darkTheme) {
                val productionContract = kinddTopAppBarColorContract()
                val productionScheme = MaterialTheme.colorScheme
                SideEffect {
                    scheme = productionScheme
                    contract = productionContract
                }
                RegionalCentersTopAppBar(
                    onBack = {},
                    colorContract = productionContract
                )
            }
        }

        composeRule.onNodeWithText("LA County Regional Centers").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
        composeRule.runOnIdle { assertExactContract(scheme, contract) }
    }

    private fun assertExactContract(
        scheme: ColorScheme,
        contract: KiNDDTopAppBarColorContract
    ) {
        assertEquals(scheme.primaryContainer, contract.containerColor)
        listOf(
            contract.titleContentColor,
            contract.subtitleContentColor,
            contract.actionIconContentColor,
            contract.navigationIconContentColor
        ).forEach { contentColor ->
            assertEquals(scheme.onPrimaryContainer, contentColor)
            assertReadable(contract.containerColor, contentColor)
        }
    }

    private fun assertReadable(background: Color, foreground: Color) {
        val lighter = maxOf(background.luminance(), foreground.luminance()).toDouble()
        val darker = minOf(background.luminance(), foreground.luminance()).toDouble()
        val contrast = (lighter + 0.05) / (darker + 0.05)
        assertTrue("App bar contrast was $contrast", contrast >= 4.5)
    }
}
