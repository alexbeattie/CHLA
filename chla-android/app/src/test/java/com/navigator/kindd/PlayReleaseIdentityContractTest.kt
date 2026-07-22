package com.navigator.kindd

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayReleaseIdentityContractTest {
    private val targetPackage = "com.navigator.kindd"
    private val legacyMarker = charArrayOf('c', 'h', 'l', 'a').concatToString()
    private val legacyPackage = "com.$legacyMarker.kindd"

    @Test
    fun gradleAndReleaseRulesUseNavigatorIdentity() {
        val gradle = File("build.gradle.kts").readText()
        val proguard = File("proguard-rules.pro").readText()
        val releaseGuide = File("../PLAY_STORE_RELEASE.md").readText()

        assertTrue(gradle.contains("namespace = \"$targetPackage\""))
        assertTrue(gradle.contains("applicationId = \"$targetPackage\""))
        assertTrue(proguard.contains(targetPackage))
        assertTrue(releaseGuide.contains("`$targetPackage`"))
        assertFalse(gradle.contains(legacyPackage))
        assertFalse(proguard.contains(legacyPackage))
        assertFalse(releaseGuide.contains(legacyPackage))
    }

    @Test
    fun sourceTreesAndStoreAssetsContainNoLegacyMarker() {
        val roots = listOf(
            File("src/main"),
            File("src/test"),
            File("src/androidTest"),
            File("../play-assets")
        )
        val offenders = roots
            .flatMap { root -> root.walkTopDown().filter(File::isFile).toList() }
            .filter { file -> file.extension in setOf("kt", "kts", "xml", "md", "svg", "pro") }
            .filter { file -> file.readText().contains(legacyMarker, ignoreCase = true) }

        assertTrue("Legacy branding remains in: $offenders", offenders.isEmpty())
    }

    @Test
    fun sourcePackagesLiveUnderNavigatorPath() {
        val targetPath = "com/navigator/kindd"
        val legacyPath = "com/$legacyMarker/kindd"
        listOf("main", "test", "androidTest").forEach { sourceSet ->
            assertTrue(File("src/$sourceSet/java/$targetPath").isDirectory)
            assertFalse(File("src/$sourceSet/java/$legacyPath").exists())
        }
    }
}
