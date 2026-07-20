package com.chla.kindd.ui.onboarding

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chla.kindd.R
import com.chla.kindd.data.profile.AudienceType

@Composable
internal fun AudienceStep(
    selectedAudience: AudienceType?,
    onAudienceSelected: (AudienceType) -> Unit,
    modifier: Modifier = Modifier
) {
    OnboardingStepColumn(modifier = modifier) {
        OnboardingHeading(stringResource(R.string.onboarding_welcome_title))
        Text(
            text = stringResource(R.string.onboarding_welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.onboarding_audience_prompt),
            style = MaterialTheme.typography.titleMedium
        )
        OnboardingChoice(
            label = stringResource(R.string.onboarding_audience_family),
            selected = selectedAudience == AudienceType.FAMILY,
            onClick = { onAudienceSelected(AudienceType.FAMILY) },
            testTag = "onboarding_audience_family",
            role = Role.RadioButton
        )
        OnboardingChoice(
            label = stringResource(R.string.onboarding_audience_clinician),
            selected = selectedAudience == AudienceType.CLINICIAN,
            onClick = { onAudienceSelected(AudienceType.CLINICIAN) },
            testTag = "onboarding_audience_clinician",
            role = Role.RadioButton
        )
    }
}

@Composable
internal fun OnboardingStepColumn(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = content
    )
}

@Composable
internal fun OnboardingHeading(text: String) {
    Text(
        text = text,
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
internal fun OnboardingChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    role: Role
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag(testTag)
            .selectable(selected = selected, role = role, onClick = onClick)
            .semantics {
                contentDescription = label
                this.selected = selected
            },
        shape = MaterialTheme.shapes.medium,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RadioButton(selected = selected, onClick = null)
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
