package com.chla.kindd.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = KiNDDDeepIndigo,
    onPrimary = KiNDDSurfaceLight,
    primaryContainer = Color(0xFFE2E1FF),
    onPrimaryContainer = Color(0xFF170065),
    secondary = KiNDDViolet,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFEDE5FF),
    onSecondaryContainer = Color(0xFF251047),
    tertiary = KiNDDMatchedGreen,
    onTertiary = KiNDDCanvasDark,
    tertiaryContainer = Color(0xFFD5F6E9),
    onTertiaryContainer = Color(0xFF002117),
    background = KiNDDCanvasLight,
    onBackground = KiNDDOnSurfaceLight,
    surface = KiNDDSurfaceLight,
    onSurface = KiNDDOnSurfaceLight,
    surfaceVariant = KiNDDSurfaceVariantLight,
    onSurfaceVariant = KiNDDOnSurfaceVariantLight,
    outline = KiNDDOutlineLight,
    error = KiNDDErrorLight,
    onError = KiNDDOnErrorLight,
    errorContainer = KiNDDErrorContainerLight,
    onErrorContainer = KiNDDOnErrorContainerLight,
)

private val DarkColorScheme = darkColorScheme(
    primary = KiNDDViolet,
    onPrimary = Color.Black,
    primaryContainer = KiNDDDeepIndigo,
    onPrimaryContainer = Color(0xFFE2E1FF),
    secondary = KiNDDPurple,
    onSecondary = KiNDDCanvasDark,
    secondaryContainer = Color(0xFF4D2875),
    onSecondaryContainer = Color(0xFFEDE5FF),
    tertiary = Color(0xFF6EE7B7),
    onTertiary = Color(0xFF00382A),
    tertiaryContainer = KiNDDMatchedGreen,
    onTertiaryContainer = Color(0xFF002117),
    background = KiNDDCanvasDark,
    onBackground = KiNDDOnSurfaceDark,
    surface = KiNDDSurfaceDark,
    onSurface = KiNDDOnSurfaceDark,
    surfaceVariant = KiNDDSurfaceVariantDark,
    onSurfaceVariant = KiNDDOnSurfaceVariantDark,
    outline = KiNDDOutlineDark,
    error = KiNDDErrorDark,
    onError = KiNDDOnErrorDark,
    errorContainer = KiNDDErrorContainerDark,
    onErrorContainer = KiNDDOnErrorContainerDark,
)

private val KiNDDShapes = Shapes(
    extraSmall = RoundedCornerShape(KiNDDShapeTokens.Selection),
    small = RoundedCornerShape(KiNDDShapeTokens.Compact),
    medium = RoundedCornerShape(KiNDDShapeTokens.Card),
    large = RoundedCornerShape(KiNDDShapeTokens.Hero),
    extraLarge = RoundedCornerShape(KiNDDShapeTokens.Sheet)
)

@Composable
fun KINDDTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KiNDDTypography,
        shapes = KiNDDShapes,
        content = content
    )
}
