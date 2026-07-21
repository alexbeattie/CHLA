package com.chla.kindd.ui.regions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.chla.kindd.R
import com.chla.kindd.data.servicearea.BundledServiceAreaDataSource
import com.chla.kindd.data.servicearea.ServiceAreaFeature

/** Loads the immutable bundled service-area geometry for presentation-only map surfaces. */
@Composable
fun rememberRegionalCenterServiceAreas(): State<List<ServiceAreaFeature>> {
    val applicationContext = LocalContext.current.applicationContext
    val dataSource = remember(applicationContext) {
        BundledServiceAreaDataSource(
            resourceReader = {
                applicationContext.resources.openRawResource(R.raw.la_regional_centers).reader()
            }
        )
    }
    return produceState(emptyList(), dataSource) {
        value = dataSource.getServiceAreas().getOrDefault(emptyList())
    }
}
