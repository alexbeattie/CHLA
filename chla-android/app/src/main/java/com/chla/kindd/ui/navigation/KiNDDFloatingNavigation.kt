package com.chla.kindd.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.chla.kindd.R
import com.chla.kindd.ui.theme.KiNDDAiGradient
import com.chla.kindd.ui.theme.KiNDDCompactIconAction
import com.chla.kindd.ui.theme.KiNDDGlassSurface
import com.chla.kindd.ui.theme.KiNDDIndigo

internal const val FLOATING_NAV_CAPSULE_TAG = "floating_nav_capsule"
internal const val FLOATING_NAV_HOME_TAG = "bottom_nav_home"
internal const val FLOATING_NAV_MAP_TAG = "bottom_nav_map"
internal const val FLOATING_NAV_ASK_TAG = "bottom_nav_ask"
internal const val FLOATING_NAV_REGIONS_TAG = "bottom_nav_regions"
internal const val FLOATING_NAV_LIST_TAG = "bottom_nav_list"
internal const val FLOATING_NAV_MORE_TAG = "bottom_nav_more"

private data class FloatingDestination(
    val screen: Screen,
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

private val floatingDestinations = listOf(
    FloatingDestination(
        Screen.Home,
        R.string.nav_home,
        Icons.Filled.Home,
        Icons.Outlined.Home,
        FLOATING_NAV_HOME_TAG
    ),
    FloatingDestination(
        Screen.Map,
        R.string.nav_map,
        Icons.Filled.Place,
        Icons.Outlined.Place,
        FLOATING_NAV_MAP_TAG
    ),
    FloatingDestination(
        Screen.RegionalCenters,
        R.string.nav_regions,
        Icons.Filled.Map,
        Icons.Outlined.Map,
        FLOATING_NAV_REGIONS_TAG
    ),
    FloatingDestination(
        Screen.Providers,
        R.string.nav_list,
        Icons.AutoMirrored.Filled.List,
        Icons.AutoMirrored.Filled.List,
        FLOATING_NAV_LIST_TAG
    ),
    FloatingDestination(
        Screen.More,
        R.string.nav_more,
        Icons.Filled.MoreHoriz,
        Icons.Outlined.MoreHoriz,
        FLOATING_NAV_MORE_TAG
    )
)

internal fun isMoreRoute(currentRoute: String?): Boolean = when (currentRoute) {
    Screen.More.route,
    Screen.Settings.route,
    Screen.FAQ.route,
    Screen.About.route -> true
    else -> false
}

@Composable
internal fun KiNDDFloatingNavigation(
    currentRoute: String?,
    onDestinationClick: (Screen) -> Unit,
    onAskClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    KiNDDGlassSurface(
        modifier = modifier.testTag(FLOATING_NAV_CAPSULE_TAG),
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            floatingDestinations.take(2).forEach { destination ->
                FloatingDestinationAction(destination, currentRoute, onDestinationClick)
            }
            KiNDDCompactIconAction(
                icon = Icons.Filled.AutoAwesome,
                contentDescription = stringResource(R.string.nav_ask),
                onClick = onAskClick,
                iconBrush = KiNDDAiGradient,
                modifier = Modifier
                    .padding(horizontal = 1.dp)
                    .testTag(FLOATING_NAV_ASK_TAG)
                    .semantics {
                        role = Role.Button
                    }
            )
            floatingDestinations.drop(2).forEach { destination ->
                FloatingDestinationAction(destination, currentRoute, onDestinationClick)
            }
        }
    }
}

@Composable
private fun FloatingDestinationAction(
    destination: FloatingDestination,
    currentRoute: String?,
    onDestinationClick: (Screen) -> Unit
) {
    val selected = currentRoute == destination.screen.destinationRoute ||
        currentRoute == destination.screen.route ||
        (destination.screen == Screen.More && isMoreRoute(currentRoute))
    val label = stringResource(destination.labelRes)
    KiNDDCompactIconAction(
        icon = if (selected) destination.selectedIcon else destination.unselectedIcon,
        contentDescription = label,
        onClick = { onDestinationClick(destination.screen) },
        tint = if (selected) KiNDDIndigo else MaterialTheme.colorScheme.onSurfaceVariant,
        containerColor = if (selected) KiNDDIndigo.copy(alpha = 0.10f) else null,
        modifier = Modifier
            .testTag(destination.tag)
            .semantics {
                this.selected = selected
                role = Role.Tab
            }
    )
}
