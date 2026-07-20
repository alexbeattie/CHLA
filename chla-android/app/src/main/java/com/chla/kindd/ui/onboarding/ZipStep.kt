package com.chla.kindd.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chla.kindd.R

@Composable
internal fun ZipStep(
    zipCode: String,
    locationState: LocationState,
    isLookingUpCenter: Boolean,
    useLocationEnabled: Boolean,
    canContinue: Boolean,
    onZipChanged: (String) -> Unit,
    onUseLocation: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    OnboardingStepColumn(modifier = modifier) {
        OnboardingHeading(stringResource(R.string.onboarding_zip_title))
        Text(
            text = stringResource(R.string.onboarding_zip_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = zipCode,
            onValueChange = onZipChanged,
            modifier = Modifier.fillMaxWidth().testTag("onboarding_zip_input"),
            label = { Text(stringResource(R.string.onboarding_zip_label)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { if (canContinue) onContinue() }
            )
        )
        when (locationState) {
            LocationState.LOCATING -> Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator()
                Text(stringResource(R.string.onboarding_location_locating))
            }
            LocationState.DENIED -> LocationMessage(R.string.onboarding_location_denied)
            LocationState.FAILED -> LocationMessage(R.string.onboarding_location_failed)
            LocationState.IDLE -> Unit
        }
        if (isLookingUpCenter) {
            Row(
                modifier = Modifier.testTag("onboarding_zip_lookup_loading"),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator()
                Text(stringResource(R.string.loading))
            }
        }
        TextButton(
            onClick = onUseLocation,
            enabled = useLocationEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("onboarding_use_location")
        ) {
            Text(stringResource(R.string.onboarding_use_location))
        }
    }
}

@Composable
private fun LocationMessage(stringId: Int) {
    Text(
        text = stringResource(stringId),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error
    )
}
