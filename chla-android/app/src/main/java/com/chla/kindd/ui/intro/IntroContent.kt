package com.chla.kindd.ui.intro

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.chla.kindd.R
import com.chla.kindd.platform.launchEmail
import com.chla.kindd.platform.launchWebsite
import com.chla.kindd.ui.screens.KINDD_PRIVACY_URL
import com.chla.kindd.ui.theme.KiNDDIndigo
import com.chla.kindd.ui.theme.KiNDDShapeTokens

private data class IntroSection(
    @StringRes val heading: Int,
    @StringRes val body: Int,
    val testTag: String,
    val urgent: Boolean = false
)

private val introSections = listOf(
    IntroSection(R.string.intro_how_it_works_heading, R.string.intro_how_it_works_body, "intro_how_it_works"),
    IntroSection(R.string.intro_services_heading, R.string.intro_services_body, "intro_services"),
    IntroSection(R.string.intro_audience_heading, R.string.intro_audience_body, "intro_audience"),
    IntroSection(R.string.intro_after_questions_heading, R.string.intro_after_questions_body, "intro_after_questions"),
    IntroSection(R.string.intro_privacy_heading, R.string.intro_privacy_body, "intro_privacy"),
    IntroSection(R.string.intro_language_heading, R.string.intro_language_body, "intro_language"),
    IntroSection(R.string.intro_keep_in_mind_heading, R.string.intro_keep_in_mind_body, "intro_keep_in_mind"),
    IntroSection(R.string.intro_providers_heading, R.string.intro_providers_body, "intro_providers"),
    IntroSection(R.string.intro_urgent_heading, R.string.intro_urgent_body, "intro_urgent", urgent = true),
    IntroSection(R.string.intro_help_heading, R.string.intro_help_body, "intro_help")
)

private data class IntroLink(val label: String, val onClick: () -> Unit)

/** Non-scrolling intro body; hosts own scrolling and the primary action. */
@Composable
fun IntroContent(
    modifier: Modifier = Modifier,
    onEmailSupport: (() -> Unit)? = null,
    onOpenPrivacyPolicy: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val supportEmail = stringResource(R.string.intro_support_email)
    val emailSupport: () -> Unit = onEmailSupport ?: { context.launchEmail(supportEmail) }
    val openPrivacyPolicy: () -> Unit =
        onOpenPrivacyPolicy ?: { context.launchWebsite(KINDD_PRIVACY_URL) }
    val links = listOf(
        IntroLink(supportEmail, emailSupport),
        IntroLink(stringResource(R.string.intro_privacy_link), openPrivacyPolicy)
    )
    Column(
        modifier = modifier.fillMaxWidth().testTag("intro_content"),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = stringResource(R.string.intro_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() }.testTag("intro_title")
        )
        Text(
            text = stringResource(R.string.intro_lead),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        introSections.forEach { section ->
            if (section.urgent) {
                IntroUrgentSection(section, links)
            } else {
                IntroSectionBlock(section, links)
            }
        }
    }
}

@Composable
private fun IntroSectionBlock(section: IntroSection, links: List<IntroLink>) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag(section.testTag),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        IntroHeading(stringResource(section.heading), MaterialTheme.colorScheme.onSurface)
        IntroBody(
            text = stringResource(section.body),
            links = links,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun IntroUrgentSection(section: IntroSection, links: List<IntroLink>) {
    val shape = RoundedCornerShape(KiNDDShapeTokens.Selection)
    val contentColor = MaterialTheme.colorScheme.onErrorContainer
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer, shape)
            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f), shape)
            .padding(16.dp)
            .testTag("intro_urgent_card"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ReportProblem,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            IntroHeading(stringResource(section.heading), contentColor)
        }
        IntroBody(text = stringResource(section.body), links = links, color = contentColor)
    }
}

@Composable
private fun IntroHeading(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier.semantics { heading() }
    )
}

@Composable
private fun IntroBody(text: String, links: List<IntroLink>, color: Color) {
    val presentLinks = links.filter { text.contains(it.label) }
    val style = MaterialTheme.typography.bodyLarge.copy(color = color)
    if (presentLinks.isEmpty()) {
        Text(text = text, style = style)
        return
    }
    val annotated = remember(text, presentLinks.map(IntroLink::label)) {
        linkedText(text, presentLinks.map(IntroLink::label))
    }
    ClickableText(
        text = annotated,
        style = style,
        modifier = Modifier.semantics {
            customActions = presentLinks.map { link ->
                CustomAccessibilityAction(link.label) {
                    link.onClick()
                    true
                }
            }
        },
        onClick = { offset ->
            annotated.getStringAnnotations(LINK_TAG, offset, offset)
                .firstOrNull()
                ?.let { annotation -> presentLinks.firstOrNull { it.label == annotation.item } }
                ?.onClick
                ?.invoke()
        }
    )
}

private fun linkedText(text: String, labels: List<String>): AnnotatedString = buildAnnotatedString {
    append(text)
    labels.forEach { label ->
        var start = text.indexOf(label)
        while (start >= 0) {
            val end = start + label.length
            addStyle(
                SpanStyle(color = KiNDDIndigo, textDecoration = TextDecoration.Underline),
                start,
                end
            )
            addStringAnnotation(LINK_TAG, label, start, end)
            start = text.indexOf(label, end)
        }
    }
}

private const val LINK_TAG = "intro_link"
