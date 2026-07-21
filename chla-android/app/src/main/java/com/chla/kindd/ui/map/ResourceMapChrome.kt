package com.chla.kindd.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chla.kindd.R
import com.chla.kindd.data.discovery.DiscoveryCriteria
import com.chla.kindd.data.discovery.DiscoveryOrigin
import com.chla.kindd.data.discovery.DiscoveryState
import com.chla.kindd.ui.discovery.ActiveFilterChips
import com.chla.kindd.ui.discovery.DiscoveryUiActions
import com.chla.kindd.ui.discovery.KiNDDSearchOverlay
import com.chla.kindd.ui.discovery.discoveryErrorText
import com.chla.kindd.ui.screens.MapLocationState
import com.chla.kindd.ui.screens.MapLocationStatus
import com.chla.kindd.ui.theme.KiNDDGlassSurface
import com.chla.kindd.ui.theme.KiNDDIndigo
import com.chla.kindd.ui.theme.KiNDDPurple
import com.chla.kindd.ui.theme.KiNDDShapeTokens
import com.chla.kindd.ui.theme.KiNDDSpacingTokens

internal fun DiscoveryCriteria.activeMapFilterCount(): Int =
    therapyTypes.size +
        (if (ageGroup != null) 1 else 0) +
        (if (diagnosis != null) 1 else 0) +
        (if (insurance != null) 1 else 0) +
        (if (origin is DiscoveryOrigin.DeviceLocation) 1 else 0)

@Composable
internal fun ResourceMapSearchChrome(
    criteria: DiscoveryCriteria,
    actions: DiscoveryUiActions,
    onShowFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeFilterCount = criteria.activeMapFilterCount()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(
                start = KiNDDSpacingTokens.PageInset,
                top = 8.dp,
                end = KiNDDSpacingTokens.PageInset
            )
            .testTag("map_search_chrome"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.weight(1f)) {
                KiNDDSearchOverlay(
                    query = criteria.query,
                    onQueryChange = actions.onQueryChange
                )
            }
            Box {
                KiNDDGlassSurface(
                    modifier = Modifier.requiredSize(48.dp),
                    shape = CircleShape
                ) {
                    IconButton(
                        onClick = onShowFilters,
                        modifier = Modifier
                            .requiredSize(48.dp)
                            .testTag("map_top_filter")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = stringResource(R.string.filters),
                            tint = if (activeFilterCount > 0) {
                                KiNDDIndigo
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                if (activeFilterCount > 0) {
                    FilterCountBadge(
                        count = activeFilterCount,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .testTag("map_filter_badge")
                    )
                }
            }
        }

        if (activeFilterCount > 0) {
            KiNDDGlassSurface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(KiNDDShapeTokens.Compact),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                ActiveFilterChips(
                    criteria = criteria,
                    onRemoveTherapy = actions.onRemoveTherapy,
                    onRemoveAge = actions.onRemoveAge,
                    onRemoveDiagnosis = actions.onRemoveDiagnosis,
                    onRemoveInsurance = actions.onRemoveInsurance,
                    onRemoveRadius = actions.onRemoveRadius,
                    onClearAll = actions.onClearAll,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("map_active_filter_row")
                )
            }
        }
    }
}

@Composable
internal fun ResourceMapControlRail(
    activeFilterCount: Int,
    locationState: MapLocationState,
    isRefreshing: Boolean,
    onShowFilters: () -> Unit,
    onUseMyLocation: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    KiNDDGlassSurface(
        modifier = modifier.testTag("map_control_rail"),
        shape = RoundedCornerShape(KiNDDShapeTokens.Compact)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box {
                ResourceMapControlButton(
                    icon = Icons.Default.FilterList,
                    contentDescription = stringResource(R.string.filters),
                    tag = "map_rail_filter",
                    tint = if (activeFilterCount > 0) KiNDDIndigo else null,
                    onClick = onShowFilters
                )
                if (activeFilterCount > 0) {
                    FilterCountBadge(
                        count = activeFilterCount,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 2.dp, y = (-2).dp)
                            .testTag("map_rail_filter_badge")
                    )
                }
            }
            RailDivider()
            ResourceMapControlButton(
                icon = Icons.Default.MyLocation,
                contentDescription = stringResource(R.string.discovery_use_my_location),
                tag = "map_use_location",
                enabled = locationState.status != MapLocationStatus.LOCATING,
                tint = if (locationState.hasPermission) KiNDDIndigo else null,
                progress = locationState.status == MapLocationStatus.LOCATING,
                onClick = onUseMyLocation
            )
            RailDivider()
            ResourceMapControlButton(
                icon = Icons.Default.Refresh,
                contentDescription = stringResource(R.string.map_refresh),
                tag = "map_refresh",
                enabled = !isRefreshing,
                progress = isRefreshing,
                onClick = onRefresh
            )
        }
    }
}

@Composable
private fun ResourceMapControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    tag: String,
    enabled: Boolean = true,
    tint: Color? = null,
    progress: Boolean = false,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .requiredSize(48.dp)
            .testTag(tag)
            .semantics {
                this.contentDescription = contentDescription
                if (progress) liveRegion = LiveRegionMode.Polite
            }
    ) {
        if (progress) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = KiNDDIndigo
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint ?: MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RailDivider() {
    HorizontalDivider(
        modifier = Modifier.width(32.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
    )
}

@Composable
private fun FilterCountBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(20.dp)
            .background(KiNDDIndigo, CircleShape)
            .semantics(mergeDescendants = true) {},
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = count.toString(),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
internal fun ResourceMapContextBadges(
    state: DiscoveryState,
    onNavigateToList: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mappedProviderCount = state.mapProviders.size
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                start = KiNDDSpacingTokens.PageInset,
                end = KiNDDSpacingTokens.PageInset,
                bottom = KiNDDSpacingTokens.FloatingNavigationContentClearance + 12.dp
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        state.profile.regionalCenter?.shortName?.let { shortName ->
            ResourceMapBadge(
                text = shortName,
                color = KiNDDPurple,
                tag = "map_regional_center_badge"
            )
        }
        if (state.profile.regionalCenter == null) {
            Box(Modifier)
        }
        if (state.providers.isNotEmpty()) {
            ResourceMapBadge(
                text = stringResource(
                    if (mappedProviderCount == 1) {
                        R.string.resource_found
                    } else {
                        R.string.resources_found
                    },
                    mappedProviderCount
                ),
                color = KiNDDIndigo,
                tag = "map_result_count",
                contentDescription = pluralStringResource(
                    R.plurals.map_result_count_action,
                    mappedProviderCount,
                    mappedProviderCount
                ),
                onClick = onNavigateToList
            )
        }
    }
}

@Composable
private fun ResourceMapBadge(
    text: String,
    color: Color,
    tag: String,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null
) {
    val actionModifier = if (onClick == null) {
        Modifier
    } else {
        Modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick)
    }
    KiNDDGlassSurface(
        modifier = Modifier
            .testTag(tag)
            .then(actionModifier)
            .semantics(mergeDescendants = true) {
                contentDescription?.let { this.contentDescription = it }
            },
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Apartment,
                contentDescription = null,
                modifier = Modifier.size(15.dp),
                tint = color
            )
            Text(
                text = text,
                color = color,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
internal fun ResourceMapRetainedContextOverlays(
    state: DiscoveryState,
    locationState: MapLocationState,
    onRetry: () -> Unit,
    onShowFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .widthIn(max = 360.dp)
            .padding(horizontal = KiNDDSpacingTokens.PageInset),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        when (locationState.status) {
            MapLocationStatus.PERMISSION_DENIED,
            MapLocationStatus.FAILED -> {
                RetainedStatusSurface(
                    tag = "map_location_status",
                    text = stringResource(
                        if (locationState.status == MapLocationStatus.PERMISSION_DENIED) {
                            R.string.discovery_location_denied
                        } else {
                            R.string.discovery_location_failed
                        }
                    ),
                    color = MaterialTheme.colorScheme.error
                )
            }
            MapLocationStatus.LOCATING,
            MapLocationStatus.IDLE -> Unit
        }

        when {
            state.isLoading && state.providers.isEmpty() -> {
                KiNDDGlassSurface(
                    modifier = Modifier
                        .testTag("map_loading_overlay")
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    shape = RoundedCornerShape(KiNDDShapeTokens.Card),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        Text(stringResource(R.string.loading))
                    }
                }
            }
            state.error != null -> {
                KiNDDGlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("map_error_overlay")
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    shape = RoundedCornerShape(KiNDDShapeTokens.Card),
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = discoveryErrorText(state.error),
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.error
                        )
                        TextButton(
                            onClick = onRetry,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }
            state.isLoading -> {
                KiNDDGlassSurface(
                    modifier = Modifier
                        .testTag("map_refresh_progress")
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text(stringResource(R.string.loading))
                    }
                }
            }
            state.hasLoadedOnce && state.providers.isEmpty() -> {
                KiNDDGlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("discovery_empty")
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    shape = RoundedCornerShape(KiNDDShapeTokens.Card),
                    contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.no_resources_found),
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = onShowFilters,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(stringResource(R.string.filters))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RetainedStatusSurface(tag: String, text: String, color: Color) {
    KiNDDGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag)
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(KiNDDShapeTokens.Compact),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(text = text, color = color, style = MaterialTheme.typography.bodySmall)
    }
}
