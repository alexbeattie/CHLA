package com.chla.kindd.ui.discovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.chla.kindd.R
import com.chla.kindd.data.discovery.DiscoveryCriteria
import com.chla.kindd.data.discovery.DiscoveryOrigin
import com.chla.kindd.data.discovery.TherapyType
import com.chla.kindd.data.profile.AgeGroup
import com.chla.kindd.ui.theme.KiNDDIndigo
import com.chla.kindd.ui.theme.KiNDDPink
import com.chla.kindd.ui.theme.KiNDDPurple
import com.chla.kindd.ui.theme.KiNDDViolet

@Composable
fun ActiveFilterChips(
    criteria: DiscoveryCriteria,
    onRemoveTherapy: (TherapyType) -> Unit,
    onRemoveAge: () -> Unit,
    onRemoveDiagnosis: () -> Unit,
    onRemoveInsurance: () -> Unit,
    onRemoveRadius: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasRadius = criteria.origin is DiscoveryOrigin.DeviceLocation
    val hasFilters = criteria.therapyTypes.isNotEmpty() ||
        criteria.ageGroup != null ||
        criteria.diagnosis != null ||
        criteria.insurance != null ||
        hasRadius
    if (!hasFilters) return

    val chips = buildList {
        criteria.therapyTypes.sortedBy(TherapyType::ordinal).forEach { therapy ->
            add(
                ActiveFilterChipModel(
                    key = "therapy_${therapy.name}",
                    label = stringResource(therapy.displayResId),
                    tag = "filter_chip_therapy_${therapy.name}",
                    color = activeTherapyColor(therapy),
                    onRemove = { onRemoveTherapy(therapy) }
                )
            )
        }
        criteria.ageGroup?.let {
            add(
                ActiveFilterChipModel(
                    key = "age",
                    label = stringResource(R.string.discovery_age_chip, ageGroupLabel(it)),
                    tag = "filter_chip_age",
                    onRemove = onRemoveAge
                )
            )
        }
        criteria.diagnosis?.let {
            add(
                ActiveFilterChipModel(
                    key = "diagnosis",
                    label = stringResource(
                        R.string.discovery_diagnosis_chip,
                        diagnosisLabel(it)
                    ),
                    tag = "filter_chip_diagnosis",
                    onRemove = onRemoveDiagnosis
                )
            )
        }
        criteria.insurance?.let {
            add(
                ActiveFilterChipModel(
                    key = "insurance",
                    label = stringResource(
                        R.string.discovery_insurance_chip,
                        insuranceLabel(it)
                    ),
                    tag = "filter_chip_insurance",
                    onRemove = onRemoveInsurance
                )
            )
        }
        if (hasRadius) {
            add(
                ActiveFilterChipModel(
                    key = "radius",
                    label = pluralStringResource(
                        R.plurals.discovery_radius_chip,
                        criteria.radiusMiles,
                        criteria.radiusMiles
                    ),
                    tag = "filter_chip_radius",
                    onRemove = onRemoveRadius
                )
            )
        }
    }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(chips, key = ActiveFilterChipModel::key) { chip ->
            RemovableFilterChip(
                label = chip.label,
                tag = chip.tag,
                color = chip.color,
                onRemove = chip.onRemove
            )
        }
        item(key = "clear_all") {
            AssistChip(
                onClick = onClearAll,
                label = { Text(stringResource(R.string.discovery_clear_all)) },
                modifier = Modifier.testTag("discovery_clear_all")
            )
        }
    }
}

private data class ActiveFilterChipModel(
    val key: String,
    val label: String,
    val tag: String,
    val color: Color? = null,
    val onRemove: () -> Unit
)

@Composable
private fun RemovableFilterChip(
    label: String,
    tag: String,
    color: Color? = null,
    onRemove: () -> Unit
) {
    val removeDescription = stringResource(R.string.discovery_remove_filter, label)
    AssistChip(
        onClick = onRemove,
        label = { Text(label) },
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null
            )
        },
        modifier = Modifier
            .testTag(tag)
            .semantics { contentDescription = removeDescription },
        colors = if (color == null) {
            AssistChipDefaults.assistChipColors()
        } else {
            AssistChipDefaults.assistChipColors(
                containerColor = color.copy(alpha = 0.12f),
                labelColor = color
            )
        }
    )
}

private fun activeTherapyColor(therapy: TherapyType): Color = when (therapy) {
    TherapyType.ABA -> KiNDDIndigo
    TherapyType.SPEECH -> KiNDDPink
    TherapyType.OCCUPATIONAL -> KiNDDViolet
    TherapyType.PHYSICAL -> KiNDDPurple
    else -> Color(0xFF667085)
}

@Composable
internal fun ageGroupLabel(ageGroup: AgeGroup): String = stringResource(
    when (ageGroup) {
        AgeGroup.EARLY_INTERVENTION -> R.string.onboarding_age_early_intervention
        AgeGroup.SCHOOL_AGE -> R.string.onboarding_age_school_age
        AgeGroup.ADOLESCENT -> R.string.onboarding_age_adolescent
        AgeGroup.ADULT -> R.string.onboarding_age_adult
        AgeGroup.ALL_AGES -> R.string.onboarding_age_all_ages
    }
)

@Composable
internal fun diagnosisLabel(value: String): String = stringResource(
    when (value) {
        "Autism Spectrum Disorder" -> R.string.discovery_diagnosis_autism
        "Global Development Delay" -> R.string.discovery_diagnosis_global_delay
        "Intellectual Disability" -> R.string.discovery_diagnosis_intellectual
        "Speech and Language Disorder" -> R.string.discovery_diagnosis_speech_language
        "Other" -> R.string.discovery_other
        else -> R.string.discovery_unknown_filter
    }
)

@Composable
internal fun insuranceLabel(value: String): String = stringResource(
    when (value) {
        "Regional Center" -> R.string.discovery_insurance_regional_center
        "Private Pay" -> R.string.discovery_insurance_private_pay
        "Medi-Cal" -> R.string.discovery_insurance_medi_cal
        "Medicare" -> R.string.discovery_insurance_medicare
        "Blue Cross" -> R.string.discovery_insurance_blue_cross
        "Blue Shield" -> R.string.discovery_insurance_blue_shield
        "Anthem" -> R.string.discovery_insurance_anthem
        "Aetna" -> R.string.discovery_insurance_aetna
        "Cigna" -> R.string.discovery_insurance_cigna
        "Kaiser Permanente" -> R.string.discovery_insurance_kaiser
        "United Healthcare" -> R.string.discovery_insurance_united
        "Health Net" -> R.string.discovery_insurance_health_net
        "Molina" -> R.string.discovery_insurance_molina
        "L.A. Care" -> R.string.discovery_insurance_la_care
        "Covered California" -> R.string.discovery_insurance_covered_california
        else -> R.string.discovery_unknown_filter
    }
)
