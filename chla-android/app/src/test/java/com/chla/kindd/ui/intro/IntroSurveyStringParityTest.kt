package com.chla.kindd.ui.intro

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class IntroSurveyStringParityTest {

    @Test
    fun introAndSurveyStringsExistInEnglishAndSpanish() {
        val english = translatableStrings("src/main/res/values/strings.xml")
        val spanish = translatableStrings("src/main/res/values-es/strings.xml")

        assertTrue("Expected intro strings", english.keys.any { it.startsWith("intro_") })
        assertTrue("Expected survey strings", english.keys.any { it.startsWith("survey_") })
        assertEquals(english.keys, spanish.keys)
    }

    @Test
    fun spanishIntroLinkLabelsAppearInTheirBodies() {
        val spanish = translatableStrings("src/main/res/values-es/strings.xml")
        val english = translatableStrings("src/main/res/values/strings.xml")

        assertTrue(spanish.getValue("intro_privacy_body").contains(spanish.getValue("intro_privacy_link")))
        assertTrue(english.getValue("intro_privacy_body").contains(english.getValue("intro_privacy_link")))
        assertTrue(spanish.getValue("intro_device_body").contains(spanish.getValue("intro_privacy_link")))
        assertTrue(english.getValue("intro_device_body").contains(english.getValue("intro_privacy_link")))
    }

    @Test
    fun introAndSurveyCopyAvoidsLegacyBrandNames() {
        (translatableStrings("src/main/res/values/strings.xml") +
            translatableStrings("src/main/res/values-es/strings.xml")).forEach { (name, value) ->
            assertFalse(name, value.contains("CHLA"))
            assertFalse(name, value.contains("Children's Hospital"))
        }
    }

    private fun translatableStrings(path: String): Map<String, String> {
        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(File(path))
        val nodes = document.getElementsByTagName("string")
        return buildMap {
            repeat(nodes.length) { index ->
                val element = nodes.item(index) as Element
                val name = element.getAttribute("name")
                if (
                    (name.startsWith("intro_") || name.startsWith("survey_")) &&
                    element.getAttribute("translatable") != "false"
                ) {
                    put(name, element.textContent)
                }
            }
        }
    }
}
