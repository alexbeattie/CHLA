package com.navigator.kindd.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.navigator.kindd.data.profile.UserProfile
import com.navigator.kindd.ui.app.AppEntryContent
import com.navigator.kindd.ui.app.AppEntryState
import com.navigator.kindd.ui.app.AppEntryViewModel
import com.navigator.kindd.ui.onboarding.OnboardingMode
import com.navigator.kindd.ui.onboarding.OnboardingRoute

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
