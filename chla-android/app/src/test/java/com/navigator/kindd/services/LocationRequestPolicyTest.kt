package com.navigator.kindd.services

import com.google.android.gms.location.Priority
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationRequestPolicyTest {

    @Test
    fun locationRequestsRequireOnlyForegroundCoarsePermission() {
        assertEquals(
            "android.permission.ACCESS_COARSE_LOCATION",
            LocationRequestPolicy.permission
        )
        assertFalse(LocationRequestPolicy.canRequest(coarsePermissionGranted = false))
        assertTrue(LocationRequestPolicy.canRequest(coarsePermissionGranted = true))
    }

    @Test
    fun locationRequestsUseCoarseCompatibleAccuracy() {
        assertEquals(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            LocationRequestPolicy.priority
        )
    }
}
