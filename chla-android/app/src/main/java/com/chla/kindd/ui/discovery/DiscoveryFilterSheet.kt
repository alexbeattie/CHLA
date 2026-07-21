package com.chla.kindd.ui.discovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.chla.kindd.R
import com.chla.kindd.data.discovery.DiscoveryCatalog
import com.chla.kindd.data.discovery.DiscoveryCriteria
import com.chla.kindd.data.discovery.DiscoveryOrigin
import com.chla.kindd.data.discovery.TherapyType
import com.chla.kindd.data.profile.AgeGroup
import com.chla.kindd.ui.theme.KiNDDShapeTokens

data class DiscoveryFilterSelection(
    val therapyTypes: Set<TherapyType>,
    val ageGroup: AgeGroup?,
    val diagnosis: String?,
    val insurance: String?,
    val radiusMiles: Int
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DiscoveryFilterSheet(
    criteria: DiscoveryCriteria,
    onDismissRequest: () -> Unit,
    onApply: (DiscoveryFilterSelection) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        shape = RoundedCornerShape(
            topStart = KiNDDShapeTokens.Sheet,
            topEnd = KiNDDShapeTokens.Sheet
        )
    ) {
        DiscoveryFilterContent(
            criteria = criteria,
            onDismissRequest = onDismissRequest,
            onApply = onApply
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscoveryFilterContent(
    criteria: DiscoveryCriteria,
    onDismissRequest: () -> Unit,
    onApply: (DiscoveryFilterSelection) -> Unit
) {
    var therapies by remember(criteria) { mutableStateOf(criteria.therapyTypes) }
    var age by remember(criteria) { mutableStateOf(criteria.ageGroup) }
    var diagnosis by remember(criteria) { mutableStateOf(criteria.diagnosis) }
    var insurance by remember(criteria) { mutableStateOf(criteria.insurance) }
    var radius by remember(criteria) { mutableIntStateOf(criteria.radiusMiles) }

    Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.filters),
                    modifier = Modifier
                        .testTag("discovery_filter_title")
                        .semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall
                )
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.testTag("discovery_filter_dismiss")
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.close)
                    )
                }
            }

            FilterSection(stringResource(R.string.therapy_types)) {
                DiscoveryCatalog.therapyTypes.forEach { therapy ->
                    FilterChip(
                        selected = therapy in therapies,
                        onClick = {
                            therapies = if (therapy in therapies) {
                                therapies - therapy
                            } else {
                                therapies + therapy
                            }
                        },
                        label = { Text(stringResource(therapy.displayResId)) }
                    )
                }
            }
            FilterSection(stringResource(R.string.discovery_age)) {
                NullableFilterChip(
                    selected = age == null,
                    label = stringResource(R.string.discovery_any_age),
                    onClick = { age = null }
                )
                DiscoveryCatalog.ageGroups.forEach { option ->
                    NullableFilterChip(
                        selected = age == option,
                        label = ageGroupLabel(option),
                        onClick = { age = option }
                    )
                }
            }
            FilterSection(stringResource(R.string.discovery_diagnosis)) {
                NullableFilterChip(
                    selected = diagnosis == null,
                    label = stringResource(R.string.discovery_any_diagnosis),
                    onClick = { diagnosis = null }
                )
                DiscoveryCatalog.diagnoses.forEach { option ->
                    NullableFilterChip(
                        selected = diagnosis == option,
                        label = diagnosisLabel(option),
                        onClick = { diagnosis = option }
                    )
                }
            }
            FilterSection(stringResource(R.string.discovery_insurance)) {
                NullableFilterChip(
                    selected = insurance == null,
                    label = stringResource(R.string.discovery_any_insurance),
                    onClick = { insurance = null }
                )
                DiscoveryCatalog.insurances.forEach { option ->
                    NullableFilterChip(
                        selected = insurance == option,
                        label = insuranceLabel(option),
                        onClick = { insurance = option }
                    )
                }
            }
            if (criteria.origin is DiscoveryOrigin.DeviceLocation) {
                FilterSection(stringResource(R.string.discovery_radius)) {
                    listOf(5, 10, 15, 25, 50).forEach { option ->
                        NullableFilterChip(
                            selected = radius == option,
                            label = pluralStringResource(
                                R.plurals.discovery_radius_option,
                                option,
                                option
                            ),
                            onClick = { radius = option }
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        therapies = emptySet()
                        age = null
                        diagnosis = null
                        insurance = null
                        radius = 15
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("discovery_filter_reset")
                ) {
                    Text(stringResource(R.string.discovery_reset))
                }
                Button(
                    onClick = {
                        onApply(
                            DiscoveryFilterSelection(
                                therapyTypes = therapies,
                                ageGroup = age,
                                diagnosis = diagnosis,
                                insurance = insurance,
                                radiusMiles = radius
                            )
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("discovery_filter_apply")
                ) {
                    Text(stringResource(R.string.discovery_apply))
                }
            }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterSection(
    title: String,
    content: @Composable androidx.compose.foundation.layout.FlowRowScope.() -> Unit
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .padding(top = 12.dp, bottom = 4.dp)
            .semantics { heading() }
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content
    )
}

@Composable
private fun NullableFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}
