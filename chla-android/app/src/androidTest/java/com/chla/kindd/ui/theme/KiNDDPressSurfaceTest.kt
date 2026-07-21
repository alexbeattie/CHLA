package com.chla.kindd.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.center
import androidx.compose.ui.test.down
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class KiNDDPressSurfaceTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pressFeedbackMovesTheWholeDrawnSurfaceForEverySharedClickableControl() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            KINDDTheme {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        Modifier
                            .width(180.dp)
                            .height(56.dp)
                            .background(Color.Green)
                            .testTag(PRIMARY_FRAME_TAG)
                    ) {
                        KiNDDPrimaryGradientCapsule(
                            onClick = {},
                            modifier = Modifier
                                .matchParentSize()
                                .testTag(PRIMARY_TAG)
                        ) { Text("Primary") }
                    }
                    Box(
                        Modifier
                            .width(180.dp)
                            .height(56.dp)
                            .background(Color.Green)
                            .testTag(SECONDARY_FRAME_TAG)
                    ) {
                        KiNDDSecondaryCapsule(
                            onClick = {},
                            modifier = Modifier
                                .matchParentSize()
                                .testTag(SECONDARY_TAG)
                        ) { Text("Secondary") }
                    }
                    Box(
                        Modifier
                            .width(48.dp)
                            .height(48.dp)
                            .background(Color.Green)
                            .testTag(ICON_FRAME_TAG)
                    ) {
                        KiNDDCompactIconAction(
                            icon = Icons.Filled.Home,
                            contentDescription = "Home",
                            onClick = {},
                            containerBrush = KiNDDAiGradient,
                            modifier = Modifier.testTag(ICON_TAG)
                        )
                    }
                    Box(
                        Modifier
                            .width(180.dp)
                            .height(80.dp)
                            .background(Color.Green)
                            .testTag(CARD_FRAME_TAG)
                    ) {
                        KiNDDCardSurface(
                            onClick = {},
                            contentPadding = PaddingValues(8.dp),
                            modifier = Modifier
                                .matchParentSize()
                                .testTag(CARD_TAG)
                        ) { Text("Card") }
                    }
                }
            }
        }

        listOf(
            PRIMARY_TAG to PRIMARY_FRAME_TAG,
            SECONDARY_TAG to SECONDARY_FRAME_TAG,
            ICON_TAG to ICON_FRAME_TAG,
            CARD_TAG to CARD_FRAME_TAG
        ).forEach { (tag, frameTag) ->
            val node = composeRule.onNodeWithTag(tag)
            val frame = composeRule.onNodeWithTag(frameTag)
            val before = frame.captureToImage().toPixelMap().let { pixels ->
                pixels[2, pixels.height / 2].toArgb()
            }
            node.performTouchInput { down(center) }
            composeRule.mainClock.advanceTimeBy(600)
            val pressed = frame.captureToImage().toPixelMap().let { pixels ->
                pixels[2, pixels.height / 2].toArgb()
            }
            node.performTouchInput { up() }
            composeRule.mainClock.advanceTimeBy(600)

            assertNotEquals("$tag must transform its drawn edge, not only content", before, pressed)
        }
    }

    private companion object {
        const val PRIMARY_TAG = "press_primary"
        const val PRIMARY_FRAME_TAG = "press_primary_frame"
        const val SECONDARY_TAG = "press_secondary"
        const val SECONDARY_FRAME_TAG = "press_secondary_frame"
        const val ICON_TAG = "press_icon"
        const val ICON_FRAME_TAG = "press_icon_frame"
        const val CARD_TAG = "press_card"
        const val CARD_FRAME_TAG = "press_card_frame"
    }
}
