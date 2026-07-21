package com.chla.kindd.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
object KiNDDShapeTokens {
    val Selection = 14.dp
    val Compact = 16.dp
    val Card = 20.dp
    val Hero = 26.dp
    val Sheet = 28.dp
}

@Immutable
object KiNDDSpacingTokens {
    val PageInset = 18.dp
    val Section = 22.dp
    val FloatingNavigationBottom = 8.dp
    val FloatingNavigationContentClearance = 80.dp
}

val KiNDDPrimaryActionGradient: Brush
    get() = Brush.linearGradient(listOf(KiNDDIndigo, KiNDDViolet))

val KiNDDAiGradient: Brush
    get() = Brush.linearGradient(listOf(KiNDDViolet, KiNDDPink))

@Composable
fun kinddTopWash(): Brush {
    val darkTheme = isSystemInDarkTheme()
    return remember(darkTheme) {
        Brush.verticalGradient(
            colors = listOf(
                KiNDDIndigo.copy(alpha = if (darkTheme) 0.20f else 0.09f),
                Color.Transparent
            )
        )
    }
}
