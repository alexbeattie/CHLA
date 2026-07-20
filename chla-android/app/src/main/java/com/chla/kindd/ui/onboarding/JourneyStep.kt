package com.chla.kindd.ui.onboarding

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import com.chla.kindd.R
import com.chla.kindd.data.profile.JourneyStage

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
            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                role = Role.RadioButton
            )
        }
    }
}
