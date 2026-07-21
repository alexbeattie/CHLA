package com.chla.kindd.ui.settings

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocaleConfigContractTest {

    @Test
    fun manifestDeclaresExactlyEnglishAndSpanishPerAppLocales() {
        val manifest = File("src/main/AndroidManifest.xml")
        val localeConfig = File("src/main/res/xml/locales_config.xml")

        assertTrue("AndroidManifest.xml must exist", manifest.isFile)
        assertTrue("Per-app locale config must exist", localeConfig.isFile)
        assertTrue(
            "Application must point at the locale config",
            manifest.readText().contains("android:localeConfig=\"@xml/locales_config\"")
        )

        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(localeConfig)
        val locales = document.getElementsByTagName("locale")
        val languageTags = buildList {
            repeat(locales.length) { index ->
                add(locales.item(index).attributes.getNamedItem("android:name").nodeValue)
            }
        }

        assertEquals(listOf("en", "es"), languageTags)
    }
}
