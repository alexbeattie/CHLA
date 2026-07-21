package com.chla.kindd.ui.navigation

import androidx.compose.runtime.Composable
import com.chla.kindd.data.profile.UserProfile
import com.chla.kindd.ui.chat.ChatLaunchPrompt
import com.chla.kindd.ui.onboarding.OnboardingMode
import com.chla.kindd.ui.onboarding.OnboardingRoute
import com.chla.kindd.ui.screens.AboutScreen
import com.chla.kindd.ui.screens.ChatScreen
import com.chla.kindd.ui.screens.FAQScreen
import com.chla.kindd.ui.screens.HomeScreen
import com.chla.kindd.ui.screens.MapScreen
import com.chla.kindd.ui.screens.MoreScreen
import com.chla.kindd.ui.screens.ProviderDetailScreen
import com.chla.kindd.ui.screens.ProviderListScreen
import com.chla.kindd.ui.screens.RegionalCentersScreen
import com.chla.kindd.ui.screens.SettingsScreen

data class MainNavActions(
    val navigateToMap: () -> Unit,
    val navigateToList: () -> Unit,
    val navigateToRegions: () -> Unit,
    val navigateToChat: (ChatLaunchPrompt?) -> Unit,
    val navigateToProviderDetail: (String) -> Unit,
    val navigateToFaq: () -> Unit,
    val navigateToAbout: () -> Unit,
    val navigateToEditProfile: () -> Unit,
    val navigateBack: () -> Unit,
    val navigateToSettings: () -> Unit = {}
)

internal data class SettingsDestinationActions(
    val navigateToFaq: () -> Unit,
    val navigateToAbout: () -> Unit,
    val navigateToEditProfile: () -> Unit,
    val navigateBackToMore: () -> Unit
)

internal fun MainNavActions.toSettingsDestinationActions() = SettingsDestinationActions(
    navigateToFaq = navigateToFaq,
    navigateToAbout = navigateToAbout,
    navigateToEditProfile = navigateToEditProfile,
    navigateBackToMore = navigateBack
)

interface MainDestinationContent {
    @Composable
    fun home(profile: UserProfile, actions: MainNavActions)

    @Composable
    fun map(actions: MainNavActions)

    @Composable
    fun list(actions: MainNavActions)

    @Composable
    fun chat(prompt: ChatLaunchPrompt?, actions: MainNavActions)

    @Composable
    fun settings(actions: MainNavActions)

    @Composable
    fun more(actions: MainNavActions)

    @Composable
    fun providerDetail(providerId: String, actions: MainNavActions)

    @Composable
    fun regions(actions: MainNavActions)

    @Composable
    fun faq(actions: MainNavActions)

    @Composable
    fun about(actions: MainNavActions)

    @Composable
    fun editProfile(profile: UserProfile, actions: MainNavActions)
}

object ProductionMainDestinationContent : MainDestinationContent {
    @Composable
    override fun home(profile: UserProfile, actions: MainNavActions) {
        HomeScreen(
            profile = profile,
            onNavigateToMap = actions.navigateToMap,
            onNavigateToProviders = actions.navigateToList,
            onNavigateToRegionalCenters = actions.navigateToRegions,
            onNavigateToChat = actions.navigateToChat,
            onNavigateToAbout = actions.navigateToAbout,
            onNavigateToFaq = actions.navigateToFaq,
            onNavigateToEditProfile = actions.navigateToEditProfile,
            onNavigateToSettings = actions.navigateToSettings
        )
    }

    @Composable
    override fun map(actions: MainNavActions) {
        MapScreen(
            onProviderClick = actions.navigateToProviderDetail,
            onNavigateToList = actions.navigateToList
        )
    }

    @Composable
    override fun list(actions: MainNavActions) {
        ProviderListScreen(onProviderClick = actions.navigateToProviderDetail)
    }

    @Composable
    override fun chat(prompt: ChatLaunchPrompt?, actions: MainNavActions) {
        ChatScreen(initialPrompt = prompt)
    }

    @Composable
    override fun settings(actions: MainNavActions) {
        val settingsActions = actions.toSettingsDestinationActions()
        SettingsScreen(
            onNavigateToFAQ = settingsActions.navigateToFaq,
            onNavigateToAbout = settingsActions.navigateToAbout,
            onNavigateToEditProfile = settingsActions.navigateToEditProfile,
            onNavigateBack = settingsActions.navigateBackToMore
        )
    }

    @Composable
    override fun more(actions: MainNavActions) {
        MoreScreen(
            onNavigateToFAQ = actions.navigateToFaq,
            onNavigateToAbout = actions.navigateToAbout,
            onNavigateToRegions = actions.navigateToRegions,
            onNavigateToEditProfile = actions.navigateToEditProfile,
            onNavigateToSettings = actions.navigateToSettings
        )
    }

    @Composable
    override fun providerDetail(providerId: String, actions: MainNavActions) {
        ProviderDetailScreen(
            providerId = providerId,
            onBack = actions.navigateBack
        )
    }

    @Composable
    override fun regions(actions: MainNavActions) {
        RegionalCentersScreen(onBack = actions.navigateBack)
    }

    @Composable
    override fun faq(actions: MainNavActions) {
        FAQScreen(onBack = actions.navigateBack)
    }

    @Composable
    override fun about(actions: MainNavActions) {
        AboutScreen(onBack = actions.navigateBack)
    }

    @Composable
    override fun editProfile(profile: UserProfile, actions: MainNavActions) {
        OnboardingRoute(
            mode = OnboardingMode.EDIT,
            initialProfile = profile,
            onSaved = actions.navigateBack,
            onClose = actions.navigateBack
        )
    }
}
