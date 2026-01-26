package com.chla.kindd.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.chla.kindd.R
import com.chla.kindd.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProviderDetailScreen(
    providerId: String,
    onBack: () -> Unit,
    viewModel: ProviderDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(providerId) {
        viewModel.loadProvider(providerId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.provider_details)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CHLABlue,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.provider != null -> {
                val provider = uiState.provider!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = CHLABlue.copy(alpha = 0.05f)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = provider.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (provider.distance != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(R.string.distance_away, provider.formattedDistance),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CHLABlueLight
                                )
                            }
                        }
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (provider.phone != null) {
                            FilledTonalButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${provider.phone}")
                                    }
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.call_now))
                            }
                        }
                        if (provider.hasCoordinates) {
                            Button(
                                onClick = {
                                    val uri = Uri.parse("google.navigation:q=${provider.latitude},${provider.longitude}")
                                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                        setPackage("com.google.android.apps.maps")
                                    }
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Directions, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.get_directions))
                            }
                        }
                    }

                    Divider()

                    // Contact Info
                    if (provider.fullAddress.isNotEmpty() || provider.phone != null || provider.website != null) {
                        SectionHeader(stringResource(R.string.contact))
                        
                        if (provider.fullAddress.isNotEmpty()) {
                            ContactItem(
                                icon = Icons.Default.LocationOn,
                                label = stringResource(R.string.address),
                                value = provider.fullAddress
                            )
                        }
                        if (provider.phone != null) {
                            ContactItem(
                                icon = Icons.Default.Phone,
                                label = stringResource(R.string.phone),
                                value = provider.formattedPhone,
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:${provider.phone}")
                                    }
                                    context.startActivity(intent)
                                }
                            )
                        }
                        if (provider.website != null) {
                            ContactItem(
                                icon = Icons.Default.Language,
                                label = stringResource(R.string.website),
                                value = stringResource(R.string.visit_website),
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse(provider.website)
                                    }
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }

                    // Services
                    if (!provider.therapyTypes.isNullOrEmpty()) {
                        Divider()
                        SectionHeader(stringResource(R.string.services))
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            provider.therapyTypes.forEach { service ->
                                AssistChip(
                                    onClick = { },
                                    label = { Text(service) }
                                )
                            }
                        }
                    }

                    // Insurance
                    if (!provider.insuranceAccepted.isNullOrEmpty()) {
                        Divider(modifier = Modifier.padding(top = 16.dp))
                        SectionHeader(stringResource(R.string.insurance_accepted))
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            provider.insuranceAccepted.forEach { insurance ->
                                AssistChip(
                                    onClick = { },
                                    label = { Text(insurance) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = Success.copy(alpha = 0.1f)
                                    )
                                )
                            }
                        }
                    }

                    // Age Groups
                    if (!provider.ageGroups.isNullOrEmpty()) {
                        Divider(modifier = Modifier.padding(top = 16.dp))
                        SectionHeader(stringResource(R.string.age_groups))
                        FlowRow(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            provider.ageGroups.forEach { ageGroup ->
                                AssistChip(
                                    onClick = { },
                                    label = { Text(ageGroup) },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = Warning.copy(alpha = 0.1f)
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.error_loading))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun ContactItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.padding(0.dp) else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CHLABlue,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = if (onClick != null) CHLABlueLight else MaterialTheme.colorScheme.onSurface
            )
        }
        if (onClick != null) {
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null
                )
            }
        }
    }
}
