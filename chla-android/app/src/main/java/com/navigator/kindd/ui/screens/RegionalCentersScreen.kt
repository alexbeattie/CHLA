package com.navigator.kindd.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.navigator.kindd.R
import com.navigator.kindd.data.servicearea.ServiceAreaFeature
import com.navigator.kindd.ui.map.RegionalCenterMapRenderModel
import com.navigator.kindd.ui.regions.RegionalCentersContent
import com.navigator.kindd.ui.regions.RegionalCenterServiceAreaState
import com.navigator.kindd.ui.regions.rememberRegionalCenterServiceAreas
import com.navigator.kindd.ui.theme.KiNDDTopAppBarColorContract
import com.navigator.kindd.ui.theme.kinddTopAppBarColorContract

@Composable
fun RegionalCentersScreen(
    onBack: () -> Unit,
    viewModel: RegionalCentersViewModel = hiltViewModel(),
    serviceAreasOverride: List<ServiceAreaFeature>? = null,
    mapContent: (@Composable (RegionalCenterMapRenderModel, (String) -> Unit) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bundledServiceAreaState by rememberRegionalCenterServiceAreas()

    RegionalCentersContent(
        uiState = uiState,
        serviceAreaState = serviceAreasOverride?.let(RegionalCenterServiceAreaState::Success)
            ?: bundledServiceAreaState,
        onBack = onBack,
        onZipChanged = viewModel::onZipChanged,
        onSubmitZip = viewModel::submitZip,
        mapContent = mapContent
    )
}

/** Retained for focused adaptive-app-bar contract tests and secondary push destinations. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RegionalCentersTopAppBar(
    onBack: () -> Unit,
    colorContract: KiNDDTopAppBarColorContract = kinddTopAppBarColorContract()
) {
    TopAppBar(
        title = { Text(stringResource(R.string.regional_centers_title)) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colorContract.containerColor,
            titleContentColor = colorContract.titleContentColor,
            navigationIconContentColor = colorContract.navigationIconContentColor
        )
    )
}
