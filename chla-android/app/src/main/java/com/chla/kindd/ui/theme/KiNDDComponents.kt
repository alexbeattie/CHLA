package com.chla.kindd.ui.theme

import android.animation.ValueAnimator
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private fun Modifier.kinddPressable(
    onClick: () -> Unit,
    role: Role,
    card: Boolean = false
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val spatialMotionEnabled = ValueAnimator.areAnimatorsEnabled()
    val targetScale = if (pressed && spatialMotionEnabled) {
        if (card) 0.985f else 0.97f
    } else {
        1f
    }
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "KiNDD press scale"
    )
    val alpha = if (pressed && !spatialMotionEnabled) 0.66f else 1f

    graphicsLayer {
        scaleX = scale
        scaleY = scale
        this.alpha = alpha
    }.clickable(
        interactionSource = interactionSource,
        indication = null,
        role = role,
        onClick = onClick
    )
}

@Composable
fun KiNDDGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(KiNDDShapeTokens.Card),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val darkTheme = isSystemInDarkTheme()
    val fill = if (darkTheme) {
        KiNDDSurfaceDark.copy(alpha = 0.92f)
    } else {
        Color.White.copy(alpha = 0.90f)
    }
    val highlight = if (darkTheme) {
        Color.White.copy(alpha = 0.16f)
    } else {
        Color.White.copy(alpha = 0.72f)
    }

    Box(
        modifier = modifier
            .shadow(24.dp, shape, ambientColor = Color.Black.copy(alpha = 0.16f))
            .shadow(4.dp, shape, ambientColor = Color.Black.copy(alpha = 0.10f))
            .clip(shape)
            .background(fill)
            .border(0.5.dp, highlight, shape)
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun KiNDDPrimaryGradientCapsule(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    content: @Composable BoxScope.() -> Unit
) {
    KiNDDGradientCapsule(
        brush = KiNDDPrimaryActionGradient,
        onClick = onClick,
        modifier = modifier,
        contentPadding = contentPadding,
        content = content
    )
}

@Composable
private fun KiNDDGradientCapsule(
    brush: Brush,
    onClick: () -> Unit,
    modifier: Modifier,
    contentPadding: PaddingValues,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(brush)
            .kinddPressable(onClick = onClick, role = Role.Button)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun KiNDDSecondaryCapsule(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 11.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(percent = 50)
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.30f), shape)
            .kinddPressable(onClick = onClick, role = Role.Button)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun KiNDDCompactIconAction(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    containerBrush: Brush? = null
) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .then(
                if (containerBrush == null) {
                    Modifier
                } else {
                    Modifier.background(containerBrush)
                }
            )
            .kinddPressable(onClick = onClick, role = Role.Button)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

@Composable
fun KiNDDCardSurface(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(KiNDDShapeTokens.Card)
    val clickableModifier = if (onClick == null) {
        Modifier
    } else {
        Modifier
            .sizeIn(minHeight = 48.dp)
            .kinddPressable(onClick = onClick, role = Role.Button, card = true)
    }
    Box(
        modifier = modifier
            .shadow(2.dp, shape, ambientColor = Color.Black.copy(alpha = 0.10f))
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), shape)
            .then(clickableModifier)
            .padding(contentPadding),
        content = content
    )
}
