package com.chla.kindd.ui.discovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.chla.kindd.R
import com.chla.kindd.data.discovery.DiscoveryError
import com.chla.kindd.data.discovery.DiscoveryState
import com.chla.kindd.data.models.Provider

@Composable
fun DiscoveryStateContent(
    state: DiscoveryState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    emptyContent: (@Composable () -> Unit)? = null,
    content: @Composable (List<Provider>) -> Unit
) {
    when {
        state.isLoading && !state.hasLoadedOnce && state.providers.isEmpty() -> {
            Box(
                modifier
                    .fillMaxSize()
                    .testTag("discovery_initial_loading")
                    .semantics { liveRegion = LiveRegionMode.Polite },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(stringResource(R.string.loading))
                }
            }
        }
        state.error != null && state.providers.isEmpty() -> {
            FullError(
                error = state.error,
                onRetry = onRetry,
                modifier = modifier.testTag("discovery_initial_error")
            )
        }
        else -> {
            Column(modifier = modifier.fillMaxSize()) {
                if (state.isLoading && state.providers.isNotEmpty()) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("discovery_refresh_progress")
                    )
                }
                if (state.error != null && state.providers.isNotEmpty()) {
                    RefreshErrorBanner(state.error, onRetry)
                }
                if (state.providers.isNotEmpty()) {
                    Text(
                        text = stringResource(
                            if (state.providers.size == 1) {
                                R.string.resource_found
                            } else {
                                R.string.resources_found
                            },
                            state.providers.size
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("discovery_result_count")
                            .semantics { liveRegion = LiveRegionMode.Polite }
                    )
                    Box(Modifier.weight(1f)) { content(state.providers) }
                } else if (state.hasLoadedOnce) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("discovery_empty")
                            .semantics { liveRegion = LiveRegionMode.Polite },
                        contentAlignment = Alignment.Center
                    ) {
                        emptyContent?.invoke() ?: DefaultDiscoveryEmptyState()
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultDiscoveryEmptyState() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Default.SearchOff,
            contentDescription = null
        )
        Text(
            text = stringResource(R.string.no_resources_found),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleMedium
        )
        Text(stringResource(R.string.try_adjusting_filters))
    }
}

@Composable
private fun FullError(
    error: DiscoveryError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .fillMaxSize()
            .semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = discoveryErrorText(error),
                style = MaterialTheme.typography.bodyLarge
            )
            Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
private fun RefreshErrorBanner(error: DiscoveryError, onRetry: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("discovery_error_banner")
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = discoveryErrorText(error),
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

@Composable
internal fun discoveryErrorText(error: DiscoveryError): String = stringResource(
    when (error) {
        DiscoveryError.NETWORK -> R.string.discovery_error_network
        DiscoveryError.TIMEOUT -> R.string.discovery_error_timeout
        DiscoveryError.SERVER -> R.string.discovery_error_server
        DiscoveryError.UNKNOWN -> R.string.discovery_error_unknown
    }
)
