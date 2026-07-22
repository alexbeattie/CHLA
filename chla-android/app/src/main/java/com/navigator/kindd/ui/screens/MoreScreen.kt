package com.navigator.kindd.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.navigator.kindd.BuildConfig
import com.navigator.kindd.R
import com.navigator.kindd.ui.theme.KiNDDIndigo
import com.navigator.kindd.ui.theme.KiNDDPurple
import com.navigator.kindd.ui.theme.KiNDDShapeTokens
import com.navigator.kindd.ui.theme.KiNDDSpacingTokens
import com.navigator.kindd.ui.theme.KiNDDViolet
import com.navigator.kindd.ui.theme.kinddTopWash

internal const val KINDD_WEBSITE_URL = "https://kinddhelp.org"
internal const val KINDD_PRIVACY_URL = "$KINDD_WEBSITE_URL/privacy"
internal const val KINDD_TERMS_URL = "$KINDD_WEBSITE_URL/terms"

@Composable
fun MoreScreen(
    onNavigateToFAQ: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToRegions: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    MoreContent(
        onNavigateToFAQ = onNavigateToFAQ,
        onNavigateToAbout = onNavigateToAbout,
        onNavigateToRegions = onNavigateToRegions,
        onOpenWebsite = { context.openWebsite(KINDD_WEBSITE_URL) },
        onOpenPrivacy = { context.openWebsite(KINDD_PRIVACY_URL) },
        onOpenTerms = { context.openWebsite(KINDD_TERMS_URL) },
        onNavigateToEditProfile = onNavigateToEditProfile,
        onNavigateToSettings = onNavigateToSettings
    )
}

@Composable
fun MoreContent(
    onNavigateToFAQ: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToRegions: () -> Unit,
    onOpenWebsite: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenTerms: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    versionName: String = BuildConfig.VERSION_NAME
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("more_grouped_canvas")
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
                .testTag("more_list"),
            contentPadding = PaddingValues(
                start = KiNDDSpacingTokens.PageInset,
                top = 20.dp,
                end = KiNDDSpacingTokens.PageInset,
                bottom = 34.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.more_title),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .testTag("more_title")
                        .semantics { heading() }
                )
            }
            item { MoreSectionLabel(stringResource(R.string.more_help)) }
            item {
                MoreGroup {
                    MoreRow(
                        icon = Icons.AutoMirrored.Filled.HelpOutline,
                        iconTint = KiNDDIndigo,
                        title = stringResource(R.string.faq),
                        subtitle = stringResource(R.string.more_faq_description),
                        onClick = onNavigateToFAQ,
                        modifier = Modifier.testTag("more_faq")
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 58.dp))
                    MoreRow(
                        icon = Icons.Default.Info,
                        iconTint = KiNDDPurple,
                        title = stringResource(R.string.settings_about_kindd),
                        subtitle = stringResource(R.string.more_about_description),
                        onClick = onNavigateToAbout,
                        modifier = Modifier.testTag("more_about")
                    )
                }
            }
            item { MoreSectionLabel(stringResource(R.string.more_explore)) }
            item {
                MoreGroup {
                    MoreRow(
                        icon = Icons.Default.Apartment,
                        iconTint = KiNDDPurple,
                        title = stringResource(R.string.regional_centers),
                        subtitle = stringResource(R.string.more_regions_description),
                        onClick = onNavigateToRegions,
                        modifier = Modifier.testTag("more_regions")
                    )
                }
            }
            item { MoreSectionLabel(stringResource(R.string.more_links)) }
            item {
                MoreGroup {
                    MoreRow(
                        icon = Icons.Default.Public,
                        iconTint = KiNDDViolet,
                        title = stringResource(R.string.more_website),
                        subtitle = stringResource(R.string.more_website_description),
                        onClick = onOpenWebsite,
                        modifier = Modifier.testTag("more_website")
                    )
                }
            }
            item { MoreSectionLabel(stringResource(R.string.more_legal)) }
            item {
                MoreGroup {
                    MoreRow(
                        icon = Icons.Default.PrivacyTip,
                        iconTint = KiNDDIndigo,
                        title = stringResource(R.string.privacy_policy),
                        subtitle = stringResource(R.string.more_privacy_description),
                        onClick = onOpenPrivacy,
                        modifier = Modifier.testTag("more_privacy")
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 58.dp))
                    MoreRow(
                        icon = Icons.Default.Description,
                        iconTint = KiNDDViolet,
                        title = stringResource(R.string.terms_of_service),
                        subtitle = stringResource(R.string.more_terms_description),
                        onClick = onOpenTerms,
                        modifier = Modifier.testTag("more_terms")
                    )
                }
            }
            item { MoreSectionLabel(stringResource(R.string.more_profile)) }
            item {
                MoreGroup {
                    MoreRow(
                        icon = Icons.Default.RestartAlt,
                        iconTint = KiNDDIndigo,
                        title = stringResource(R.string.more_restart_setup),
                        subtitle = stringResource(R.string.more_restart_setup_description),
                        onClick = onNavigateToEditProfile,
                        modifier = Modifier.testTag("more_edit_profile")
                    )
                    HorizontalDivider(modifier = Modifier.padding(start = 58.dp))
                    MoreRow(
                        icon = Icons.Default.Settings,
                        iconTint = KiNDDPurple,
                        title = stringResource(R.string.settings_title),
                        subtitle = stringResource(R.string.more_settings_description),
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("more_settings")
                    )
                }
            }
            item { MoreSectionLabel(stringResource(R.string.more_app_info)) }
            item {
                MoreGroup {
                    MoreRow(
                        icon = Icons.Default.Info,
                        iconTint = KiNDDIndigo,
                        title = stringResource(R.string.version),
                        trailingText = versionName,
                        modifier = Modifier.testTag("more_version")
                    )
                }
            }
            item {
                Text(
                    text = stringResource(R.string.more_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 18.dp)
                )
            }
        }
    }
}

@Composable
private fun MoreSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 2.dp)
    )
}

@Composable
private fun MoreGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(KiNDDShapeTokens.Card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun MoreRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailingText: String? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .then(
                if (onClick == null) Modifier
                else Modifier
                    .clickable(role = Role.Button, onClick = onClick)
                    .semantics { role = Role.Button }
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
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
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
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.64f)
            )
        }
    }
}

private fun Context.openWebsite(url: String) {
    runCatching {
        startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
