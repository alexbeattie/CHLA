package com.chla.kindd.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
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

sealed class Screen(
    val route: String,
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : Screen("home", R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home)
    data object Map : Screen("map", R.string.nav_map, Icons.Filled.Map, Icons.Outlined.Map)
    data object Providers : Screen("providers", R.string.nav_resources, Icons.AutoMirrored.Filled.List, Icons.AutoMirrored.Outlined.List)
    data object Chat : Screen("chat", R.string.nav_chat, Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat)
    data object Settings : Screen("settings", R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings)

    data object ProviderDetail : Screen("provider/{providerId}", R.string.provider_details, Icons.AutoMirrored.Filled.List, Icons.AutoMirrored.Outlined.List)
    data object RegionalCenters : Screen("regional-centers", R.string.regional_centers, Icons.Filled.Map, Icons.Outlined.Map)
    data object FAQ : Screen("faq", R.string.faq, Icons.Filled.Settings, Icons.Outlined.Settings)
    data object About : Screen("about", R.string.about, Icons.Filled.Settings, Icons.Outlined.Settings)
    data object EditProfile : Screen("edit-profile", R.string.welcome, Icons.Filled.Settings, Icons.Outlined.Settings)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Map,
    Screen.Providers,
    Screen.Chat,
    Screen.Settings
)

@Composable
fun KINDDMainNavHost(
    profile: UserProfile,
    navController: NavHostController = rememberNavController(),
    destinationContent: MainDestinationContent = ProductionMainDestinationContent
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = currentDestination?.route !in setOf(
        Screen.ProviderDetail.route,
        Screen.EditProfile.route
    )
    val actions = MainNavActions(
        navigateToMap = { navController.navigate(Screen.Map.route) },
        navigateToList = { navController.navigate(Screen.Providers.route) },
        navigateToRegions = { navController.navigate(Screen.RegionalCenters.route) },
        navigateToChat = { navController.navigate(Screen.Chat.route) },
        navigateToProviderDetail = { providerId -> navController.navigate("provider/$providerId") },
        navigateToFaq = { navController.navigate(Screen.FAQ.route) },
        navigateToAbout = { navController.navigate(Screen.About.route) },
        navigateToEditProfile = { navController.navigate(Screen.EditProfile.route) },
        navigateBack = { navController.popBackStack() }
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == screen.route
                        } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) {
                                        screen.selectedIcon
                                    } else {
                                        screen.unselectedIcon
                                    },
                                    contentDescription = stringResource(screen.titleRes)
                                )
                            },
                            label = { Text(stringResource(screen.titleRes)) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            modifier = Modifier.testTag(screen.bottomNavigationTag())
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
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
            composable(Screen.Chat.route) {
                destinationContent.chat(actions)
            }
            composable(Screen.Settings.route) {
                destinationContent.settings(actions)
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
    }
}

private fun Screen.bottomNavigationTag(): String = when (this) {
    Screen.Providers -> "bottom_nav_list"
    else -> "bottom_nav_$route"
}
