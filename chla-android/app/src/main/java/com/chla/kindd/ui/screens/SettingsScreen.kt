package com.chla.kindd.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chla.kindd.BuildConfig
import com.chla.kindd.R
import com.chla.kindd.ui.settings.SettingsEvent
import com.chla.kindd.ui.settings.SettingsViewModel

@Composable
fun SettingsScreen(
    onNavigateToFAQ: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToEditProfile: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val editNavigator = remember(onNavigateToEditProfile) { onNavigateToEditProfile }
    LaunchedEffect(viewModel, editNavigator) {
        viewModel.events.collect { event ->
            when (event) {
                SettingsEvent.NavigateToEditProfile -> editNavigator()
            }
        }
    }

    SettingsContent(
        onNavigateToFAQ = onNavigateToFAQ,
        onNavigateToAbout = onNavigateToAbout,
        onEditProfile = viewModel::editProfile,
        onClearProfile = viewModel::clearProfile
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    onNavigateToFAQ: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onEditProfile: () -> Unit,
    onClearProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearConfirmation by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        modifier = Modifier
                            .testTag("settings_title")
                            .semantics { heading() }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SettingsSectionHeading(
                    title = stringResource(R.string.settings_profile),
                    testTag = "settings_profile_heading"
                )
            }
            item {
                SettingsActionCard(
                    icon = Icons.Default.Person,
                    title = stringResource(R.string.settings_edit_profile),
                    subtitle = stringResource(R.string.settings_edit_profile_description),
                    onClick = onEditProfile,
                    modifier = Modifier.testTag("settings_edit_profile")
                )
            }
            item {
                SettingsActionCard(
                    icon = Icons.Default.RestartAlt,
                    title = stringResource(R.string.settings_clear_profile),
                    subtitle = stringResource(R.string.settings_clear_profile_description),
                    onClick = { showClearConfirmation = true },
                    modifier = Modifier.testTag("settings_clear_profile"),
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            item {
                SettingsSectionHeading(
                    title = stringResource(R.string.about),
                    testTag = "settings_about_heading"
                )
            }
            item {
                SettingsActionCard(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.about),
                    onClick = onNavigateToAbout
                )
            }
            item {
                SettingsActionCard(
                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                    title = stringResource(R.string.faq),
                    onClick = onNavigateToFAQ
                )
            }
            item {
                SettingsActionCard(
                    icon = Icons.Default.Policy,
                    title = stringResource(R.string.privacy_policy),
                    onClick = { }
                )
            }
            item {
                SettingsActionCard(
                    icon = Icons.Default.Description,
                    title = stringResource(R.string.terms_of_service),
                    onClick = { }
                )
            }
            item {
                SettingsSectionHeading(
                    title = stringResource(R.string.settings_app_info),
                    testTag = "settings_app_info_heading"
                )
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.version),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = BuildConfig.VERSION_NAME,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                Text(
                    text = stringResource(R.string.settings_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                )
            }
        }
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
private fun SettingsSectionHeading(title: String, testTag: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp)
            .testTag(testTag)
            .semantics { heading() }
    )
}

@Composable
private fun SettingsActionCard(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor.copy(alpha = 0.78f)
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = contentColor
            )
        }
    }
}
