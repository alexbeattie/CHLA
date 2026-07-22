package com.navigator.kindd.ui.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.navigator.kindd.R
import com.navigator.kindd.data.profile.JourneyStage

@Composable
internal fun JourneyStep(
    selectedJourney: JourneyStage?,
    onJourneySelected: (JourneyStage) -> Unit,
    modifier: Modifier = Modifier
) {
    OnboardingStepColumn(modifier = modifier) {
        OnboardingHeading(stringResource(R.string.onboarding_journey_title))
        Text(
            text = stringResource(R.string.onboarding_journey_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        JourneyStage.entries.forEach { journey ->
            val label = when (journey) {
                JourneyStage.JUST_DIAGNOSED ->
                    stringResource(R.string.onboarding_journey_just_diagnosed)
                JourneyStage.WAITING_FOR_INTAKE ->
                    stringResource(R.string.onboarding_journey_waiting_intake)
                JourneyStage.RECEIVING_SERVICES ->
                    stringResource(R.string.onboarding_journey_receiving_services)
                JourneyStage.EXPLORING ->
                    stringResource(R.string.onboarding_journey_exploring)
            }
            OnboardingChoice(
                label = label,
                selected = selectedJourney == journey,
                onClick = { onJourneySelected(journey) },
                testTag = "onboarding_journey_${journey.storageValue}",
                role = Role.RadioButton,
                icon = when (journey) {
                    JourneyStage.JUST_DIAGNOSED -> Icons.Default.MedicalServices
                    JourneyStage.WAITING_FOR_INTAKE -> Icons.Default.Schedule
                    JourneyStage.RECEIVING_SERVICES -> Icons.Default.Favorite
                    JourneyStage.EXPLORING -> Icons.Default.Explore
                }
            )
        }
    }
}
