package com.navigator.kindd.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.navigator.kindd.R
import com.navigator.kindd.data.profile.RegionalCenterIdentity
import com.navigator.kindd.ui.map.RegionalCenterMapRenderModel
import com.navigator.kindd.ui.map.mapAttributionBottomPaddingDp
import com.navigator.kindd.ui.regions.RegionalCenterServiceAreaMap
import com.navigator.kindd.ui.regions.RegionalCenterServiceAreaState
import com.navigator.kindd.ui.theme.KiNDDIndigo
import com.navigator.kindd.ui.theme.KiNDDMatchedGreen
import com.navigator.kindd.ui.theme.KiNDDShapeTokens

@Composable
internal fun RegionalCenterStep(
    center: RegionalCenterIdentity?,
    lookupState: CenterLookupState,
    serviceAreaState: RegionalCenterServiceAreaState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    mapContent: (@Composable (RegionalCenterMapRenderModel, (String) -> Unit) -> Unit)? = null
) {
    val density = LocalDensity.current
    val separateCard = shouldSeparateMatchedCenterCard(density.fontScale)
    var overlayCardHeightPx by remember(separateCard) { mutableIntStateOf(0) }
    val mapContentPadding = PaddingValues(
        bottom = mapAttributionBottomPaddingDp(
            overlayHeightPx = overlayCardHeightPx,
            density = density.density,
            overlaysMap = !separateCard
        ).dp
    )
    OnboardingStepColumn(
        modifier = modifier
            .testTag("onboarding_center_status")
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .clip(RoundedCornerShape(KiNDDShapeTokens.Hero))
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                    RoundedCornerShape(KiNDDShapeTokens.Hero)
                )
                .testTag("onboarding_center_map_hero")
        ) {
            RegionalCenterServiceAreaMap(
                state = serviceAreaState,
                highlightedAcronym = center?.shortName,
                interactive = false,
                onAreaClick = {},
                contentPadding = mapContentPadding,
                modifier = Modifier.matchParentSize(),
                mapContent = mapContent
            )
            if (!separateCard) {
                CenterStatusCard(
                    center = center,
                    lookupState = lookupState,
                    onRetry = onRetry,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(10.dp)
                        .onSizeChanged { overlayCardHeightPx = it.height }
                )
            }
        }
        if (separateCard) {
            CenterStatusCard(
                center = center,
                lookupState = lookupState,
                onRetry = onRetry
            )
        }
    }
}

internal fun shouldSeparateMatchedCenterCard(fontScale: Float): Boolean = fontScale >= 1.3f

@Composable
private fun CenterStatusCard(
    center: RegionalCenterIdentity?,
    lookupState: CenterLookupState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                RoundedCornerShape(KiNDDShapeTokens.Card)
            )
            .border(
                1.dp,
                if (lookupState == CenterLookupState.MATCHED) {
                    KiNDDIndigo.copy(alpha = 0.25f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
                },
                RoundedCornerShape(KiNDDShapeTokens.Card)
            )
            .testTag("onboarding_matched_center_card")
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (lookupState) {
            CenterLookupState.MATCHED -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.onboarding_center_title),
                        modifier = Modifier.weight(1f).semantics { heading() },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(KiNDDMatchedGreen, CircleShape)
                        )
                        Text(
                            text = stringResource(R.string.onboarding_center_matched),
                            style = MaterialTheme.typography.labelMedium,
                            color = KiNDDMatchedGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                center?.let {
                    Text(
                        text = it.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = it.shortName,
                        style = MaterialTheme.typography.labelLarge,
                        color = KiNDDIndigo
                    )
                }
                Text(
                    text = stringResource(R.string.onboarding_center_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            CenterLookupState.UNMATCHED -> {
                StatusCopy(
                    title = stringResource(R.string.onboarding_center_unmatched_title),
                    body = stringResource(R.string.onboarding_center_unmatched_body)
                )
            }
            CenterLookupState.UNAVAILABLE -> {
                StatusCopy(
                    title = stringResource(R.string.onboarding_center_unavailable_title),
                    body = stringResource(R.string.onboarding_center_unavailable_body)
                )
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("onboarding_retry_center")
                ) {
                    Text(stringResource(R.string.action_retry))
                }
            }
            CenterLookupState.LOADING -> Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                Text(stringResource(R.string.loading))
            }
            CenterLookupState.IDLE -> {
                Text(
                    text = stringResource(R.string.onboarding_center_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() }
                )
            }
        }
    }
}

@Composable
private fun StatusCopy(title: String, body: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.semantics { heading() }
    )
    Text(
        text = body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
