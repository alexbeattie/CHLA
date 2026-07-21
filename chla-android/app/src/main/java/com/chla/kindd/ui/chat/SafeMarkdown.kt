package com.chla.kindd.ui.chat

import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.chla.kindd.R
import com.chla.kindd.ui.theme.KiNDDIndigo

internal const val SAFE_MARKDOWN_URL_TAG = "URL"

@Composable
internal fun SafeMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onOpenLink: ((String) -> Unit)? = null
) {
    val rendered = remember(markdown) { parseSafeMarkdown(markdown) }
    val links = rendered.getStringAnnotations(
        tag = SAFE_MARKDOWN_URL_TAG,
        start = 0,
        end = rendered.length
    )
    if (links.isEmpty()) {
        Text(text = rendered, modifier = modifier, style = style)
        return
    }

    val uriHandler = LocalUriHandler.current
    val openVerb = stringResource(R.string.chat_open_link_action)
    val openLink: (String) -> Boolean = remember(onOpenLink, uriHandler) {
        { url ->
            runCatching { (onOpenLink ?: uriHandler::openUri)(url) }.isSuccess
        }
    }
    val accessibilityActions = remember(rendered, links, openVerb, openLink) {
        links.map { link ->
            val label = rendered.text.substring(link.start, link.end)
            CustomAccessibilityAction(label = "$openVerb $label") {
                openLink(link.item)
            }
        }
    }
    ClickableText(
        text = rendered,
        modifier = modifier.semantics {
            customActions = accessibilityActions
        },
        style = style,
        onClick = { offset ->
            rendered.getStringAnnotations(
                tag = SAFE_MARKDOWN_URL_TAG,
                start = offset,
                end = offset
            ).firstOrNull()?.let { link ->
                openLink(link.item)
            }
        }
    )
}

internal fun parseSafeMarkdown(source: String): AnnotatedString {
    val normalized = source
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .trim('\n')
    val builder = AnnotatedString.Builder()

    normalized.lines().forEachIndexed { index, rawLine ->
        val line = rawLine.trimEnd()
        val content = line.trimStart()
        when {
            SAFE_HEADING.matches(content) -> {
                val match = requireNotNull(SAFE_HEADING.matchEntire(content))
                val start = builder.length
                builder.appendSafeInlineMarkdown(match.groupValues[1])
                builder.addStyle(
                    style = SpanStyle(fontWeight = FontWeight.Bold),
                    start = start,
                    end = builder.length
                )
            }
            content.startsWith("- ") -> {
                builder.append("• ")
                builder.appendSafeInlineMarkdown(content.removePrefix("- "))
            }
            ORDERED_STEP.matches(content) -> {
                val match = requireNotNull(ORDERED_STEP.matchEntire(content))
                builder.append(match.groupValues[1])
                builder.append(". ")
                builder.appendSafeInlineMarkdown(match.groupValues[2])
            }
            else -> builder.appendSafeInlineMarkdown(line)
        }
        if (index != normalized.lines().lastIndex) builder.append('\n')
    }

    return builder.toAnnotatedString()
}

private fun AnnotatedString.Builder.appendSafeInlineMarkdown(source: String) {
    var cursor = 0
    while (cursor < source.length) {
        if (source.startsWith("**", cursor)) {
            val closing = source.indexOf("**", startIndex = cursor + 2)
            if (closing > cursor + 2) {
                val start = length
                append(source.substring(cursor + 2, closing))
                addStyle(
                    style = SpanStyle(fontWeight = FontWeight.Bold),
                    start = start,
                    end = length
                )
                cursor = closing + 2
                continue
            }
        }

        if (source[cursor] == '[') {
            val labelEnd = source.indexOf("](", startIndex = cursor + 1)
            val urlEnd = if (labelEnd >= 0) {
                source.findClosingLinkParenthesis(startIndex = labelEnd + 2)
            } else {
                -1
            }
            if (labelEnd > cursor + 1 && urlEnd > labelEnd + 2) {
                val label = source.substring(cursor + 1, labelEnd)
                val url = source.substring(labelEnd + 2, urlEnd)
                val start = length
                append(label)
                if (url.isSafeWebUrl()) {
                    addStyle(
                        style = SpanStyle(
                            color = KiNDDIndigo,
                            textDecoration = TextDecoration.Underline
                        ),
                        start = start,
                        end = length
                    )
                    addStringAnnotation(
                        tag = SAFE_MARKDOWN_URL_TAG,
                        annotation = url,
                        start = start,
                        end = length
                    )
                }
                cursor = urlEnd + 1
                continue
            }
        }

        append(source[cursor])
        cursor += 1
    }
}

private fun String.isSafeWebUrl(): Boolean =
    startsWith("https://", ignoreCase = true) || startsWith("http://", ignoreCase = true)

private fun String.findClosingLinkParenthesis(startIndex: Int): Int {
    var nestedDepth = 0
    for (index in startIndex until length) {
        when (this[index]) {
            '(' -> nestedDepth += 1
            ')' -> if (nestedDepth == 0) return index else nestedDepth -= 1
        }
    }
    return -1
}

private val ORDERED_STEP = Regex("^(\\d+)\\.\\s+(.+)$")
private val SAFE_HEADING = Regex("^#{1,6}\\s+(.+)$")
