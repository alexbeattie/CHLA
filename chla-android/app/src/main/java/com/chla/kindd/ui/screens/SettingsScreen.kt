package com.chla.kindd.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chla.kindd.BuildConfig
import com.chla.kindd.R
import com.chla.kindd.data.discovery.DiscoveryCriteria
import com.chla.kindd.data.profile.AudienceType
import com.chla.kindd.ui.discovery.DiscoveryFilterSelection
import com.chla.kindd.ui.discovery.DiscoveryFilterSheet
import com.chla.kindd.ui.settings.SettingsEvent
import com.chla.kindd.ui.settings.SettingsLocationPermissionStatus
import com.chla.kindd.ui.settings.SettingsViewModel
import com.chla.kindd.ui.settings.formatSettingsAppVersion
import com.chla.kindd.ui.settings.supportsPerAppLanguageSettings
import com.chla.kindd.ui.theme.KiNDDIndigo
import com.chla.kindd.ui.theme.KiNDDPurple
import com.chla.kindd.ui.theme.KiNDDShapeTokens
import com.chla.kindd.ui.theme.KiNDDSpacingTokens
import com.chla.kindd.ui.theme.KiNDDViolet
import com.chla.kindd.ui.theme.kinddTopWash

@SuppressLint("InlinedApi")
@Composable
fun SettingsScreen(
    onNavigateToFAQ: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateBack: (() -> Unit)? = null,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    var clearFailed by rememberSaveable { mutableStateOf(false) }
    var preferenceUpdateFailed by rememberSaveable { mutableStateOf(false) }
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val discoveryState by viewModel.discoveryState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val editNavigator = remember(onNavigateToEditProfile) { onNavigateToEditProfile }
    LaunchedEffect(viewModel, editNavigator) {
        viewModel.events.collect { event ->
            when (event) {
                SettingsEvent.NavigateToEditProfile -> editNavigator()
                SettingsEvent.ClearFailed -> clearFailed = true
                SettingsEvent.PreferenceUpdateFailed -> preferenceUpdateFailed = true
            }
        }
    }

    val locationPermissionStatus = remember(context) {
        SettingsLocationPermissionStatus {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    DisposableEffect(lifecycleOwner, locationPermissionStatus) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                locationPermissionStatus.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val openLanguageSettings: (() -> Unit)? =
        if (supportsPerAppLanguageSettings(Build.VERSION.SDK_INT)) {
            {
                context.launchSafely(
                    Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                )
            }
        } else {
            null
        }

    SettingsContent(
        onNavigateToFAQ = onNavigateToFAQ,
        onNavigateToAbout = onNavigateToAbout,
        onEditProfile = viewModel::editProfile,
        onClearProfile = {
            clearFailed = false
            viewModel.clearProfile()
        },
        clearFailed = clearFailed,
        onNavigateBack = onNavigateBack,
        onOpenLanguageSettings = openLanguageSettings,
        onOpenLocationSettings = {
            context.launchSafely(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            )
        },
        onOpenWebsite = {
            context.launchSafely(Intent(Intent.ACTION_VIEW, Uri.parse(KINDD_WEBSITE_URL)))
        },
        appMode = profile.audienceType,
        criteria = discoveryState.criteria,
        onAppModeChange = { audienceType ->
            preferenceUpdateFailed = false
            viewModel.updateAppMode(audienceType)
        },
        onApplySearchFilters = viewModel::applySearchFilters,
        onDefaultRadiusChange = viewModel::updateDefaultRadius,
        preferenceUpdateFailed = preferenceUpdateFailed,
        locationStatus = stringResource(
            if (locationPermissionStatus.isAllowed) R.string.settings_location_allowed
            else R.string.settings_location_not_allowed
        ),
        appVersion = formatSettingsAppVersion(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
    )
}

@Composable
fun SettingsContent(
    onNavigateToFAQ: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onEditProfile: () -> Unit,
    onClearProfile: () -> Unit,
    modifier: Modifier = Modifier,
    clearFailed: Boolean = false,
    onNavigateBack: (() -> Unit)? = null,
    onOpenLanguageSettings: (() -> Unit)? = null,
    onOpenLocationSettings: (() -> Unit)? = null,
    onOpenWebsite: (() -> Unit)? = null,
    appMode: AudienceType? = null,
    criteria: DiscoveryCriteria? = null,
    onAppModeChange: ((AudienceType) -> Unit)? = null,
    onApplySearchFilters: ((DiscoveryFilterSelection) -> Unit)? = null,
    onDefaultRadiusChange: ((Int) -> Unit)? = null,
    preferenceUpdateFailed: Boolean = false,
    locationStatus: String? = null,
    appVersion: String = formatSettingsAppVersion(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
) {
    var showClearConfirmation by rememberSaveable { mutableStateOf(false) }
    var showAppModeSelection by rememberSaveable { mutableStateOf(false) }
    var showSearchFilters by rememberSaveable { mutableStateOf(false) }
    var showRadiusSelection by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("settings_grouped_canvas")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(kinddTopWash())
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .testTag("settings_list"),
            contentPadding = PaddingValues(
                start = KiNDDSpacingTokens.PageInset,
                top = 8.dp,
                end = KiNDDSpacingTokens.PageInset,
                bottom = 36.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SettingsHeader(onNavigateBack = onNavigateBack)
            }

            if (onOpenLanguageSettings != null) {
                item {
                    SettingsSectionLabel(
                        text = stringResource(R.string.language),
                        testTag = "settings_language_heading"
                    )
                }
                item {
                    SettingsGroup(modifier = Modifier.testTag("settings_language_group")) {
                        SettingsRow(
                            icon = Icons.Default.Language,
                            iconTint = KiNDDIndigo,
                            title = stringResource(R.string.language),
                            subtitle = stringResource(R.string.settings_language_description),
                            trailingText = stringResource(R.string.settings_language_system_default),
                            onClick = onOpenLanguageSettings,
                            modifier = Modifier.testTag("settings_language")
                        )
                    }
                }
            }

            item {
                SettingsSectionLabel(
                    text = stringResource(R.string.settings_setup),
                    modifier = Modifier.padding(top = 6.dp),
                    testTag = "settings_profile_heading"
                )
            }
            item {
                SettingsGroup(modifier = Modifier.testTag("settings_setup_group")) {
                    SettingsRow(
                        icon = Icons.Default.RestartAlt,
                        iconTint = KiNDDIndigo,
                        title = stringResource(R.string.settings_restart_welcome_setup),
                        subtitle = stringResource(R.string.settings_restart_welcome_setup_description),
                        onClick = onEditProfile,
                        modifier = Modifier.testTag("settings_restart_setup")
                    )
                }
            }

            item {
                SettingsSectionLabel(
                    text = stringResource(R.string.settings_search_preferences),
                    modifier = Modifier.padding(top = 6.dp),
                    testTag = "settings_search_preferences_heading"
                )
            }
            item {
                SettingsGroup(modifier = Modifier.testTag("settings_preferences_group")) {
                    if (appMode != null && onAppModeChange != null) {
                        SettingsRow(
                            icon = Icons.Default.Groups,
                            iconTint = KiNDDIndigo,
                            title = stringResource(R.string.settings_app_mode),
                            trailingText = stringResource(
                                when (appMode) {
                                    AudienceType.FAMILY -> R.string.settings_app_mode_family
                                    AudienceType.CLINICIAN -> R.string.settings_app_mode_clinician
                                }
                            ),
                            onClick = { showAppModeSelection = true },
                            modifier = Modifier.testTag("settings_app_mode")
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 58.dp))
                    }
                    SettingsRow(
                        icon = Icons.Default.AccountBox,
                        iconTint = KiNDDPurple,
                        title = stringResource(R.string.settings_edit_profile),
                        subtitle = stringResource(R.string.settings_edit_profile_description),
                        onClick = onEditProfile,
                        modifier = Modifier.testTag("settings_edit_profile")
                    )
                    if (criteria != null && onApplySearchFilters != null) {
                        HorizontalDivider(modifier = Modifier.padding(start = 58.dp))
                        SettingsRow(
                            icon = Icons.Default.Tune,
                            iconTint = KiNDDIndigo,
                            title = stringResource(R.string.settings_search_filters),
                            trailingText = criteria.activeFilterCount()
                                .takeIf { it > 0 }
                                ?.let {
                                    pluralStringResource(
                                        R.plurals.settings_active_filter_count,
                                        it,
                                        it
                                    )
                                },
                            onClick = { showSearchFilters = true },
                            modifier = Modifier.testTag("settings_search_filters")
                        )
                    }
                    if (criteria != null && onDefaultRadiusChange != null) {
                        HorizontalDivider(modifier = Modifier.padding(start = 58.dp))
                        SettingsRow(
                            icon = Icons.Default.MyLocation,
                            iconTint = KiNDDViolet,
                            title = stringResource(R.string.settings_default_radius),
                            trailingText = pluralStringResource(
                                R.plurals.discovery_radius_option,
                                criteria.radiusMiles,
                                criteria.radiusMiles
                            ),
                            onClick = { showRadiusSelection = true },
                            modifier = Modifier.testTag("settings_default_radius")
                        )
                    }
                }
            }
            if (preferenceUpdateFailed) {
                item {
                    Text(
                        text = stringResource(R.string.settings_preference_update_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_preference_update_error")
                            .semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
            }

            item {
                SettingsSectionLabel(
                    text = stringResource(R.string.settings_location),
                    modifier = Modifier.padding(top = 6.dp),
                    testTag = "settings_location_heading"
                )
            }
            item {
                SettingsGroup(modifier = Modifier.testTag("settings_location_group")) {
                    SettingsRow(
                        icon = Icons.Default.LocationOn,
                        iconTint = KiNDDViolet,
                        title = stringResource(R.string.settings_location_access),
                        subtitle = locationStatus,
                        trailingText = if (onOpenLocationSettings == null) null else {
                            stringResource(R.string.settings_open)
                        },
                        onClick = onOpenLocationSettings,
                        modifier = Modifier.testTag("settings_location")
                    )
                    if (onOpenLocationSettings == null) {
                        Text(
                            text = stringResource(R.string.settings_location_unavailable),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 62.dp, end = 16.dp, bottom = 12.dp)
                        )
                    }
                }
            }

            item {
                SettingsSectionLabel(
                    text = stringResource(R.string.settings_information),
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            item {
                SettingsGroup(modifier = Modifier.testTag("settings_information_group")) {
                    SettingsRow(
                        icon = Icons.Default.Info,
                        iconTint = KiNDDIndigo,
                        title = stringResource(R.string.settings_about_kindd),
                        subtitle = stringResource(R.string.settings_about_description),
                        onClick = onNavigateToAbout,
                        modifier = Modifier.testTag("settings_about")
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 58.dp))
                    SettingsRow(
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        iconTint = KiNDDPurple,
                        title = stringResource(R.string.faq),
                        subtitle = stringResource(R.string.settings_faq_description),
                        onClick = onNavigateToFAQ,
                        modifier = Modifier.testTag("settings_faq")
                    )
                    if (onOpenWebsite != null) {
                        HorizontalDivider(modifier = Modifier.padding(start = 58.dp))
                        SettingsRow(
                            icon = Icons.Default.Public,
                            iconTint = KiNDDViolet,
                            title = stringResource(R.string.settings_website),
                            subtitle = stringResource(R.string.settings_website_description),
                            onClick = onOpenWebsite,
                            modifier = Modifier.testTag("settings_website")
                        )
                    }
                }
            }

            item {
                SettingsSectionLabel(
                    text = stringResource(R.string.settings_app_info),
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            item {
                SettingsGroup(modifier = Modifier.testTag("settings_app_info_group")) {
                    SettingsRow(
                        icon = Icons.Default.Info,
                        iconTint = KiNDDIndigo,
                        title = stringResource(R.string.version),
                        trailingText = appVersion
                    )
                }
            }

            item {
                SettingsSectionLabel(
                    text = stringResource(R.string.settings_reset),
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            item {
                SettingsGroup {
                    SettingsRow(
                        icon = Icons.Default.DeleteOutline,
                        iconTint = MaterialTheme.colorScheme.error,
                        title = stringResource(R.string.settings_clear_profile),
                        subtitle = stringResource(R.string.settings_clear_profile_description),
                        titleColor = MaterialTheme.colorScheme.error,
                        onClick = { showClearConfirmation = true },
                        modifier = Modifier.testTag("settings_clear_profile")
                    )
                }
            }
            if (clearFailed) {
                item {
                    Text(
                        text = stringResource(R.string.settings_clear_profile_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_clear_profile_error")
                            .semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
            }
            item {
                Text(
                    text = stringResource(R.string.settings_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 18.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }

    if (showAppModeSelection && appMode != null && onAppModeChange != null) {
        SettingsAppModeDialog(
            selected = appMode,
            onSelect = { selected ->
                showAppModeSelection = false
                onAppModeChange(selected)
            },
            onDismiss = { showAppModeSelection = false }
        )
    }

    if (showRadiusSelection && criteria != null && onDefaultRadiusChange != null) {
        SettingsRadiusDialog(
            selectedRadius = criteria.radiusMiles,
            onSelect = { selected ->
                showRadiusSelection = false
                onDefaultRadiusChange(selected)
            },
            onDismiss = { showRadiusSelection = false }
        )
    }

    if (showSearchFilters && criteria != null && onApplySearchFilters != null) {
        DiscoveryFilterSheet(
            criteria = criteria,
            onDismissRequest = { showSearchFilters = false },
            onApply = { selection ->
                showSearchFilters = false
                onApplySearchFilters(selection)
            }
        )
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            modifier = Modifier
                .testTag("settings_clear_confirmation")
                .semantics { liveRegion = LiveRegionMode.Polite },
            title = { Text(stringResource(R.string.settings_clear_profile_dialog_title)) },
            text = { Text(stringResource(R.string.settings_clear_profile_dialog_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation = false
                        onClearProfile()
                    },
                    modifier = Modifier.testTag("settings_confirm_clear")
                ) {
                    Text(
                        text = stringResource(R.string.settings_clear_profile_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SettingsAppModeDialog(
    selected: AudienceType,
    onSelect: (AudienceType) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_app_mode)) },
        text = {
            Column {
                SettingsChoiceRow(
                    label = stringResource(R.string.settings_app_mode_family),
                    selected = selected == AudienceType.FAMILY,
                    onClick = { onSelect(AudienceType.FAMILY) },
                    modifier = Modifier.testTag("settings_app_mode_family")
                )
                SettingsChoiceRow(
                    label = stringResource(R.string.settings_app_mode_clinician),
                    selected = selected == AudienceType.CLINICIAN,
                    onClick = { onSelect(AudienceType.CLINICIAN) },
                    modifier = Modifier.testTag("settings_app_mode_clinician")
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun SettingsRadiusDialog(
    selectedRadius: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_default_radius)) },
        text = {
            Column {
                listOf(5, 10, 15, 25, 50).forEach { radius ->
                    SettingsChoiceRow(
                        label = pluralStringResource(
                            R.plurals.discovery_radius_option,
                            radius,
                            radius
                        ),
                        selected = selectedRadius == radius,
                        onClick = { onSelect(radius) },
                        modifier = Modifier.testTag("settings_default_radius_$radius")
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun SettingsChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SettingsHeader(onNavigateBack: (() -> Unit)?) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (onNavigateBack != null) {
            Row(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(role = Role.Button, onClick = onNavigateBack)
                    .testTag("settings_back_to_more")
                    .semantics { role = Role.Button },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = KiNDDIndigo,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    text = stringResource(R.string.settings_back_to_more),
                    style = MaterialTheme.typography.titleMedium,
                    color = KiNDDIndigo
                )
            }
        }
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(top = if (onNavigateBack == null) 18.dp else 4.dp, bottom = 10.dp)
                .testTag("settings_title")
                .semantics { heading() }
        )
    }
}

@Composable
private fun SettingsSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 2.dp)
            .then(if (testTag == null) Modifier else Modifier.testTag(testTag))
            .semantics { heading() }
    )
}

@Composable
private fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(KiNDDShapeTokens.Card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailingText: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .then(
                if (onClick == null) Modifier
                else Modifier.clickable(role = Role.Button, onClick = onClick)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(
                    brush = Brush.linearGradient(
                        listOf(iconTint.copy(alpha = 0.18f), iconTint.copy(alpha = 0.08f))
                    ),
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(21.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = titleColor
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (!trailingText.isNullOrBlank()) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        if (onClick != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.64f)
            )
        }
    }
}

private fun Context.launchSafely(intent: Intent) {
    runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

private fun DiscoveryCriteria.activeFilterCount(): Int =
    therapyTypes.size +
        listOfNotNull(ageGroup, diagnosis, insurance).size
