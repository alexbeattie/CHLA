package com.navigator.kindd.ui.regions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.navigator.kindd.R
import com.navigator.kindd.data.servicearea.BundledServiceAreaDataSource
import com.navigator.kindd.data.servicearea.ServiceAreaFeature

@Immutable
sealed interface RegionalCenterServiceAreaState {
    data object Loading : RegionalCenterServiceAreaState

    data class Success(val areas: List<ServiceAreaFeature>) : RegionalCenterServiceAreaState

    data object Error : RegionalCenterServiceAreaState
}

internal fun Result<List<ServiceAreaFeature>>.toRegionalCenterServiceAreaState():
    RegionalCenterServiceAreaState = fold(
    onSuccess = { areas ->
        if (areas.isEmpty()) {
            RegionalCenterServiceAreaState.Error
        } else {
            RegionalCenterServiceAreaState.Success(areas)
        }
    },
    onFailure = { RegionalCenterServiceAreaState.Error }
)

/** Loads bundled service-area geometry without collapsing load failures into an empty map. */
@Composable
fun rememberRegionalCenterServiceAreas(): State<RegionalCenterServiceAreaState> {
    val applicationContext = LocalContext.current.applicationContext
    val dataSource = remember(applicationContext) {
        BundledServiceAreaDataSource(
            resourceReader = {
                applicationContext.resources.openRawResource(R.raw.la_regional_centers).reader()
            }
        )
    }
    return produceState<RegionalCenterServiceAreaState>(
        initialValue = RegionalCenterServiceAreaState.Loading,
        key1 = dataSource
    ) {
        value = dataSource.getServiceAreas().toRegionalCenterServiceAreaState()
    }
}
