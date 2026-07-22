package com.navigator.kindd

import java.io.File
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class PlayReleaseIdentityContractTest {
    private val targetPackage = "com.navigator.kindd"
    private val targetPath = "com/navigator/kindd"
    private val legacyMarker = charArrayOf('c', 'h', 'l', 'a').concatToString()
    private val legacyPackage = "com.$legacyMarker.kindd"
    private val forbiddenExactBrands = listOf(legacyMarker, legacyPackage)
    private val formerInstitutionWords = listOf(
        "child" + "ren" + "s",
        "hospital",
        "los",
        "angeles"
    )
    private val textExtensions = setOf(
        "geojson",
        "gradle",
        "json",
        "kt",
        "kts",
        "md",
        "pro",
        "properties",
        "svg",
        "txt",
        "xml"
    )
    private val binaryExtensions = setOf("png")
    private val treeRoots = listOf(
        File("src/main"),
        File("src/test"),
        File("src/androidTest"),
        File("../play-assets")
    )
    private val projectTextFiles = listOf(
        File("build.gradle.kts"),
        File("proguard-rules.pro"),
        File("../build.gradle.kts"),
        File("../settings.gradle.kts"),
        File("../gradle.properties"),
        File("../gradle/wrapper/gradle-wrapper.properties"),
        File("../local.defaults.properties"),
        File("../CLAUDE.md"),
        File("../README.md"),
        File("../PLAY_STORE_RELEASE.md")
    )
    private val infrastructureMarkerFiles = setOf(
        File("../CLAUDE.md").canonicalFile,
        File("../README.md").canonicalFile,
        File("../PLAY_STORE_RELEASE.md").canonicalFile,
        File("../settings.gradle.kts").canonicalFile
    )

    @Test
    fun gradleAndReleaseRulesDeclareNavigatorIdentity() {
        val gradle = File("build.gradle.kts").readText()
        val proguard = File("proguard-rules.pro").readText()
        val releaseGuide = File("../PLAY_STORE_RELEASE.md").readText()

        assertTrue(
            Regex("(?m)^\\s*namespace\\s*=\\s*\"${Regex.escape(targetPackage)}\"\\s*$")
                .containsMatchIn(gradle)
        )
        assertTrue(
            Regex("(?m)^\\s*applicationId\\s*=\\s*\"${Regex.escape(targetPackage)}\"\\s*$")
                .containsMatchIn(gradle)
        )
        assertTrue(proguard.contains(targetPackage))
        assertTrue(releaseGuide.contains("`$targetPackage`"))
        assertFalse(gradle.contains(legacyPackage))
        assertFalse(proguard.contains(legacyPackage))
        assertFalse(releaseGuide.contains(legacyPackage))
    }

    @Test
    fun kotlinSourcesUseNavigatorRootAndMatchingPackages() {
        listOf("main", "test", "androidTest").forEach { sourceSet ->
            val javaRoot = File("src/$sourceSet/java")
            val kotlinFiles = javaRoot.walkTopDown()
                .filter { file -> file.isFile && file.extension == "kt" }
                .toList()

            assertTrue("No Kotlin files found below ${javaRoot.path}", kotlinFiles.isNotEmpty())
            kotlinFiles.forEach { file ->
                val relativePath = file.relativeTo(javaRoot).invariantSeparatorsPath
                assertTrue(
                    "Kotlin source is outside $targetPath: $relativePath",
                    relativePath.startsWith("$targetPath/")
                )

                val expectedPackage = requireNotNull(file.parentFile)
                    .relativeTo(javaRoot)
                    .invariantSeparatorsPath
                    .replace('/', '.')
                val packageDeclaration = file.useLines { lines ->
                    lines.map(String::trim)
                        .firstOrNull { line -> line.startsWith("package ") }
                }
                assertEquals(
                    "Package declaration does not match ${file.path}",
                    "package $expectedPackage",
                    packageDeclaration
                )
            }
        }
    }

    @Test
    fun mergedReleaseManifestResolvesNavigatorComponents() {
        val manifest = releaseManifest()
        assertEquals(targetPackage, manifest.documentElement.getAttribute("package"))

        val applications = manifest.getElementsByTagName("application")
        assertEquals("Merged manifest must contain one application", 1, applications.length)
        val application = applications.item(0) as Element
        assertEquals(
            "$targetPackage.KINDDApplication",
            application.getAttributeNS(ANDROID_NAMESPACE, "name")
        )

        val activities = manifest.getElementsByTagName("activity")
        val launcherActivities = (0 until activities.length)
            .map { index -> activities.item(index) as Element }
            .filter { activity ->
                activity.hasAndroidNamedDescendant(
                    tagName = "action",
                    name = "android.intent.action.MAIN"
                ) && activity.hasAndroidNamedDescendant(
                    tagName = "category",
                    name = "android.intent.category.LAUNCHER"
                )
            }
            .map { activity -> activity.getAttributeNS(ANDROID_NAMESPACE, "name") }

        assertEquals(listOf("$targetPackage.MainActivity"), launcherActivities)
    }

    @Test
    fun textIdentitySurfacesContainNoLegacyBranding() {
        val offenders = identityTextFiles()
            .filter { file ->
                val text = file.readText().withoutAllowedInfrastructureMarker(file)
                text.containsForbiddenBrand()
            }

        assertTrue("Legacy branding remains in: $offenders", offenders.isEmpty())
    }

    @Test
    fun forbiddenBrandDetectorNormalizesIdentifiersPunctuationAndCase() {
        val subject = "Child" + "ren"
        val institution = "Hos" + "pital"
        val location = listOf("Los", "Angeles").joinToString(" ")
        val variants = listOf(
            "$subject's $institution $location",
            "$subject’s $institution $location",
            "${subject.uppercase(Locale.ROOT)}’S -- " +
                "${institution.lowercase(Locale.ROOT)}, ${location.uppercase(Locale.ROOT)}",
            "${subject}s | $institution / $location",
            "${subject}\\'s $institution $location",
            "${subject}&" + "apos;s $institution $location",
            "${subject}&" + "#39;s $institution $location",
            "${subject}&" + "#x27;s $institution $location"
        )

        (forbiddenExactBrands + variants).forEach { value ->
            assertTrue("Forbidden brand variant was not detected", value.containsForbiddenBrand())
        }
    }

    @Test
    fun binaryIdentityAssetsUseSeparateReleaseGate() {
        val scopedFiles = scopedTreeFiles()
        val unclassified = scopedFiles.filter { file ->
            file.extension.lowercase() !in textExtensions + binaryExtensions
        }
        assertTrue(
            "Unclassified identity files require an explicit gate: $unclassified",
            unclassified.isEmpty()
        )

        val binaryAssets = scopedFiles.filter { file ->
            file.extension.lowercase() in binaryExtensions
        }
        assertTrue("Expected binary identity assets to be classified", binaryAssets.isNotEmpty())

        val releaseGuide = File("../PLAY_STORE_RELEASE.md").readText()
        assertTrue(releaseGuide.contains("## Binary asset identity gate"))
        assertTrue(releaseGuide.contains("PNG files are not read as text"))
    }

    private fun identityTextFiles(): List<File> = (
        scopedTreeFiles().filter { file -> file.extension.lowercase() in textExtensions } +
            projectTextFiles
        )
        .distinctBy { file -> file.canonicalPath }

    private fun scopedTreeFiles(): List<File> = treeRoots
        .flatMap { root -> root.walkTopDown().filter(File::isFile).toList() }

    private fun String.containsForbiddenBrand(): Boolean {
        val caseFolded = lowercase(Locale.ROOT)
        if (forbiddenExactBrands.any { brand -> caseFolded.contains(brand) }) return true

        val words = normalizedBrandWords()
        return words.windowed(formerInstitutionWords.size)
            .any { sequence -> sequence == formerInstitutionWords }
    }

    private fun String.normalizedBrandWords(): List<String> = lowercase(Locale.ROOT)
        .replace(Regex("(?i)&(?:apos|#39|#x27);"), "'")
        .replace(Regex("""\\*['’‘ʼ]"""), "")
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
        .split(Regex("\\s+"))
        .filter(String::isNotEmpty)

    private fun String.withoutAllowedInfrastructureMarker(file: File): String {
        if (file.canonicalFile !in infrastructureMarkerFiles) return this
        val infrastructureName = "$legacyMarker-android"
        return replace(
            Regex("(?i)${Regex.escape(infrastructureName)}"),
            ""
        )
    }

    private fun Element.hasAndroidNamedDescendant(tagName: String, name: String): Boolean {
        val descendants = getElementsByTagName(tagName)
        return (0 until descendants.length).any { index ->
            val element = descendants.item(index) as Element
            element.getAttributeNS(ANDROID_NAMESPACE, "name") == name
        }
    }
}
