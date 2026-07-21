package com.chla.kindd.ui.regions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chla.kindd.R
import com.chla.kindd.ui.map.RegionalCenterMapRenderModel
import com.chla.kindd.ui.map.RegionalCenterMapSurface
import com.chla.kindd.ui.theme.KiNDDIndigo
import com.chla.kindd.ui.theme.KiNDDViolet

@Composable
internal fun RegionalCenterServiceAreaMap(
    state: RegionalCenterServiceAreaState,
    highlightedAcronym: String?,
    interactive: Boolean,
    onAreaClick: (String) -> Unit,
    contentPadding: PaddingValues = PaddingValues(),
    modifier: Modifier = Modifier,
    mapContent: (@Composable (RegionalCenterMapRenderModel, (String) -> Unit) -> Unit)? = null
) {
    when (state) {
        is RegionalCenterServiceAreaState.Success -> Box(
            modifier = modifier.testTag("regions_boundaries_${state.areas.size}")
        ) {
            RegionalCenterMapSurface(
                areas = state.areas,
                highlightedAcronym = highlightedAcronym,
                interactive = interactive,
                onAreaClick = onAreaClick,
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize(),
                mapContent = mapContent
            )
        }
        RegionalCenterServiceAreaState.Loading -> ServiceAreaFallback(
            isLoading = true,
            modifier = modifier.testTag("regional_center_service_areas_loading")
        )
        RegionalCenterServiceAreaState.Error -> ServiceAreaFallback(
            isLoading = false,
            modifier = modifier.testTag("regional_center_service_areas_error")
        )
    }
}

@Composable
private fun ServiceAreaFallback(isLoading: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    listOf(
                        KiNDDIndigo.copy(alpha = 0.22f),
                        KiNDDViolet.copy(alpha = 0.15f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                    )
                )
            )
            .semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
            } else {
                Icon(
                    imageVector = Icons.Outlined.Map,
                    contentDescription = null,
                    tint = KiNDDIndigo,
                    modifier = Modifier.size(36.dp)
                )
            }
            Text(
                text = stringResource(
                    if (isLoading) R.string.regional_center_service_areas_loading
                    else R.string.regional_center_service_areas_error
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
