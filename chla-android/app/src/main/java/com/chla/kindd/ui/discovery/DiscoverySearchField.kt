package com.chla.kindd.ui.discovery

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.chla.kindd.R
import com.chla.kindd.data.discovery.TherapyType

data class DiscoveryUiActions(
    val onQueryChange: (String) -> Unit,
    val onApplyFilters: (DiscoveryFilterSelection) -> Unit,
    val onRemoveTherapy: (TherapyType) -> Unit,
    val onRemoveAge: () -> Unit,
    val onRemoveDiagnosis: () -> Unit,
    val onRemoveInsurance: () -> Unit,
    val onRemoveRadius: () -> Unit,
    val onClearAll: () -> Unit,
    val onRetry: () -> Unit
)

@Composable
fun DiscoverySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.testTag("discovery_search_field"),
        singleLine = true,
        label = { Text(stringResource(R.string.discovery_search_label)) },
        placeholder = { Text(stringResource(R.string.search_placeholder)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null
            )
        },
        trailingIcon = {
            Row {
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.testTag("discovery_clear_query")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.discovery_clear_search)
                        )
                    }
                }
                IconButton(
                    onClick = onFilterClick,
                    modifier = Modifier.testTag("discovery_filter_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = stringResource(R.string.filters)
                    )
                }
            }
        }
    )
}
