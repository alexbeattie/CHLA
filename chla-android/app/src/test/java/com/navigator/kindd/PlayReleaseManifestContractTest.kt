package com.navigator.kindd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayReleaseManifestContractTest {

    @Test
    fun releaseLocationPermissionIsForegroundCoarseOnly() {
        val permissions = releaseManifest()
            .getElementsByTagName("uses-permission")
            .let { nodes ->
                (0 until nodes.length)
                    .map { nodes.item(it) }
                    .map { it.attributes.getNamedItemNS(ANDROID_NAMESPACE, "name").nodeValue }
                    .toSet()
            }

        assertTrue(permissions.contains("android.permission.ACCESS_COARSE_LOCATION"))
        assertFalse(permissions.contains("android.permission.ACCESS_FINE_LOCATION"))
        assertFalse(permissions.contains("android.permission.ACCESS_BACKGROUND_LOCATION"))
    }

    @Test
    fun optionalLocationDoesNotFilterDevicesWithoutLocationHardware() {
        val features = releaseManifest()
            .getElementsByTagName("uses-feature")
            .let { nodes ->
                (0 until nodes.length)
                    .map { nodes.item(it) }
                    .mapNotNull { node ->
                        val name = node.attributes
                            .getNamedItemNS(ANDROID_NAMESPACE, "name")
                            ?.nodeValue
                            ?: return@mapNotNull null
                        val required = node.attributes
                            .getNamedItemNS(ANDROID_NAMESPACE, "required")
                            ?.nodeValue
                            ?: return@mapNotNull null
                        name to required
                    }
                    .toMap()
            }

        listOf(
            "android.hardware.location",
            "android.hardware.location.network",
            "android.hardware.location.gps"
        ).forEach { feature ->
            assertEquals(
                "$feature must be explicitly optional",
                "false",
                features[feature]
            )
        }
    }
}
