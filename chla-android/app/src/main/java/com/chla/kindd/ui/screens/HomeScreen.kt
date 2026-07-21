package com.chla.kindd.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.TextLayoutResult
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chla.kindd.data.discovery.TherapyType
import com.chla.kindd.data.profile.UserProfile
import com.chla.kindd.platform.launchDialer
import com.chla.kindd.ui.chat.ChatLaunchPrompt
import com.chla.kindd.ui.home.HomeEvent
import com.chla.kindd.ui.home.HomeUiState
import com.chla.kindd.ui.home.HomeViewModel
import com.chla.kindd.ui.home.components.HomeAskCapsule
import com.chla.kindd.ui.home.components.HomeCompactHeader
import com.chla.kindd.ui.home.components.HomeInfoFooter
import com.chla.kindd.ui.home.components.HomeMapHero
import com.chla.kindd.ui.home.components.HomeNextStepCard
import com.chla.kindd.ui.home.components.HomeQuestionSection
import com.chla.kindd.ui.home.components.HomeServiceTiles
import com.chla.kindd.ui.map.RegionalCenterMapRenderModel
import com.chla.kindd.ui.theme.KiNDDSpacingTokens
import com.chla.kindd.ui.theme.kinddTopWash

@Composable
fun HomeScreen(
    profile: UserProfile,
    onNavigateToMap: () -> Unit,
    onNavigateToProviders: () -> Unit,
    onNavigateToRegionalCenters: () -> Unit,
    onNavigateToChat: (ChatLaunchPrompt?) -> Unit,
    onNavigateToAbout: () -> Unit = {},
    onNavigateToFaq: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel, profile) {
        viewModel.onReadyProfileChanged(profile)
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                HomeEvent.NavigateToMap -> onNavigateToMap()
                HomeEvent.NavigateToList -> onNavigateToProviders()
                HomeEvent.NavigateToRegionalCenters -> onNavigateToRegionalCenters()
                is HomeEvent.NavigateToChat -> onNavigateToChat(event.prompt)
                is HomeEvent.Dial -> context.launchDialer(event.digits)
            }
        }
    }

    HomeContent(
        profile = profile,
        uiState = uiState,
        onZipChanged = viewModel::onZipChanged,
        onSubmitZip = {
            viewModel.submitZip(
                expectedProfile = profile,
                displayedZip = uiState.displayedZip(profile)
            )
        },
        onNavigateToMap = viewModel::openMap,
        onNavigateToList = viewModel::openList,
        onNavigateToRegionalCenters = viewModel::openRegionalCenters,
        onNavigateToChat = viewModel::openChat,
        onOpenChat = { onNavigateToChat(null) },
        onTherapySelected = viewModel::selectTherapy,
        onCall = { digits -> viewModel.callCenter(profile, digits) },
        onNavigateToAbout = onNavigateToAbout,
        onNavigateToFaq = onNavigateToFaq,
        onNavigateToEditProfile = onNavigateToEditProfile,
        onNavigateToSettings = onNavigateToSettings
    )
}

@Composable
@Suppress("UNUSED_PARAMETER")
fun HomeContent(
    profile: UserProfile,
    uiState: HomeUiState,
    onZipChanged: (String) -> Unit,
    onSubmitZip: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToList: () -> Unit,
    onNavigateToRegionalCenters: () -> Unit,
    onNavigateToChat: (ChatLaunchPrompt) -> Unit,
    onOpenChat: () -> Unit,
    onTherapySelected: (TherapyType) -> Unit,
    onCall: (String) -> Unit,
    onNavigateToAbout: () -> Unit = {},
    onNavigateToFaq: () -> Unit = {},
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    regionalCenterMapContent: (@Composable (
        RegionalCenterMapRenderModel,
        (String) -> Unit
    ) -> Unit)? = null,
    onCenterRoleTextLayout: (TextLayoutResult) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(kinddTopWash())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = KiNDDSpacingTokens.PageInset)
                .padding(top = 14.dp, bottom = 190.dp),
            verticalArrangement = Arrangement.spacedBy(KiNDDSpacingTokens.Section)
        ) {
            HomeCompactHeader(
                onEditProfile = onNavigateToEditProfile,
                onOpenSettings = onNavigateToSettings
            )

            HomeMapHero(
                profile = profile,
                uiState = uiState,
                onZipChanged = onZipChanged,
                onSubmitZip = onSubmitZip,
                onExplore = onNavigateToRegionalCenters,
                onDetails = onNavigateToRegionalCenters,
                onCall = onCall,
                mapContent = regionalCenterMapContent,
                onCenterRoleTextLayout = onCenterRoleTextLayout
            )

            HomeServiceTiles(onTherapySelected = onTherapySelected)

            HomeNextStepCard(
                profile = profile,
                uiState = uiState,
                onChat = onNavigateToChat,
                onCall = onCall
            )

            HomeQuestionSection(
                profile = profile,
                onChat = onNavigateToChat
            )

            HomeInfoFooter(
                onAbout = onNavigateToAbout,
                onFaq = onNavigateToFaq
            )
        }

        HomeAskCapsule(
            onClick = onOpenChat,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = KiNDDSpacingTokens.PageInset)
                .padding(bottom = 94.dp)
        )
    }
}
