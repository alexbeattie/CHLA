package com.navigator.kindd.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.navigator.kindd.R
import com.navigator.kindd.data.profile.UserProfile
import com.navigator.kindd.ui.home.HomeLookupState
import com.navigator.kindd.ui.home.HomeMessage
import com.navigator.kindd.ui.home.HomeUiState
import com.navigator.kindd.ui.map.RegionalCenterMapSurface
import com.navigator.kindd.ui.map.RegionalCenterMapRenderModel
import com.navigator.kindd.ui.map.mapAttributionBottomPaddingDp
import com.navigator.kindd.ui.theme.KiNDDIndigo
import com.navigator.kindd.ui.theme.KiNDDPink
import com.navigator.kindd.ui.theme.KiNDDShapeTokens
import com.navigator.kindd.ui.theme.KiNDDViolet

@Composable
fun HomeMapHero(
    profile: UserProfile,
    uiState: HomeUiState,
    onZipChanged: (String) -> Unit,
    onSubmitZip: () -> Unit,
    onExplore: () -> Unit,
    onDetails: () -> Unit,
    onCall: (String) -> Unit,
    modifier: Modifier = Modifier,
    mapContent: (@Composable (RegionalCenterMapRenderModel, (String) -> Unit) -> Unit)? = null,
    onCenterRoleTextLayout: (TextLayoutResult) -> Unit = {}
) {
    val identity = profile.regionalCenter
    val density = LocalDensity.current
    var overlayCardHeightPx by remember(identity != null) { mutableIntStateOf(0) }
    val mapContentPadding = PaddingValues(
        bottom = mapAttributionBottomPaddingDp(
            overlayHeightPx = overlayCardHeightPx,
            density = density.density,
            overlaysMap = true
        ).dp
    )
    val summary = identity?.let {
        stringResource(R.string.home_parity_map_summary, it.name)
    } ?: stringResource(R.string.home_parity_map_summary_unmatched)
    val heroShape = RoundedCornerShape(KiNDDShapeTokens.Hero)

    val heroHeight = homeMapHeroHeightDp(
        availableWidthDp = LocalConfiguration.current.screenWidthDp,
        fontScale = density.fontScale
    ).dp
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heroHeight)
            .shadow(18.dp, heroShape, ambientColor = KiNDDIndigo.copy(alpha = 0.12f))
            .clip(heroShape)
            .background(
                Brush.linearGradient(
                    listOf(
                        KiNDDIndigo.copy(alpha = 0.34f),
                        KiNDDPink.copy(alpha = 0.20f)
                    )
                )
            )
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.10f), heroShape)
            .testTag("home_map_hero")
            .semantics { contentDescription = summary }
    ) {
        RegionalCenterMapSurface(
            areas = uiState.serviceAreas,
            highlightedAcronym = identity?.shortName,
            interactive = false,
            onAreaClick = {},
            contentPadding = mapContentPadding,
            mapContent = mapContent,
            modifier = Modifier
                .fillMaxSize()
                .clearAndSetSemantics { }
        )
        if (uiState.serviceAreaLoadState == com.navigator.kindd.ui.home.ServiceAreaLoadState.LOADING) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Button(
            onClick = onExplore,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(percent = 50),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 13.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .heightIn(min = 48.dp)
                .testTag("home_explore")
        ) {
            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.home_explore), fontWeight = FontWeight.SemiBold)
        }

        if (identity == null) {
            HomeZipOverlayCard(
                profile = profile,
                uiState = uiState,
                onZipChanged = onZipChanged,
                onSubmitZip = onSubmitZip,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(10.dp)
                    .onSizeChanged { overlayCardHeightPx = it.height }
            )
        } else {
            HomeMatchedCenterOverlayCard(
                profile = profile,
                uiState = uiState,
                onDetails = onDetails,
                onCall = onCall,
                onCenterRoleTextLayout = onCenterRoleTextLayout,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(10.dp)
                    .onSizeChanged { overlayCardHeightPx = it.height }
            )
        }
    }
}

@Composable
private fun HomeMatchedCenterOverlayCard(
    profile: UserProfile,
    uiState: HomeUiState,
    onDetails: () -> Unit,
    onCall: (String) -> Unit,
    onCenterRoleTextLayout: (TextLayoutResult) -> Unit,
    modifier: Modifier = Modifier
) {
    val identity = profile.regionalCenter ?: return
    val roleColor = regionalCenterRoleColor(identity.shortName)
    val cardShape = RoundedCornerShape(KiNDDShapeTokens.Card)
    val formattedPhone = uiState.formattedPhoneFor(profile)
    val dialDigits = uiState.dialDigitsFor(profile)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, cardShape, ambientColor = roleColor.copy(alpha = 0.16f))
            .clip(cardShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.97f))
            .background(
                Brush.verticalGradient(
                    listOf(roleColor.copy(alpha = 0.09f), Color.Transparent)
                )
            )
            .border(1.dp, roleColor.copy(alpha = 0.22f), cardShape)
            .testTag("home_matched_center_card")
    ) {
        val fontScale = LocalDensity.current.fontScale
        val stackActions = shouldStackMatchedCenterActions(maxWidth.value.toInt(), fontScale)
        val cardPadding = if (stackActions) 12.dp else 18.dp
        val contentSpacing = if (stackActions) 6.dp else 9.dp

        Column(
            modifier = Modifier.padding(cardPadding),
            verticalArrangement = Arrangement.spacedBy(contentSpacing)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.home_your_regional_center).uppercase(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = androidx.compose.ui.unit.TextUnit(0.8f, androidx.compose.ui.unit.TextUnitType.Sp)
                )
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    stringResource(R.string.home_matched),
                    color = Color(0xFF10B981),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = identity.name,
                modifier = Modifier.semantics { heading() },
                style = if (stackActions) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.titleLarge
                },
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.home_parity_center_role),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                onTextLayout = onCenterRoleTextLayout,
                modifier = Modifier.testTag("home_center_role")
            )
            if (stackActions) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (dialDigits != null && !formattedPhone.isNullOrBlank()) {
                        MatchedCenterPhoneAction(
                            formattedPhone = formattedPhone,
                            roleColor = roleColor,
                            onClick = { onCall(dialDigits) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    MatchedCenterDetailsAction(
                        roleColor = roleColor,
                        onClick = onDetails,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (dialDigits != null && !formattedPhone.isNullOrBlank()) {
                        MatchedCenterPhoneAction(
                            formattedPhone = formattedPhone,
                            roleColor = roleColor,
                            onClick = { onCall(dialDigits) }
                        )
                    }
                    MatchedCenterDetailsAction(roleColor = roleColor, onClick = onDetails)
                }
            }
        }
    }
}

@Composable
private fun MatchedCenterPhoneAction(
    formattedPhone: String,
    roleColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(percent = 50),
        colors = ButtonDefaults.buttonColors(containerColor = roleColor),
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(7.dp))
        Text(formattedPhone, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MatchedCenterDetailsAction(
    roleColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(percent = 50),
        border = BorderStroke(1.dp, roleColor.copy(alpha = 0.45f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = roleColor),
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        Text(stringResource(R.string.home_details), fontWeight = FontWeight.SemiBold)
    }
}

internal fun shouldStackMatchedCenterActions(availableWidthDp: Int, fontScale: Float): Boolean =
    availableWidthDp <= 340 || fontScale >= 1.3f

internal fun homeMapHeroHeightDp(availableWidthDp: Int, fontScale: Float): Int =
    if (availableWidthDp <= 340 && fontScale >= 1.3f) 420 else 340

@Composable
private fun HomeZipOverlayCard(
    profile: UserProfile,
    uiState: HomeUiState,
    onZipChanged: (String) -> Unit,
    onSubmitZip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(KiNDDShapeTokens.Card)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(16.dp, shape, ambientColor = KiNDDIndigo.copy(alpha = 0.12f))
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.97f))
            .border(1.dp, KiNDDIndigo.copy(alpha = 0.16f), shape)
            .padding(16.dp)
            .testTag("home_zip_center_card"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            stringResource(R.string.home_who_serves),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            stringResource(R.string.home_parity_zip_explanation),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = uiState.displayedZip(profile),
                onValueChange = onZipChanged,
                placeholder = { Text(stringResource(R.string.onboarding_zip_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(onSearch = { onSubmitZip() }),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("home_zip_input")
            )
            Button(
                onClick = onSubmitZip,
                enabled = uiState.displayedZip(profile).matches(Regex("[0-9]{5}")) &&
                    uiState.lookupState != HomeLookupState.LOADING,
                shape = RoundedCornerShape(percent = 50),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                if (uiState.lookupState == HomeLookupState.LOADING) {
                    val loadingDescription = stringResource(R.string.loading)
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .size(20.dp)
                            .testTag("home_zip_lookup_loading")
                            .semantics {
                                contentDescription = loadingDescription
                                liveRegion = LiveRegionMode.Polite
                            }
                    )
                } else {
                    Text(stringResource(R.string.home_find))
                }
            }
        }
        uiState.message?.let { message ->
            Text(
                text = stringResource(
                    when (message) {
                        HomeMessage.INVALID_ZIP -> R.string.home_invalid_zip
                        HomeMessage.NO_MATCH -> R.string.home_no_match
                        HomeMessage.LOOKUP_UNAVAILABLE -> R.string.home_lookup_unavailable
                    }
                ),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .testTag("home_zip_lookup_message")
                    .semantics { liveRegion = LiveRegionMode.Polite }
            )
        }
    }
}

private fun regionalCenterRoleColor(acronym: String): Color = when (
    acronym.trim().uppercase().replace("/", "")
) {
    "NLACRC" -> Color(0xFFD9A621)
    "FDLRC" -> Color(0xFF9966B3)
    "HRC" -> Color(0xFF3399DB)
    "SCLARC" -> Color(0xFFF28C33)
    "ELARC" -> Color(0xFF4DBF73)
    "WRC" -> Color(0xFFE64D80)
    "SGPRC" -> Color(0xFF338C59)
    else -> KiNDDViolet
}
