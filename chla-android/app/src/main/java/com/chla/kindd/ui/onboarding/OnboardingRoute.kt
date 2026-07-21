package com.chla.kindd.ui.onboarding

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chla.kindd.R
import com.chla.kindd.data.profile.AgeGroup
import com.chla.kindd.data.profile.AudienceType
import com.chla.kindd.data.profile.JourneyStage
import com.chla.kindd.data.profile.UserProfile
import com.chla.kindd.ui.map.RegionalCenterMapRenderModel
import com.chla.kindd.ui.regions.RegionalCenterServiceAreaState
import com.chla.kindd.ui.regions.rememberRegionalCenterServiceAreas
import com.chla.kindd.ui.theme.KiNDDIndigo
import com.chla.kindd.ui.theme.KiNDDPrimaryGradientCapsule
import com.chla.kindd.ui.theme.KiNDDSecondaryCapsule
import com.chla.kindd.ui.theme.KiNDDViolet
import com.chla.kindd.ui.theme.kinddTopWash

@Composable
fun OnboardingRoute(
    mode: OnboardingMode,
    initialProfile: UserProfile,
    onSaved: () -> Unit = {},
    onClose: () -> Unit = {},
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val serviceAreaState by rememberRegionalCenterServiceAreas()
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
        onCancel = viewModel::cancel,
        serviceAreaState = serviceAreaState
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
    modifier: Modifier = Modifier,
    serviceAreaState: RegionalCenterServiceAreaState = RegionalCenterServiceAreaState.Loading,
    mapContent: (@Composable (RegionalCenterMapRenderModel, (String) -> Unit) -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(360.dp)
                .background(kinddTopWash())
        )
        Column(modifier = Modifier.fillMaxSize()) {
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
                        serviceAreaState = serviceAreaState,
                        mapContent = mapContent,
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
}

@Composable
private fun OnboardingProgress(
    state: OnboardingUiState,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 12.dp)
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
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .testTag("onboarding_progress_capsules"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OnboardingStep.entries.forEachIndexed { index, _ ->
                val active = index == state.step.ordinal
                Box(
                    modifier = Modifier
                        .size(width = if (active) 22.dp else 7.dp, height = 7.dp)
                        .background(
                            color = if (active) KiNDDIndigo else KiNDDIndigo.copy(alpha = 0.18f),
                            shape = CircleShape
                        )
                        .then(
                            if (active) Modifier.testTag("onboarding_progress_active_$index")
                            else Modifier
                        )
                )
            }
        }
    }
}

@Composable
private fun OnboardingActions(
    state: OnboardingUiState,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onFinish: () -> Unit
) {
    val primaryEnabled = state.canContinue && !state.isSaving
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.step != OnboardingStep.AUDIENCE) {
            KiNDDSecondaryCapsule(
                onClick = { if (!state.isSaving) onBack() },
                modifier = Modifier
                    .testTag("onboarding_back_action")
                    .alpha(if (state.isSaving) 0.45f else 1f)
                    .semantics { if (state.isSaving) disabled() }
            ) {
                Text(
                    text = stringResource(R.string.action_back),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        KiNDDPrimaryGradientCapsule(
            onClick = {
                if (primaryEnabled) {
                    if (state.step == OnboardingStep.AGE) onFinish() else onContinue()
                }
            },
            modifier = Modifier
                .testTag("onboarding_primary_action")
                .alpha(if (primaryEnabled) 1f else 0.45f)
                .semantics { if (!primaryEnabled) disabled() }
        ) {
            Row(
                modifier = Modifier.testTag("onboarding_primary_gradient"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(
                        when {
                            state.step != OnboardingStep.AGE -> R.string.action_continue
                            state.mode == OnboardingMode.EDIT -> R.string.action_save
                            else -> R.string.action_get_started
                        }
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
