package com.chla.kindd.ui.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import com.chla.kindd.R
import com.chla.kindd.data.profile.AgeGroup

@Composable
internal fun AgeGroupStep(
    selectedAgeGroups: List<AgeGroup>,
    hasMultipleChildren: Boolean,
    onAgeSelected: (AgeGroup) -> Unit,
    onMultipleChildrenToggled: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selected = selectedAgeGroups.filter { it in AgeGroup.childAges }
    val multiple = hasMultipleChildren || selected.size > 1
    OnboardingStepColumn(modifier = modifier) {
        OnboardingHeading(
            stringResource(
                if (multiple) {
                    R.string.onboarding_age_title_plural
                } else {
                    R.string.onboarding_age_title
                }
            )
        )
        Text(
            text = stringResource(R.string.onboarding_age_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        AgeGroup.childAges.forEach { ageGroup ->
            OnboardingChoice(
                label = ageGroup.displayLabel(),
                selected = ageGroup in selected,
                onClick = { onAgeSelected(ageGroup) },
                testTag = when (ageGroup) {
                    AgeGroup.EARLY_INTERVENTION -> "onboarding_age_early_intervention"
                    AgeGroup.SCHOOL_AGE -> "onboarding_age_school_age"
                    AgeGroup.ADOLESCENT -> "onboarding_age_adolescent"
                    AgeGroup.ADULT -> "onboarding_age_adult"
                    AgeGroup.ALL_AGES -> "onboarding_age_all_ages"
                },
                role = Role.Checkbox,
                icon = when (ageGroup) {
                    AgeGroup.EARLY_INTERVENTION -> Icons.Default.ChildCare
                    AgeGroup.SCHOOL_AGE -> Icons.Default.School
                    AgeGroup.ADOLESCENT -> Icons.Default.Groups
                    AgeGroup.ADULT -> Icons.Default.Person
                    AgeGroup.ALL_AGES -> Icons.Default.Groups
                }
            )
        }
        OnboardingChoice(
            label = stringResource(R.string.onboarding_age_multiple_children),
            selected = multiple,
            onClick = onMultipleChildrenToggled,
            testTag = "onboarding_age_multiple_children",
            role = Role.Checkbox,
            icon = Icons.Default.Groups
        )
    }
}

@Composable
private fun AgeGroup.displayLabel(): String = when (this) {
    AgeGroup.EARLY_INTERVENTION -> stringResource(R.string.onboarding_age_early_intervention)
    AgeGroup.SCHOOL_AGE -> stringResource(R.string.onboarding_age_school_age)
    AgeGroup.ADOLESCENT -> stringResource(R.string.onboarding_age_adolescent)
    AgeGroup.ADULT -> stringResource(R.string.onboarding_age_adult)
    AgeGroup.ALL_AGES -> stringResource(R.string.onboarding_age_all_ages)
}
