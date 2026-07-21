package com.chla.kindd.ui.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.chla.kindd.R
import com.chla.kindd.data.profile.UserProfile

const val APP_ENTRY_LOADING_TAG = "app_entry_loading"
const val APP_ENTRY_ONBOARDING_TAG = "app_entry_onboarding"
const val APP_ENTRY_MAIN_TAG = "app_entry_main"

@Composable
fun AppEntryContent(
    state: AppEntryState,
    onboardingContent: @Composable (UserProfile) -> Unit,
    mainContent: @Composable (UserProfile) -> Unit
) {
    when (state) {
        AppEntryState.Loading -> LaunchScreen(
            modifier = Modifier.testTag(APP_ENTRY_LOADING_TAG)
        )

        is AppEntryState.NeedsOnboarding -> Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag(APP_ENTRY_ONBOARDING_TAG)
        ) {
            onboardingContent(state.draft)
        }

        is AppEntryState.Ready -> Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag(APP_ENTRY_MAIN_TAG)
        ) {
            mainContent(state.profile)
        }
    }
}

@Composable
fun LaunchScreen(modifier: Modifier = Modifier) {
    val loadingDescription = stringResource(R.string.app_entry_loading_content_description)
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "KiNDD",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .size(40.dp)
                    .semantics { contentDescription = loadingDescription },
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
