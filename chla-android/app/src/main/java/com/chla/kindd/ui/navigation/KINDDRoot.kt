package com.chla.kindd.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chla.kindd.data.profile.UserProfile
import com.chla.kindd.ui.app.AppEntryContent
import com.chla.kindd.ui.app.AppEntryState
import com.chla.kindd.ui.app.AppEntryViewModel
import com.chla.kindd.ui.onboarding.OnboardingMode
import com.chla.kindd.ui.onboarding.OnboardingRoute

@Composable
fun KINDDRoot(
    viewModel: AppEntryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    KINDDRootContent(state = state)
}

@Composable
fun KINDDRootContent(
    state: AppEntryState,
    onboardingContent: @Composable (UserProfile) -> Unit = { draft ->
        OnboardingRoute(
            mode = OnboardingMode.FIRST_RUN,
            initialProfile = draft
        )
    },
    mainContent: @Composable (UserProfile) -> Unit = { profile ->
        KINDDMainNavHost(profile = profile)
    }
) {
    AppEntryContent(
        state = state,
        onboardingContent = onboardingContent,
        mainContent = mainContent
    )
}
