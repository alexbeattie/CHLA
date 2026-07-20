package com.chla.kindd.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chla.kindd.R
import com.chla.kindd.data.discovery.DiscoveryState
import com.chla.kindd.data.models.Provider
import com.chla.kindd.ui.discovery.ActiveFilterChips
import com.chla.kindd.ui.discovery.DiscoveryFilterSheet
import com.chla.kindd.ui.discovery.DiscoverySearchField
import com.chla.kindd.ui.discovery.DiscoveryStateContent
import com.chla.kindd.ui.discovery.DiscoveryUiActions
import com.chla.kindd.ui.theme.CHLABlue
import com.chla.kindd.ui.theme.CHLABlueLight

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

@OptIn(ExperimentalMaterial3Api::class)
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.list_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CHLABlue,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            DiscoverySearchField(
                query = state.criteria.query,
                onQueryChange = actions.onQueryChange,
                onFilterClick = { showFilters = true },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
            ActiveFilterChips(
                criteria = state.criteria,
                onRemoveTherapy = actions.onRemoveTherapy,
                onRemoveAge = actions.onRemoveAge,
                onRemoveDiagnosis = actions.onRemoveDiagnosis,
                onRemoveInsurance = actions.onRemoveInsurance,
                onRemoveRadius = actions.onRemoveRadius,
                onClearAll = actions.onClearAll,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = sort == ProviderListSort.NAME,
                    onClick = { onSortChange(ProviderListSort.NAME) },
                    label = { Text(stringResource(R.string.discovery_sort_name)) }
                )
                FilterChip(
                    selected = sort == ProviderListSort.DISTANCE,
                    onClick = { onSortChange(ProviderListSort.DISTANCE) },
                    label = { Text(stringResource(R.string.discovery_sort_distance)) }
                )
            }
            DiscoveryStateContent(
                state = state,
                onRetry = actions.onRetry,
                modifier = Modifier.weight(1f)
            ) {
                ProviderCards(
                    providers = providers,
                    onProviderClick = onProviderClick
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
private fun ProviderCards(
    providers: List<Provider>,
    onProviderClick: (String) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(providers, key = Provider::id) { provider ->
            ProviderCard(
                provider = provider,
                onClick = { onProviderClick(provider.id) }
            )
        }
    }
}

@Composable
private fun ProviderCard(provider: Provider, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("provider_${provider.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = provider.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (provider.distance != null) {
                    Text(
                        text = provider.formattedDistance,
                        style = MaterialTheme.typography.bodySmall,
                        color = CHLABlueLight
                    )
                }
            }
            if (!provider.therapyTypes.isNullOrEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    provider.therapyTypes.take(3).forEachIndexed { index, therapy ->
                        Surface(
                            modifier = Modifier.testTag(
                                "provider_therapy_${provider.id}_$index"
                            ),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = therapy,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
            if (provider.fullAddress.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = provider.fullAddress,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
