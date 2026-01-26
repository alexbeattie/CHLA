package com.chla.kindd.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.List
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
import com.chla.kindd.ui.screens.*

sealed class Screen(
    val route: String,
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : Screen("home", R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home)
    data object Map : Screen("map", R.string.nav_map, Icons.Filled.Map, Icons.Outlined.Map)
    data object Providers : Screen("providers", R.string.nav_resources, Icons.Filled.List, Icons.Outlined.List)
    data object Chat : Screen("chat", R.string.nav_chat, Icons.Filled.Chat, Icons.Outlined.Chat)
    data object Settings : Screen("settings", R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings)

    // Detail screens (not in bottom nav)
    data object ProviderDetail : Screen("provider/{providerId}", R.string.provider_details, Icons.Filled.List, Icons.Outlined.List)
    data object RegionalCenters : Screen("regional-centers", R.string.regional_centers, Icons.Filled.Map, Icons.Outlined.Map)
    data object FAQ : Screen("faq", R.string.faq, Icons.Filled.Settings, Icons.Outlined.Settings)
    data object About : Screen("about", R.string.about, Icons.Filled.Settings, Icons.Outlined.Settings)
    data object Onboarding : Screen("onboarding", R.string.welcome, Icons.Filled.Home, Icons.Outlined.Home)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Map,
    Screen.Providers,
    Screen.Chat,
    Screen.Settings
)

@Composable
fun KINDDNavHost(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Hide bottom nav on certain screens
    val showBottomBar = currentDestination?.route !in listOf(
        Screen.Onboarding.route,
        Screen.ProviderDetail.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
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
                            }
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
                HomeScreen(
                    onNavigateToMap = { navController.navigate(Screen.Map.route) },
                    onNavigateToProviders = { navController.navigate(Screen.Providers.route) },
                    onNavigateToRegionalCenters = { navController.navigate(Screen.RegionalCenters.route) },
                    onNavigateToChat = { navController.navigate(Screen.Chat.route) }
                )
            }
            composable(Screen.Map.route) {
                MapScreen(
                    onProviderClick = { providerId ->
                        navController.navigate("provider/$providerId")
                    }
                )
            }
            composable(Screen.Providers.route) {
                ProviderListScreen(
                    onProviderClick = { providerId ->
                        navController.navigate("provider/$providerId")
                    }
                )
            }
            composable(Screen.Chat.route) {
                ChatScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToFAQ = { navController.navigate(Screen.FAQ.route) },
                    onNavigateToAbout = { navController.navigate(Screen.About.route) }
                )
            }
            composable(
                route = Screen.ProviderDetail.route,
                arguments = listOf(
                    navArgument("providerId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val providerId = backStackEntry.arguments?.getString("providerId") ?: return@composable
                ProviderDetailScreen(
                    providerId = providerId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.RegionalCenters.route) {
                RegionalCentersScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.FAQ.route) {
                FAQScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.About.route) {
                AboutScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onComplete = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
