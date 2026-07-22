package com.navigator.kindd.ui.onboarding

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.navigator.kindd.R
import com.navigator.kindd.data.profile.AudienceType
import com.navigator.kindd.ui.theme.KiNDDIndigo
import com.navigator.kindd.ui.theme.KiNDDShapeTokens

@Composable
internal fun AudienceStep(
    selectedAudience: AudienceType?,
    onAudienceSelected: (AudienceType) -> Unit,
    modifier: Modifier = Modifier
) {
    OnboardingStepColumn(modifier = modifier) {
        KiNDDOnboardingLogo()
        OnboardingHeading(stringResource(R.string.onboarding_welcome_title))
        Text(
            text = stringResource(R.string.onboarding_welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Text(
            text = stringResource(R.string.onboarding_audience_prompt),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    RoundedCornerShape(KiNDDShapeTokens.Compact)
                )
                .padding(4.dp)
                .testTag("onboarding_audience_segmented"),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AudienceSegment(
                label = stringResource(R.string.onboarding_audience_family),
                icon = Icons.Default.Groups,
                selected = selectedAudience == AudienceType.FAMILY,
                onClick = { onAudienceSelected(AudienceType.FAMILY) },
                testTag = "onboarding_audience_family",
                modifier = Modifier.weight(1f)
            )
            AudienceSegment(
                label = stringResource(R.string.onboarding_audience_clinician),
                icon = Icons.Default.MedicalServices,
                selected = selectedAudience == AudienceType.CLINICIAN,
                onClick = { onAudienceSelected(AudienceType.CLINICIAN) },
                testTag = "onboarding_audience_clinician",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun KiNDDOnboardingLogo() {
    Box(
        modifier = Modifier.fillMaxWidth().testTag("onboarding_logo"),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.kindd_logo),
            contentDescription = stringResource(R.string.kindd_logo_content_description),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth(0.48f)
                .aspectRatio(300f / 138f)
                .testTag("onboarding_logo_image")
        )
    }
}

@Composable
private fun AudienceSegment(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(13.dp)
    Row(
        modifier = modifier
            .heightIn(min = 52.dp)
            .background(
                if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                shape
            )
            .then(
                if (selected) Modifier.border(1.dp, KiNDDIndigo.copy(alpha = 0.35f), shape)
                else Modifier
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics {
                contentDescription = label
                this.selected = selected
            }
            .testTag(testTag)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) KiNDDIndigo else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 6.dp)
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
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
        content = content
    )
}

@Composable
internal fun OnboardingHeading(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().semantics { heading() },
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}

@Composable
internal fun OnboardingChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    role: Role,
    icon: ImageVector? = null
) {
    val shape = RoundedCornerShape(KiNDDShapeTokens.Selection)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag(testTag)
            .selectable(selected = selected, role = role, onClick = onClick)
            .semantics {
                contentDescription = label
                this.selected = selected
            },
        shape = shape,
        border = if (selected) androidx.compose.foundation.BorderStroke(
            1.5.dp,
            KiNDDIndigo.copy(alpha = 0.60f)
        ) else null,
        color = if (selected) {
            KiNDDIndigo.copy(alpha = 0.10f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            icon?.let {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(KiNDDIndigo.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = KiNDDIndigo,
                        modifier = Modifier.size(19.dp).testTag("onboarding_choice_icon")
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = KiNDDIndigo,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
