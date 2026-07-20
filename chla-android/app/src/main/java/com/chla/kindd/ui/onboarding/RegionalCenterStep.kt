package com.chla.kindd.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chla.kindd.R
import com.chla.kindd.data.profile.RegionalCenterIdentity

@Composable
internal fun RegionalCenterStep(
    center: RegionalCenterIdentity?,
    lookupState: CenterLookupState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    OnboardingStepColumn(modifier = modifier) {
        OnboardingHeading(stringResource(R.string.onboarding_center_title))
        when (lookupState) {
            CenterLookupState.MATCHED -> MatchedCenter(center)
            CenterLookupState.UNMATCHED -> {
                Text(
                    text = stringResource(R.string.onboarding_center_unmatched_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = stringResource(R.string.onboarding_center_unmatched_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            CenterLookupState.UNAVAILABLE -> {
                Text(
                    text = stringResource(R.string.onboarding_center_unavailable_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = stringResource(R.string.onboarding_center_unavailable_body),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("onboarding_retry_center")
                ) {
                    Text(stringResource(R.string.action_retry))
                }
            }
            CenterLookupState.LOADING -> Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator()
                Text(stringResource(R.string.loading))
            }
            CenterLookupState.IDLE -> Unit
        }
    }
}

@Composable
private fun MatchedCenter(center: RegionalCenterIdentity?) {
    Text(
        text = stringResource(R.string.onboarding_center_matched),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary
    )
    center?.let {
        Text(text = it.name, style = MaterialTheme.typography.titleLarge)
        Text(text = it.shortName, style = MaterialTheme.typography.titleMedium)
    }
    Text(
        text = stringResource(R.string.onboarding_center_body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
