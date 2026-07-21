package com.chla.kindd.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.chla.kindd.R
import com.chla.kindd.data.profile.UserProfile
import com.chla.kindd.ui.chat.ChatLaunchPrompt
import com.chla.kindd.ui.theme.KiNDDShapeTokens
import com.chla.kindd.ui.theme.KiNDDSpacingTokens

sealed class Screen(
    val route: String,
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val destinationRoute: String = route
) {
    data object Home : Screen("home", R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home)
    data object Map : Screen("map", R.string.nav_map, Icons.Filled.Map, Icons.Outlined.Map)
    data object Providers : Screen(
        "providers",
        R.string.nav_list,
        Icons.AutoMirrored.Filled.List,
        Icons.AutoMirrored.Outlined.List
    )
    data object Chat : Screen(
        "chat",
        R.string.nav_chat,
        Icons.AutoMirrored.Filled.Chat,
        Icons.AutoMirrored.Outlined.Chat,
        destinationRoute = "chat?prompt={prompt}"
    ) {
        fun createRoute(prompt: ChatLaunchPrompt?): String =
            prompt?.let { "$route?prompt=${it.routeValue}" } ?: route
    }
    data object Settings : Screen("settings", R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings)
    data object More : Screen("more", R.string.nav_more, Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz)

    data object ProviderDetail : Screen("provider/{providerId}", R.string.provider_details, Icons.AutoMirrored.Filled.List, Icons.AutoMirrored.Outlined.List)
    data object RegionalCenters : Screen("regional-centers", R.string.regional_centers, Icons.Filled.Map, Icons.Outlined.Map)
    data object FAQ : Screen("faq", R.string.faq, Icons.Filled.Settings, Icons.Outlined.Settings)
    data object About : Screen("about", R.string.about, Icons.Filled.Settings, Icons.Outlined.Settings)
    data object EditProfile : Screen("edit-profile", R.string.welcome, Icons.Filled.Settings, Icons.Outlined.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KINDDMainNavHost(
    profile: UserProfile,
    navController: NavHostController = rememberNavController(),
    destinationContent: MainDestinationContent = ProductionMainDestinationContent
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showFloatingNavigation = currentDestination?.route !in setOf(
        Screen.ProviderDetail.route,
        Screen.EditProfile.route,
        Screen.Chat.destinationRoute
    )
    val reserveFloatingNavigationClearance = showFloatingNavigation &&
        currentDestination?.route?.let { route ->
            route !in setOf(Screen.Home.route, Screen.Map.route)
        } == true
    var showChatSheet by rememberSaveable { mutableStateOf(false) }
    var chatPromptRouteValue by rememberSaveable { mutableStateOf<String?>(null) }
    val chatSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val chatPrompt = ChatLaunchPrompt.fromRouteValue(chatPromptRouteValue)
    val dismissChat = {
        showChatSheet = false
        chatPromptRouteValue = null
    }
    val navigateToPrimaryDestination: (Screen) -> Unit = { screen ->
        navController.navigate(screen.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
    val actions = MainNavActions(
        navigateToMap = { navigateToPrimaryDestination(Screen.Map) },
        navigateToList = { navigateToPrimaryDestination(Screen.Providers) },
        navigateToRegions = { navigateToPrimaryDestination(Screen.RegionalCenters) },
        navigateToChat = { prompt ->
            chatPromptRouteValue = prompt?.routeValue
            showChatSheet = true
        },
        navigateToProviderDetail = { providerId -> navController.navigate("provider/$providerId") },
        navigateToFaq = { navController.navigate(Screen.FAQ.route) },
        navigateToAbout = { navController.navigate(Screen.About.route) },
        navigateToEditProfile = { navController.navigate(Screen.EditProfile.route) },
        navigateToSettings = {
            navigateToPrimaryDestination(Screen.More)
            navController.navigate(Screen.Settings.route) { launchSingleTop = true }
        },
        navigateBack = {
            if (showChatSheet) dismissChat() else navController.popBackStack()
        }
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_nav_root")
    ) {
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (reserveFloatingNavigationClearance) {
                        Modifier
                            .navigationBarsPadding()
                            .padding(bottom = KiNDDSpacingTokens.FloatingNavigationContentClearance)
                    } else {
                        Modifier
                    }
                )
        ) {
            composable(Screen.Home.route) {
                destinationContent.home(profile, actions)
            }
            composable(Screen.Map.route) {
                destinationContent.map(actions)
            }
            composable(Screen.Providers.route) {
                destinationContent.list(actions)
            }
            composable(
                route = Screen.Chat.destinationRoute,
                arguments = listOf(
                    navArgument("prompt") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val prompt = ChatLaunchPrompt.fromRouteValue(
                    backStackEntry.arguments?.getString("prompt")
                )
                LaunchedEffect(backStackEntry.id, prompt) {
                    chatPromptRouteValue = prompt?.routeValue
                    showChatSheet = true
                    navController.popBackStack()
                }
            }
            composable(Screen.Settings.route) {
                destinationContent.settings(actions)
            }
            composable(Screen.More.route) {
                destinationContent.more(actions)
            }
            composable(
                route = Screen.ProviderDetail.route,
                arguments = listOf(
                    navArgument("providerId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val providerId = backStackEntry.arguments?.getString("providerId")
                    ?: return@composable
                destinationContent.providerDetail(providerId, actions)
            }
            composable(Screen.RegionalCenters.route) {
                destinationContent.regions(actions)
            }
            composable(Screen.FAQ.route) {
                destinationContent.faq(actions)
            }
            composable(Screen.About.route) {
                destinationContent.about(actions)
            }
            composable(Screen.EditProfile.route) {
                destinationContent.editProfile(profile, actions)
            }
        }

        if (showFloatingNavigation) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(
                        horizontal = KiNDDSpacingTokens.PageInset,
                        vertical = KiNDDSpacingTokens.FloatingNavigationBottom
                    ),
                contentAlignment = Alignment.Center
            ) {
                KiNDDFloatingNavigation(
                    currentRoute = currentDestination?.route,
                    onDestinationClick = navigateToPrimaryDestination,
                    onAskClick = { actions.navigateToChat(null) }
                )
            }
        }
    }

    if (showChatSheet) {
        ModalBottomSheet(
            onDismissRequest = dismissChat,
            sheetState = chatSheetState,
            shape = RoundedCornerShape(
                topStart = KiNDDShapeTokens.Sheet,
                topEnd = KiNDDShapeTokens.Sheet
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.94f)
                    .testTag("chat_modal_sheet")
            ) {
                destinationContent.chat(chatPrompt, actions)
            }
        }
    }
}
