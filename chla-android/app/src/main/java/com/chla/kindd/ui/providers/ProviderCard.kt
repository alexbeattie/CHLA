package com.chla.kindd.ui.providers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chla.kindd.R
import com.chla.kindd.data.discovery.TherapyType
import com.chla.kindd.data.models.Provider
import com.chla.kindd.ui.theme.KiNDDCardSurface
import com.chla.kindd.ui.theme.KiNDDDeepIndigo
import com.chla.kindd.ui.theme.KiNDDIndigo
import com.chla.kindd.ui.theme.KiNDDPink
import com.chla.kindd.ui.theme.KiNDDPurple
import com.chla.kindd.ui.theme.KiNDDViolet

private const val MAX_VISIBLE_THERAPIES = 3

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProviderCard(
    provider: Provider,
    onClick: () -> Unit,
    onPhoneClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    KiNDDCardSurface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("provider_${provider.id}"),
        onClick = onClick,
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ProviderHeader(provider)

            provider.displayAddressLines.take(2).takeIf { it.isNotEmpty() }?.let { lines ->
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(top = 1.dp)
                            .size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        lines.forEachIndexed { index, line ->
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag(
                                    "provider_address_${provider.id}_$index"
                                )
                            )
                        }
                    }
                }
            }

            val therapies = provider.therapyTypes.orEmpty()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .distinctBy { it.lowercase() }
            if (therapies.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    therapies.take(MAX_VISIBLE_THERAPIES).forEachIndexed { index, therapy ->
                        TherapyTag(
                            providerId = provider.id,
                            index = index,
                            therapy = therapy
                        )
                    }
                    val overflow = therapies.size - MAX_VISIBLE_THERAPIES
                    if (overflow > 0) {
                        ProviderCapsule(
                            text = stringResource(R.string.provider_more_services, overflow),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.testTag(
                                "provider_therapy_overflow_${provider.id}"
                            )
                        )
                    }
                }
            }

            val formattedPhone = provider.formattedPhone.takeIf(String::isNotBlank)
            val regionalCenter = provider.regionalCenter?.trim()?.takeIf(String::isNotEmpty)
            if (formattedPhone != null || regionalCenter != null) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (formattedPhone != null) {
                        Row(
                            modifier = Modifier
                                .widthIn(min = 48.dp)
                                .height(48.dp)
                                .then(
                                    if (onPhoneClick == null) Modifier else Modifier.clickable(
                                        role = Role.Button,
                                        onClick = onPhoneClick
                                    )
                                )
                                .testTag("provider_phone_${provider.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = KiNDDIndigo,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = formattedPhone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }
                    if (regionalCenter != null) {
                        ProviderCapsule(
                            text = regionalCenter,
                            color = Color.White,
                            containerColor = KiNDDViolet,
                            modifier = Modifier
                                .widthIn(max = 176.dp)
                                .testTag("provider_regional_center_${provider.id}")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderHeader(provider: Provider) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = provider.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            provider.type?.trim()?.takeIf(String::isNotEmpty)?.let { type ->
                ProviderCapsule(
                    text = type,
                    color = KiNDDDeepIndigo,
                    containerColor = KiNDDIndigo.copy(alpha = 0.12f),
                    modifier = Modifier.testTag("provider_type_${provider.id}")
                )
            }
        }
        provider.distance?.let {
            Spacer(Modifier.width(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = KiNDDIndigo.copy(alpha = 0.10f),
                contentColor = KiNDDDeepIndigo,
                modifier = Modifier.testTag("provider_distance_${provider.id}")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = provider.formattedDistance,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun TherapyTag(providerId: String, index: Int, therapy: String) {
    val type = TherapyType.entries.firstOrNull {
        it.apiValue.equals(therapy, ignoreCase = true)
    }
    val color = therapyRoleColor(therapy)
    ProviderCapsule(
        text = type?.let { stringResource(it.displayResId) } ?: therapy,
        color = color,
        containerColor = color.copy(alpha = 0.11f),
        modifier = Modifier.testTag("provider_therapy_${providerId}_$index")
    )
}

@Composable
private fun ProviderCapsule(
    text: String,
    color: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        color = containerColor,
        contentColor = color
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

private fun therapyRoleColor(therapy: String): Color {
    val value = therapy.lowercase()
    return when {
        "aba" in value -> KiNDDIndigo
        "speech" in value -> KiNDDPink
        "occupational" in value -> KiNDDViolet
        "physical" in value -> KiNDDPurple
        else -> Color(0xFF667085)
    }
}
