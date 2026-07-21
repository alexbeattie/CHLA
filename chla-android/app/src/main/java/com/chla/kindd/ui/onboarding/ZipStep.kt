package com.chla.kindd.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chla.kindd.R
import com.chla.kindd.ui.theme.KiNDDIndigo
import com.chla.kindd.ui.theme.KiNDDSecondaryCapsule

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
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(KiNDDIndigo.copy(alpha = 0.13f), RoundedCornerShape(18.dp))
                .testTag("onboarding_zip_icon"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = KiNDDIndigo,
                modifier = Modifier.size(28.dp)
            )
        }
        OnboardingHeading(stringResource(R.string.onboarding_zip_title))
        Text(
            text = stringResource(R.string.onboarding_zip_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 220.dp)
                    .testTag("onboarding_zip_control")
            ) {
                OutlinedTextField(
                    value = zipCode,
                    onValueChange = onZipChanged,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_zip_input"),
                    label = { Text(stringResource(R.string.onboarding_zip_label)) },
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KiNDDIndigo,
                        unfocusedBorderColor = KiNDDIndigo.copy(alpha = 0.20f)
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { if (canContinue) onContinue() }
                    )
                )
            }
        }
        when (locationState) {
            LocationState.LOCATING -> Row(
                modifier = Modifier
                    .testTag("onboarding_location_status")
                    .semantics { liveRegion = LiveRegionMode.Polite },
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
                modifier = Modifier
                    .testTag("onboarding_zip_lookup_loading")
                    .semantics { liveRegion = LiveRegionMode.Polite },
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator()
                Text(stringResource(R.string.loading))
            }
        }
        KiNDDSecondaryCapsule(
            onClick = { if (useLocationEnabled) onUseLocation() },
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("onboarding_use_location")
                .alpha(if (useLocationEnabled) 1f else 0.45f)
                .semantics { if (!useLocationEnabled) disabled() }
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null,
                    tint = KiNDDIndigo,
                    modifier = Modifier.size(19.dp)
                )
                Text(
                    text = stringResource(R.string.onboarding_use_location),
                    color = KiNDDIndigo,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun LocationMessage(stringId: Int) {
    Text(
        text = stringResource(stringId),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier
            .testTag("onboarding_location_status")
            .semantics { liveRegion = LiveRegionMode.Polite }
    )
}
