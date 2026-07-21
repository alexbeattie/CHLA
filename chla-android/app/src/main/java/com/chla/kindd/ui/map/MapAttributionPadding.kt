package com.chla.kindd.ui.map

private const val MAP_ATTRIBUTION_CLEARANCE_DP = 20f

internal fun mapAttributionBottomPaddingDp(
    overlayHeightPx: Int,
    density: Float,
    overlaysMap: Boolean
): Float {
    if (!overlaysMap || overlayHeightPx <= 0 || density <= 0f) return 0f
    return overlayHeightPx / density + MAP_ATTRIBUTION_CLEARANCE_DP
}
