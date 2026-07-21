package com.chla.kindd.ui.regions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chla.kindd.R
import com.chla.kindd.data.models.RegionalCenter
import com.chla.kindd.data.servicearea.ServiceAreaFeature
import com.chla.kindd.platform.launchDialer
import com.chla.kindd.platform.launchWebsite
import com.chla.kindd.ui.map.RegionalCenterMapRenderModel
import com.chla.kindd.ui.screens.RegionalCentersLookupState
import com.chla.kindd.ui.screens.RegionalCentersMessage
import com.chla.kindd.ui.screens.RegionalCentersUiState
import com.chla.kindd.ui.theme.KiNDDCompactIconAction
import com.chla.kindd.ui.theme.KiNDDGlassSurface
import com.chla.kindd.ui.theme.KiNDDIndigo
import com.chla.kindd.ui.theme.KiNDDMatchedGreen
import com.chla.kindd.ui.theme.KiNDDPrimaryGradientCapsule
import com.chla.kindd.ui.theme.KiNDDShapeTokens

private enum class RegionsMode { MAP, LIST }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegionalCentersContent(
    uiState: RegionalCentersUiState,
    serviceAreaState: RegionalCenterServiceAreaState,
    onBack: () -> Unit,
    onZipChanged: (String) -> Unit,
    onSubmitZip: () -> Unit,
    modifier: Modifier = Modifier,
    mapContent: (@Composable (RegionalCenterMapRenderModel, (String) -> Unit) -> Unit)? = null
) {
    var modeName by rememberSaveable { mutableStateOf(RegionsMode.MAP.name) }
    val mode = RegionsMode.valueOf(modeName)
    var selectedCenter by remember { mutableStateOf<RegionalCenter?>(null) }
    val selectAcronym: (String) -> Unit = { acronym ->
        selectedCenter = uiState.centers.firstOrNull { center ->
            center.shortName.canonicalAcronym() == acronym.canonicalAcronym()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (mode) {
            RegionsMode.MAP -> RegionsMapMode(
                uiState = uiState,
                serviceAreaState = serviceAreaState,
                selectedAcronym = selectedCenter?.shortName ?: uiState.matchedCenter?.shortName,
                onAreaSelected = selectAcronym,
                onModeSelected = { modeName = it.name },
                onBack = onBack,
                onZipChanged = onZipChanged,
                onSubmitZip = onSubmitZip,
                onMatchedCenterSelected = { selectedCenter = it },
                mapContent = mapContent
            )
            RegionsMode.LIST -> RegionsList(
                uiState = uiState,
                onModeSelected = { modeName = it.name },
                onBack = onBack,
                onZipChanged = onZipChanged,
                onSubmitZip = onSubmitZip,
                onMatchedCenterSelected = { selectedCenter = it },
                onCenterSelected = { selectedCenter = it }
            )
        }
    }

    selectedCenter?.let { center ->
        ModalBottomSheet(onDismissRequest = { selectedCenter = null }) {
            RegionalCenterDetail(
                center = center,
                onDismiss = { selectedCenter = null }
            )
        }
    }
}

@Composable
private fun RegionsMapMode(
    uiState: RegionalCentersUiState,
    serviceAreaState: RegionalCenterServiceAreaState,
    selectedAcronym: String?,
    onAreaSelected: (String) -> Unit,
    onModeSelected: (RegionsMode) -> Unit,
    onBack: () -> Unit,
    onZipChanged: (String) -> Unit,
    onSubmitZip: () -> Unit,
    onMatchedCenterSelected: (RegionalCenter) -> Unit,
    mapContent: (@Composable (RegionalCenterMapRenderModel, (String) -> Unit) -> Unit)?
) {
    when (serviceAreaState) {
        is RegionalCenterServiceAreaState.Success -> Box(Modifier.fillMaxSize()) {
            RegionsMap(
                serviceAreaState = serviceAreaState,
                highlightedAcronym = selectedAcronym,
                onAreaSelected = onAreaSelected,
                mapContent = mapContent
            )
            RegionsControls(
                uiState = uiState,
                mode = RegionsMode.MAP,
                onModeSelected = onModeSelected,
                onBack = onBack,
                onZipChanged = onZipChanged,
                onSubmitZip = onSubmitZip,
                onMatchedCenterSelected = onMatchedCenterSelected,
                modifier = Modifier.align(Alignment.TopCenter)
            )
            RegionsLegend(
                serviceAreas = serviceAreaState.areas,
                selectedAcronym = selectedAcronym,
                onAreaSelected = onAreaSelected,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            )
        }
        RegionalCenterServiceAreaState.Loading,
        RegionalCenterServiceAreaState.Error -> Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
        ) {
            RegionsControls(
                uiState = uiState,
                mode = RegionsMode.MAP,
                onModeSelected = onModeSelected,
                onBack = onBack,
                onZipChanged = onZipChanged,
                onSubmitZip = onSubmitZip,
                onMatchedCenterSelected = onMatchedCenterSelected
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 18.dp, vertical = 10.dp)
                    .fillMaxWidth()
                    .height(340.dp)
                    .clip(RoundedCornerShape(KiNDDShapeTokens.Hero))
            ) {
                RegionsMap(
                    serviceAreaState = serviceAreaState,
                    highlightedAcronym = selectedAcronym,
                    onAreaSelected = onAreaSelected,
                    mapContent = mapContent
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RegionsMap(
    serviceAreaState: RegionalCenterServiceAreaState,
    highlightedAcronym: String?,
    onAreaSelected: (String) -> Unit,
    mapContent: (@Composable (RegionalCenterMapRenderModel, (String) -> Unit) -> Unit)?
) {
    Box(modifier = Modifier.fillMaxSize().testTag("regions_map")) {
        RegionalCenterServiceAreaMap(
            state = serviceAreaState,
            highlightedAcronym = highlightedAcronym,
            interactive = true,
            onAreaClick = onAreaSelected,
            modifier = Modifier.fillMaxSize(),
            mapContent = mapContent
        )
    }
}

@Composable
private fun RegionsList(
    uiState: RegionalCentersUiState,
    onModeSelected: (RegionsMode) -> Unit,
    onBack: () -> Unit,
    onZipChanged: (String) -> Unit,
    onSubmitZip: () -> Unit,
    onMatchedCenterSelected: (RegionalCenter) -> Unit,
    onCenterSelected: (RegionalCenter) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("regions_list"),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "controls") {
            RegionsControls(
                uiState = uiState,
                mode = RegionsMode.LIST,
                onModeSelected = onModeSelected,
                onBack = onBack,
                onZipChanged = onZipChanged,
                onSubmitZip = onSubmitZip,
                onMatchedCenterSelected = onMatchedCenterSelected
            )
        }
        if (uiState.isLoading && uiState.centers.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        items(uiState.centers, key = RegionalCenter::id) { center ->
            RegionalCenterListCard(
                center = center,
                onClick = { onCenterSelected(center) },
                modifier = Modifier.padding(horizontal = 18.dp)
            )
        }
    }
}

@Composable
private fun RegionsControls(
    uiState: RegionalCentersUiState,
    mode: RegionsMode,
    onModeSelected: (RegionsMode) -> Unit,
    onBack: () -> Unit,
    onZipChanged: (String) -> Unit,
    onSubmitZip: () -> Unit,
    onMatchedCenterSelected: (RegionalCenter) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("regions_controls")
            .statusBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KiNDDGlassSurface(shape = CircleShape) {
                KiNDDCompactIconAction(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    onClick = onBack
                )
            }
            KiNDDGlassSurface(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.regional_centers_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        KiNDDGlassSurface(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(8.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.zipDraft,
                        onValueChange = onZipChanged,
                        modifier = Modifier.weight(1f).testTag("regions_zip_input"),
                        placeholder = { Text(stringResource(R.string.regions_zip_placeholder)) },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(KiNDDShapeTokens.Compact),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(onSearch = { onSubmitZip() })
                    )
                    val submitEnabled = uiState.zipDraft.matches(Regex("[0-9]{5}")) &&
                        uiState.lookupState != RegionalCentersLookupState.LOADING
                    KiNDDPrimaryGradientCapsule(
                        onClick = { if (submitEnabled) onSubmitZip() },
                        modifier = Modifier
                            .testTag("regions_zip_submit")
                            .semantics { if (!submitEnabled) disabled() },
                        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 12.dp)
                    ) {
                        if (uiState.lookupState == RegionalCentersLookupState.LOADING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.search),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                        }
                    }
                }

                RegionsModeToggle(mode = mode, onModeSelected = onModeSelected)

                uiState.message?.let { message ->
                    Text(
                        text = stringResource(message.stringResource()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }

                uiState.matchedCenter?.let { center ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(KiNDDMatchedGreen.copy(alpha = 0.11f))
                            .clickable(role = Role.Button) { onMatchedCenterSelected(center) }
                            .testTag("regions_zip_result")
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = KiNDDMatchedGreen
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.zip_served_by),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = center.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RegionsModeToggle(
    mode: RegionsMode,
    onModeSelected: (RegionsMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f),
                RoundedCornerShape(14.dp)
            )
            .padding(4.dp)
            .testTag("regions_mode_toggle"),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        RegionModeSegment(
            label = stringResource(R.string.nav_map),
            selected = mode == RegionsMode.MAP,
            onClick = { onModeSelected(RegionsMode.MAP) },
            testTag = "regions_mode_map",
            modifier = Modifier.weight(1f)
        )
        RegionModeSegment(
            label = stringResource(R.string.nav_list),
            selected = mode == RegionsMode.LIST,
            onClick = { onModeSelected(RegionsMode.LIST) },
            testTag = "regions_mode_list",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun RegionModeSegment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(11.dp)
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent, shape)
            .then(
                if (selected) Modifier.border(1.dp, KiNDDIndigo.copy(alpha = 0.32f), shape)
                else Modifier
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) KiNDDIndigo else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RegionsLegend(
    serviceAreas: List<ServiceAreaFeature>,
    selectedAcronym: String?,
    onAreaSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    KiNDDGlassSurface(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(8.dp)
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            serviceAreas.forEach { area ->
                val selected = area.acronym.canonicalAcronym() == selectedAcronym?.canonicalAcronym()
                Surface(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("regions_area_${area.acronym.canonicalAcronym()}"),
                    onClick = { onAreaSelected(area.acronym) },
                    shape = RoundedCornerShape(percent = 50),
                    color = if (selected) KiNDDIndigo.copy(alpha = 0.14f) else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .background(
                                    ui.regionalCenterColor(area.name),
                                    CircleShape
                                )
                        )
                        Text(
                            text = area.acronym,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RegionalCenterListCard(
    center: RegionalCenter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .testTag("regions_center_${center.shortName.canonicalAcronym()}"),
        onClick = onClick,
        shape = RoundedCornerShape(KiNDDShapeTokens.Card),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(center.color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = center.shortName.take(2),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = center.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                center.countyServed?.takeIf(String::isNotBlank)?.let { county ->
                    Text(
                        text = county,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RegionalCenterDetail(center: RegionalCenter, onDismiss: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("regions_center_detail")
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(64.dp)
                    .background(center.color, RoundedCornerShape(4.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = center.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = center.shortName,
                    style = MaterialTheme.typography.labelLarge,
                    color = center.color
                )
                center.countyServed?.takeIf(String::isNotBlank)?.let { county ->
                    Text(
                        text = county,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        center.fullAddress.takeIf(String::isNotBlank)?.let { address ->
            DetailRow(icon = Icons.Default.LocationOn, text = address, tint = center.color)
        }
        center.telephone?.takeIf(String::isNotBlank)?.let { phone ->
            DetailRow(
                icon = Icons.Default.Phone,
                text = center.formattedPhone,
                tint = center.color,
                testTag = "regions_detail_phone",
                onClick = {
                    context.launchDialer(phone)
                }
            )
        }
        center.website?.takeIf(String::isNotBlank)?.let { website ->
            DetailRow(
                icon = Icons.Default.Language,
                text = website,
                tint = center.color,
                testTag = "regions_detail_website",
                onClick = {
                    context.launchWebsite(website)
                }
            )
        }

        KiNDDPrimaryGradientCapsule(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("regions_detail_close")
        ) {
            Text(
                text = stringResource(R.string.close),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    tint: Color,
    testTag: String? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (onClick == null) Modifier else Modifier.clickable(role = Role.Button, onClick = onClick))
            .then(if (testTag == null) Modifier else Modifier.testTag(testTag))
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint)
        Text(text = text, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun RegionalCentersMessage.stringResource(): Int = when (this) {
    RegionalCentersMessage.INVALID_ZIP -> R.string.regions_invalid_zip
    RegionalCentersMessage.NO_MATCH -> R.string.regions_no_match
    RegionalCentersMessage.LOOKUP_UNAVAILABLE,
    RegionalCentersMessage.CATALOG_UNAVAILABLE -> R.string.regions_unavailable
}

private fun String.canonicalAcronym(): String = trim().uppercase().replace("/", "")

private object ui {
    fun regionalCenterColor(name: String): Color = RegionalCenter(id = -1, name = name).color
}
