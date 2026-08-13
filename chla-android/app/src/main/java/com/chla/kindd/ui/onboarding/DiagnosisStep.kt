package com.chla.kindd.ui.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import com.chla.kindd.R
import com.chla.kindd.data.profile.Diagnosis

@Composable
internal fun DiagnosisStep(
    selectedDiagnoses: List<Diagnosis>,
    onDiagnosisToggled: (Diagnosis) -> Unit,
    modifier: Modifier = Modifier
) {
    OnboardingStepColumn(modifier = modifier) {
        OnboardingHeading(stringResource(R.string.onboarding_diagnosis_title))
        Text(
            text = stringResource(R.string.onboarding_diagnosis_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Diagnosis.entries.forEach { diagnosis ->
            OnboardingChoice(
                label = diagnosis.displayLabel(),
                selected = diagnosis in selectedDiagnoses,
                onClick = { onDiagnosisToggled(diagnosis) },
                testTag = "onboarding_diagnosis_${diagnosis.name.lowercase()}",
                role = Role.Checkbox,
                icon = when (diagnosis) {
                    Diagnosis.AUTISM -> Icons.Default.Psychology
                    Diagnosis.ADHD -> Icons.Default.Bolt
                    Diagnosis.GLOBAL_DEVELOPMENT_DELAY -> Icons.Default.ChildCare
                    Diagnosis.SENSORY_PROCESSING -> Icons.Default.Hearing
                    Diagnosis.SPEECH_LANGUAGE -> Icons.Default.RecordVoiceOver
                    Diagnosis.OTHER -> Icons.Default.MoreHoriz
                }
            )
        }
    }
}

@Composable
private fun Diagnosis.displayLabel(): String = when (this) {
    Diagnosis.AUTISM -> stringResource(R.string.discovery_diagnosis_autism)
    Diagnosis.ADHD -> stringResource(R.string.discovery_diagnosis_adhd)
    Diagnosis.GLOBAL_DEVELOPMENT_DELAY -> stringResource(R.string.discovery_diagnosis_global_delay)
    Diagnosis.SENSORY_PROCESSING -> stringResource(R.string.discovery_diagnosis_sensory)
    Diagnosis.SPEECH_LANGUAGE -> stringResource(R.string.discovery_diagnosis_speech_language)
    Diagnosis.OTHER -> stringResource(R.string.discovery_other)
}
