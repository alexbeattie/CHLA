package com.chla.kindd.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chla.kindd.R
import com.chla.kindd.ui.theme.KiNDDIndigo
import com.chla.kindd.ui.theme.KiNDDShapeTokens

@Composable
fun HowToGuideContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth().testTag("onboarding_howto_content"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(KiNDDIndigo.copy(alpha = 0.13f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = null,
                tint = KiNDDIndigo,
                modifier = Modifier.size(26.dp)
            )
        }
        OnboardingHeading(stringResource(R.string.onboarding_howto_title))
        Text(
            text = stringResource(R.string.onboarding_howto_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surface,
                    RoundedCornerShape(KiNDDShapeTokens.Selection)
                )
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                    RoundedCornerShape(KiNDDShapeTokens.Selection)
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HowToRow(
                icon = Icons.Default.FilterList,
                title = stringResource(R.string.onboarding_howto_filters_title),
                detail = stringResource(R.string.onboarding_howto_filters_body)
            )
            HowToRow(
                icon = Icons.Default.Groups,
                title = stringResource(R.string.onboarding_howto_children_title),
                detail = stringResource(R.string.onboarding_howto_children_body)
            )
            HowToRow(
                icon = Icons.Default.Lock,
                title = stringResource(R.string.onboarding_howto_privacy_title),
                detail = stringResource(R.string.onboarding_howto_privacy_body)
            )
        }
    }
}

@Composable
private fun HowToRow(
    icon: ImageVector,
    title: String,
    detail: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = KiNDDIndigo,
            modifier = Modifier.size(18.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun HowToStep(
    modifier: Modifier = Modifier
) {
    OnboardingStepColumn(modifier = modifier) {
        HowToGuideContent()
    }
}
