package com.chla.kindd.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.chla.kindd.R
import com.chla.kindd.platform.launchEmail
import com.chla.kindd.platform.launchWebsite
import com.chla.kindd.ui.intro.IntroBody
import com.chla.kindd.ui.intro.IntroLink
import com.chla.kindd.ui.screens.KINDD_PRIVACY_URL
import com.chla.kindd.ui.theme.KiNDDIndigo
import com.chla.kindd.ui.theme.KiNDDShapeTokens

@Composable
fun HowToGuideContent(
    modifier: Modifier = Modifier,
    onOpenIntro: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val privacyLink = IntroLink(stringResource(R.string.intro_privacy_link)) {
        context.launchWebsite(KINDD_PRIVACY_URL)
    }
    Column(
        modifier = modifier.fillMaxWidth().testTag("onboarding_howto_content"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(KiNDDIndigo.copy(alpha = 0.13f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = null,
                tint = KiNDDIndigo,
                modifier = Modifier.size(26.dp)
            )
        }
        OnboardingHeading(stringResource(R.string.onboarding_howto_title))
        Text(
            text = stringResource(R.string.onboarding_howto_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        OnboardingCard {
            HowToRow(
                icon = Icons.Default.FilterList,
                title = stringResource(R.string.onboarding_howto_filters_title),
                detail = stringResource(R.string.onboarding_howto_filters_body)
            )
            HowToRow(
                icon = Icons.Default.Groups,
                title = stringResource(R.string.onboarding_howto_children_title),
                detail = stringResource(R.string.onboarding_howto_children_body)
            )
            HowToRow(
                icon = Icons.Default.Lock,
                title = stringResource(R.string.intro_device_title),
                detail = stringResource(R.string.intro_device_body),
                links = listOf(privacyLink),
                modifier = Modifier.testTag("onboarding_howto_privacy")
            )
        }
        GoodToKnowCard(onOpenIntro = onOpenIntro)
    }
}

@Composable
private fun GoodToKnowCard(onOpenIntro: (() -> Unit)?) {
    val context = LocalContext.current
    val supportEmail = stringResource(R.string.intro_support_email)
    val shape = RoundedCornerShape(KiNDDShapeTokens.Selection)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(KiNDDIndigo.copy(alpha = 0.07f), shape)
            .border(1.dp, KiNDDIndigo.copy(alpha = 0.18f), shape)
            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 6.dp)
            .testTag("onboarding_good_to_know"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = KiNDDIndigo,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = stringResource(R.string.intro_good_to_know_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
            )
        }
        Text(
            text = stringResource(R.string.intro_good_to_know_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column {
            if (onOpenIntro != null) {
                GoodToKnowAction(
                    label = stringResource(R.string.intro_read_full),
                    onClick = onOpenIntro,
                    testTag = "onboarding_read_full_intro"
                )
            }
            GoodToKnowAction(
                label = stringResource(R.string.intro_questions_email),
                onClick = { context.launchEmail(supportEmail) },
                testTag = "onboarding_email_support"
            )
        }
    }
}

@Composable
private fun GoodToKnowAction(label: String, onClick: () -> Unit, testTag: String) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp),
        modifier = Modifier.heightIn(min = 48.dp).testTag(testTag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = KiNDDIndigo
        )
    }
}

@Composable
internal fun OnboardingCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(KiNDDShapeTokens.Selection)
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                RoundedCornerShape(KiNDDShapeTokens.Selection)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
internal fun HowToRow(
    icon: ImageVector,
    title: String,
    detail: String,
    modifier: Modifier = Modifier,
    links: List<IntroLink> = emptyList()
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = KiNDDIndigo,
            modifier = Modifier.size(18.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IntroBody(
                text = detail,
                links = links,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
internal fun HowToStep(
    modifier: Modifier = Modifier,
    onOpenIntro: (() -> Unit)? = null
) {
    OnboardingStepColumn(modifier = modifier) {
        HowToGuideContent(onOpenIntro = onOpenIntro)
    }
}
