package com.navigator.kindd.ui.chat

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.navigator.kindd.ui.theme.KiNDDIndigo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeMarkdownTest {

    @Test
    fun parserRendersContractParagraphsBoldLabelsBulletsAndNumberedSteps() {
        val rendered = parse(
            """
            Short direct answer.

            **What this may mean**
            - **Memory:** Keep notes.
            1. Contact the clinician.
            """.trimIndent()
        )

        assertEquals(
            "Short direct answer.\n\nWhat this may mean\n• Memory: Keep notes.\n1. Contact the clinician.",
            rendered.text
        )
        val boldText = rendered.spanStyles
            .filter { it.item.fontWeight == FontWeight.Bold }
            .map { rendered.text.substring(it.start, it.end) }
        assertEquals(listOf("What this may mean", "Memory:"), boldText)
    }

    @Test
    fun parserAnnotatesOnlyHttpLinksAndNeverInterpretsRawHtml() {
        val rendered = parse(
            "Read [KiNDD](https://kinddhelp.com) and [unsafe](javascript:alert(1)). <b>Plain</b>"
        )

        assertEquals(
            "Read KiNDD and unsafe. <b>Plain</b>",
            rendered.text
        )
        val links = rendered.getStringAnnotations(
            tag = "URL",
            start = 0,
            end = rendered.length
        )
        assertEquals(listOf("https://kinddhelp.com"), links.map { it.item })
        val linkRange = links.single()
        val linkStyle = rendered.spanStyles.single {
            it.start == linkRange.start && it.end == linkRange.end
        }.item
        assertEquals(KiNDDIndigo, linkStyle.color)
        assertEquals(TextDecoration.Underline, linkStyle.textDecoration)
        assertTrue(rendered.text.contains("<b>Plain</b>"))
    }

    @Test
    fun parserDegradesUnsupportedHeadingsToBoldLabelsWithoutRawMarkers() {
        val rendered = parse(
            """
            # What to Bring

            ## Documents to bring
            Details follow.
            """.trimIndent()
        )

        assertEquals(
            "What to Bring\n\nDocuments to bring\nDetails follow.",
            rendered.text
        )
        val boldText = rendered.spanStyles
            .filter { it.item.fontWeight == FontWeight.Bold }
            .map { rendered.text.substring(it.start, it.end) }
        assertEquals(listOf("What to Bring", "Documents to bring"), boldText)
    }

    private fun parse(source: String): AnnotatedString {
        return parseSafeMarkdown(source)
    }
}
