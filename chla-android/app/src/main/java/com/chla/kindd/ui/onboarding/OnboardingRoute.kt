package com.chla.kindd.ui.onboarding

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chla.kindd.R
import com.chla.kindd.data.profile.AgeGroup
import com.chla.kindd.data.profile.AudienceType
import com.chla.kindd.data.profile.JourneyStage
import com.chla.kindd.data.profile.UserProfile

@Composable
fun OnboardingRoute(
    mode: OnboardingMode,
    initialProfile: UserProfile,
    onSaved: () -> Unit = {},
    onClose: () -> Unit = {},
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    OnboardingBackGuard(
        state = state,
        mode = mode,
        onBack = viewModel::goBack,
        onClose = viewModel::cancel
    )
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onLocationPermissionResult(granted)
    }

    LaunchedEffect(viewModel, mode, initialProfile) {
        viewModel.initialize(mode, initialProfile)
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                OnboardingEvent.Saved -> onSaved()
                OnboardingEvent.Close -> onClose()
            }
        }
    }

    OnboardingContent(
        state = state,
        onAudienceSelected = viewModel::selectAudience,
        onZipChanged = viewModel::onZipChanged,
        onUseLocation = {
            if (viewModel.hasLocationPermission()) {
                viewModel.useCurrentLocation()
            } else {
                permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        },
        onRetryCenterLookup = viewModel::retryCenterLookup,
        onJourneySelected = viewModel::selectJourney,
        onAgeSelected = viewModel::selectAgeGroup,
        onBack = viewModel::goBack,
        onContinue = viewModel::continueFromCurrentStep,
        onFinish = viewModel::finish,
        onCancel = viewModel::cancel
    )
}

@Composable
internal fun OnboardingBackGuard(
    state: OnboardingUiState,
    mode: OnboardingMode = state.mode,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    val handlesBack = state.isSaving ||
        state.step != OnboardingStep.AUDIENCE ||
        mode == OnboardingMode.EDIT
    BackHandler(enabled = handlesBack) {
        when {
            state.isSaving -> Unit
            state.step != OnboardingStep.AUDIENCE -> onBack()
            mode == OnboardingMode.EDIT -> onClose()
        }
    }
}

@Composable
fun OnboardingContent(
    state: OnboardingUiState,
    onAudienceSelected: (AudienceType) -> Unit,
    onZipChanged: (String) -> Unit,
    onUseLocation: () -> Unit,
    onRetryCenterLookup: () -> Unit,
    onJourneySelected: (JourneyStage) -> Unit,
    onAgeSelected: (AgeGroup) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        OnboardingProgress(
            state = state,
            onCancel = onCancel
        )
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when (state.step) {
                OnboardingStep.AUDIENCE -> AudienceStep(
                    selectedAudience = state.draft.audienceType,
                    onAudienceSelected = onAudienceSelected
                )
                OnboardingStep.ZIP -> ZipStep(
                    zipCode = state.draft.zipCode.orEmpty(),
                    locationState = state.locationState,
                    isLookingUpCenter =
                        state.centerLookupState == CenterLookupState.LOADING,
                    useLocationEnabled =
                        !state.isSaving &&
                            state.locationState != LocationState.LOCATING &&
                            state.centerLookupState != CenterLookupState.LOADING,
                    canContinue = state.canContinue,
                    onZipChanged = onZipChanged,
                    onUseLocation = onUseLocation,
                    onContinue = onContinue
                )
                OnboardingStep.REGIONAL_CENTER -> RegionalCenterStep(
                    center = state.draft.regionalCenter,
                    lookupState = state.centerLookupState,
                    onRetry = onRetryCenterLookup
                )
                OnboardingStep.JOURNEY -> JourneyStep(
                    selectedJourney = state.draft.journeyStage,
                    onJourneySelected = onJourneySelected
                )
                OnboardingStep.AGE -> AgeGroupStep(
                    selectedAgeGroup = state.draft.ageGroup,
                    onAgeSelected = onAgeSelected
                )
            }
        }
        state.saveError?.let {
            Text(
                text = stringResource(R.string.onboarding_save_failed),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
        OnboardingActions(
            state = state,
            onBack = onBack,
            onContinue = onContinue,
            onFinish = onFinish
        )
    }
}

@Composable
private fun OnboardingProgress(
    state: OnboardingUiState,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    R.string.onboarding_progress,
                    state.progressStep,
                    OnboardingStep.entries.size
                ),
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.weight(1f))
            if (state.mode == OnboardingMode.EDIT) {
                TextButton(
                    onClick = onCancel,
                    enabled = !state.isSaving,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("onboarding_cancel_action")
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
        LinearProgressIndicator(
            progress = {
                state.progressStep / OnboardingStep.entries.size.toFloat()
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun OnboardingActions(
    state: OnboardingUiState,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onFinish: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.step != OnboardingStep.AUDIENCE) {
            TextButton(
                onClick = onBack,
                enabled = !state.isSaving,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("onboarding_back_action")
            ) {
                Text(stringResource(R.string.action_back))
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = if (state.step == OnboardingStep.AGE) onFinish else onContinue,
            enabled = state.canContinue && !state.isSaving,
            modifier = Modifier
                .heightIn(min = 48.dp)
                .testTag("onboarding_primary_action")
        ) {
            Text(
                stringResource(
                    when {
                        state.step != OnboardingStep.AGE -> R.string.action_continue
                        state.mode == OnboardingMode.EDIT -> R.string.action_save
                        else -> R.string.action_get_started
                    }
                )
            )
        }
    }
}
