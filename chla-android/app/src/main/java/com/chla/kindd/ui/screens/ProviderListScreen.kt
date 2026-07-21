package com.chla.kindd.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chla.kindd.R
import com.chla.kindd.data.discovery.DiscoveryCriteria
import com.chla.kindd.data.discovery.DiscoveryOrigin
import com.chla.kindd.data.discovery.DiscoveryState
import com.chla.kindd.data.models.Provider
import com.chla.kindd.ui.discovery.ActiveFilterChips
import com.chla.kindd.ui.discovery.DiscoveryFilterSheet
import com.chla.kindd.ui.discovery.DiscoveryStateContent
import com.chla.kindd.ui.discovery.DiscoveryUiActions
import com.chla.kindd.ui.discovery.KiNDDSearchOverlay
import com.chla.kindd.ui.providers.ProviderCard
import com.chla.kindd.ui.theme.KiNDDCompactIconAction
import com.chla.kindd.ui.theme.KiNDDIndigo
import com.chla.kindd.ui.theme.KiNDDSpacingTokens
import com.chla.kindd.ui.theme.kinddTopWash

@Composable
fun ProviderListScreen(
    onProviderClick: (String) -> Unit,
    viewModel: ProviderListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.onFirstAppearance() }

    ProviderListContent(
        state = state,
        providers = viewModel.sortedProviders(state.providers),
        sort = sort,
        onSortChange = viewModel::setSort,
        actions = DiscoveryUiActions(
            onQueryChange = viewModel::setQuery,
            onApplyFilters = { selection ->
                viewModel.applyFilters(
                    selection.therapyTypes,
                    selection.ageGroup,
                    selection.diagnosis,
                    selection.insurance,
                    selection.radiusMiles
                )
            },
            onRemoveTherapy = viewModel::removeTherapy,
            onRemoveAge = viewModel::removeAge,
            onRemoveDiagnosis = viewModel::removeDiagnosis,
            onRemoveInsurance = viewModel::removeInsurance,
            onRemoveRadius = viewModel::removeRadius,
            onClearAll = viewModel::clearAllFilters,
            onRetry = viewModel::retry
        ),
        onProviderClick = onProviderClick
    )
}

@Composable
fun ProviderListContent(
    state: DiscoveryState,
    providers: List<Provider>,
    sort: ProviderListSort,
    onSortChange: (ProviderListSort) -> Unit,
    actions: DiscoveryUiActions,
    onProviderClick: (String) -> Unit
) {
    var showFilters by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp)
                .background(kinddTopWash())
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            ProviderListHeader(
                criteria = state.criteria,
                sort = sort,
                onSortChange = onSortChange,
                onFilterClick = { showFilters = true }
            )
            KiNDDSearchOverlay(
                query = state.criteria.query,
                onQueryChange = actions.onQueryChange,
                modifier = Modifier.padding(
                    horizontal = KiNDDSpacingTokens.PageInset,
                    vertical = 8.dp
                )
            )
            ActiveFilterChips(
                criteria = state.criteria,
                onRemoveTherapy = actions.onRemoveTherapy,
                onRemoveAge = actions.onRemoveAge,
                onRemoveDiagnosis = actions.onRemoveDiagnosis,
                onRemoveInsurance = actions.onRemoveInsurance,
                onRemoveRadius = actions.onRemoveRadius,
                onClearAll = actions.onClearAll,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KiNDDSpacingTokens.PageInset)
            )
            DiscoveryStateContent(
                state = state,
                onRetry = actions.onRetry,
                modifier = Modifier.weight(1f),
                emptyContent = { ProviderListEmptyState(onRefresh = actions.onRetry) }
            ) {
                ProviderCards(
                    providers = providers,
                    onProviderClick = onProviderClick,
                    onPhoneClick = { provider ->
                        provider.phone?.takeIf(String::isNotBlank)?.let { phone ->
                            runCatching {
                                uriHandler.openUri("tel:${Uri.encode(phone)}")
                            }
                        }
                    }
                )
            }
        }
    }

    if (showFilters) {
        DiscoveryFilterSheet(
            criteria = state.criteria,
            onDismissRequest = { showFilters = false },
            onApply = {
                actions.onApplyFilters(it)
                showFilters = false
            }
        )
    }
}

@Composable
private fun ProviderListEmptyState(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Apartment,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(64.dp)
                .testTag("provider_list_empty_icon")
        )
        Text(
            text = stringResource(R.string.list_no_resources_found),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = stringResource(R.string.list_no_resources_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onRefresh,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("provider_list_empty_refresh")
        ) {
            Text(stringResource(R.string.list_refresh))
        }
    }
}

@Composable
private fun ProviderListHeader(
    criteria: DiscoveryCriteria,
    sort: ProviderListSort,
    onSortChange: (ProviderListSort) -> Unit,
    onFilterClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = KiNDDSpacingTokens.PageInset,
                end = KiNDDSpacingTokens.PageInset,
                top = 10.dp,
                bottom = 2.dp
            )
            .testTag("list_compact_header"),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.list_title),
            modifier = Modifier
                .weight(1f)
                .testTag("list_title")
                .semantics { heading() },
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        KiNDDCompactIconAction(
            icon = Icons.Default.SwapVert,
            contentDescription = stringResource(R.string.discovery_sort_action),
            onClick = {
                onSortChange(
                    if (sort == ProviderListSort.NAME) {
                        ProviderListSort.DISTANCE
                    } else {
                        ProviderListSort.NAME
                    }
                )
            },
            tint = KiNDDIndigo,
            modifier = Modifier.testTag("list_sort_button")
        )
        Box {
            KiNDDCompactIconAction(
                icon = Icons.Default.FilterList,
                contentDescription = stringResource(R.string.filters),
                onClick = onFilterClick,
                tint = KiNDDIndigo,
                modifier = Modifier.testTag("list_filter_button")
            )
            if (criteria.activeFilterCount() > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 7.dp, end = 7.dp)
                        .size(7.dp)
                        .background(KiNDDIndigo, androidx.compose.foundation.shape.CircleShape)
                )
            }
        }
    }
}

@Composable
private fun ProviderCards(
    providers: List<Provider>,
    onProviderClick: (String) -> Unit,
    onPhoneClick: (Provider) -> Unit
) {
    LazyColumn(
        modifier = Modifier.testTag("provider_list"),
        contentPadding = PaddingValues(
            start = KiNDDSpacingTokens.PageInset,
            top = 4.dp,
            end = KiNDDSpacingTokens.PageInset
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(providers, key = Provider::id) { provider ->
            ProviderCard(
                provider = provider,
                onClick = { onProviderClick(provider.id) },
                onPhoneClick = provider.phone?.takeIf(String::isNotBlank)?.let {
                    { onPhoneClick(provider) }
                }
            )
        }
        item(key = "bottom_clearance") {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(KiNDDSpacingTokens.Section)
                    .testTag("provider_list_bottom_clearance")
            )
        }
    }
}

private fun DiscoveryCriteria.activeFilterCount(): Int =
    therapyTypes.size +
        (if (ageGroup != null) 1 else 0) +
        (if (diagnosis != null) 1 else 0) +
        (if (insurance != null) 1 else 0) +
        (if (origin is DiscoveryOrigin.DeviceLocation) 1 else 0)
