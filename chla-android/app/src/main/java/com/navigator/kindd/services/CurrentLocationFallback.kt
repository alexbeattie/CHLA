package com.navigator.kindd.services

import kotlinx.coroutines.withTimeoutOrNull

internal suspend fun <T> resolveCachedOrCurrentLocation(
    timeoutMillis: Long,
    cachedLocation: suspend () -> T?,
    currentLocation: suspend () -> T?
): T? {
    cachedLocation()?.let { return it }
    return withTimeoutOrNull(timeoutMillis) { currentLocation() }
}
