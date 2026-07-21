package com.chla.kindd.ui.home.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chla.kindd.R
import com.chla.kindd.data.discovery.TherapyType
import com.chla.kindd.data.profile.JourneyStage
import com.chla.kindd.data.profile.UserProfile
import com.chla.kindd.ui.chat.ChatLaunchPrompt
import com.chla.kindd.ui.home.HomeUiState
import com.chla.kindd.ui.theme.KiNDDAiGradient
import com.chla.kindd.ui.theme.KiNDDCardSurface
import com.chla.kindd.ui.theme.KiNDDIndigo
import com.chla.kindd.ui.theme.KiNDDPink
import com.chla.kindd.ui.theme.KiNDDPrimaryGradientCapsule
import com.chla.kindd.ui.theme.KiNDDSecondaryCapsule
import com.chla.kindd.ui.theme.KiNDDShapeTokens
import com.chla.kindd.ui.theme.KiNDDViolet

@Composable
fun HomeServiceTiles(
    onTherapySelected: (TherapyType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home_service_tiles"),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ServiceTile(
            therapy = TherapyType.ABA,
            labelRes = R.string.home_parity_service_aba,
            icon = Icons.Default.Psychology,
            tint = KiNDDIndigo,
            onClick = onTherapySelected,
            modifier = Modifier.weight(1f)
        )
        ServiceTile(
            therapy = TherapyType.SPEECH,
            labelRes = R.string.home_parity_service_speech,
            icon = Icons.Default.GraphicEq,
            tint = KiNDDPink,
            onClick = onTherapySelected,
            modifier = Modifier.weight(1f)
        )
        ServiceTile(
            therapy = TherapyType.OCCUPATIONAL,
            labelRes = R.string.home_parity_service_occupational,
            icon = Icons.Default.TouchApp,
            tint = KiNDDViolet,
            onClick = onTherapySelected,
            modifier = Modifier.weight(1f)
        )
        ServiceTile(
            therapy = TherapyType.PHYSICAL,
            labelRes = R.string.home_parity_service_physical,
            icon = Icons.Default.DirectionsWalk,
            tint = Color(0xFFA855F7),
            onClick = onTherapySelected,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ServiceTile(
    therapy: TherapyType,
    @StringRes labelRes: Int,
    icon: ImageVector,
    tint: Color,
    onClick: (TherapyType) -> Unit,
    modifier: Modifier = Modifier
) {
    KiNDDCardSurface(
        onClick = { onClick(therapy) },
        modifier = modifier
            .heightIn(min = 104.dp)
            .testTag("home_service_${therapy.name.lowercase()}"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.13f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(21.dp))
            }
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun HomeNextStepCard(
    profile: UserProfile,
    uiState: HomeUiState,
    onChat: (ChatLaunchPrompt) -> Unit,
    onCall: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val stage = profile.journeyStage ?: return
    if (stage == JourneyStage.EXPLORING) return
    val centerName = profile.regionalCenter?.name
        ?: stringResource(R.string.home_parity_your_regional_center)
    val step = when (stage) {
        JourneyStage.JUST_DIAGNOSED -> HomeStep(
            titleRes = R.string.home_journey_just_diagnosed_title,
            detail = stringResource(R.string.home_parity_next_step_just_diagnosed_detail, centerName),
            actionRes = R.string.home_journey_just_diagnosed_action,
            prompt = ChatLaunchPrompt.JUST_DIAGNOSED,
            showsCall = true
        )
        JourneyStage.WAITING_FOR_INTAKE -> HomeStep(
            titleRes = R.string.home_journey_waiting_title,
            detail = stringResource(R.string.home_parity_next_step_waiting_detail),
            actionRes = R.string.home_journey_waiting_action,
            prompt = ChatLaunchPrompt.WAITING_INTAKE,
            showsCall = false
        )
        JourneyStage.RECEIVING_SERVICES -> HomeStep(
            titleRes = R.string.home_journey_receiving_title,
            detail = stringResource(R.string.home_parity_next_step_receiving_detail),
            actionRes = R.string.home_journey_receiving_action,
            prompt = ChatLaunchPrompt.RECEIVING_SERVICES,
            showsCall = false
        )
        JourneyStage.EXPLORING -> return
    }
    val shape = RoundedCornerShape(KiNDDShapeTokens.Card)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(14.dp, shape, ambientColor = KiNDDViolet.copy(alpha = 0.10f))
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .background(Brush.linearGradient(listOf(KiNDDViolet.copy(alpha = 0.08f), Color.Transparent)))
            .border(1.dp, KiNDDViolet.copy(alpha = 0.20f), shape)
            .padding(18.dp)
            .testTag("home_next_step"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.SubdirectoryArrowRight, contentDescription = null, tint = KiNDDViolet, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text(
                stringResource(R.string.home_your_next_step).uppercase(),
                color = KiNDDViolet,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            stringResource(step.titleRes),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            step.detail,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        val dialDigits = uiState.dialDigitsFor(profile).takeIf { step.showsCall }
        if (LocalDensity.current.fontScale >= 1.3f) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                dialDigits?.let { digits ->
                    NextStepCallAction(onClick = { onCall(digits) })
                }
                NextStepChatAction(step = step, onChat = onChat)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                dialDigits?.let { digits ->
                    NextStepCallAction(onClick = { onCall(digits) })
                }
                NextStepChatAction(step = step, onChat = onChat)
            }
        }
    }
}

@Composable
private fun NextStepCallAction(onClick: () -> Unit) {
    KiNDDPrimaryGradientCapsule(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(7.dp))
            Text(stringResource(R.string.home_call_now), color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun NextStepChatAction(step: HomeStep, onChat: (ChatLaunchPrompt) -> Unit) {
    KiNDDSecondaryCapsule(onClick = { onChat(step.prompt) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = KiNDDViolet, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(step.actionRes), color = KiNDDViolet, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun HomeQuestionSection(
    profile: UserProfile,
    onChat: (ChatLaunchPrompt) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(start = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                stringResource(R.string.home_parity_how_help),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                stringResource(R.string.home_parity_how_help_support),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            QuestionRow(
                icon = Icons.Default.AutoAwesome,
                tint = KiNDDViolet,
                text = stringResource(R.string.chat_prompt_first_steps),
                onClick = { onChat(ChatLaunchPrompt.FIRST_STEPS) }
            )
            QuestionRow(
                icon = Icons.Default.Psychology,
                tint = KiNDDPink,
                text = stringResource(R.string.home_parity_question_aba),
                onClick = { onChat(ChatLaunchPrompt.FIND_ABA_NEARBY) }
            )
            val center = profile.regionalCenter
            QuestionRow(
                icon = Icons.Default.AccountBalance,
                tint = KiNDDIndigo,
                text = center?.let {
                    stringResource(R.string.home_parity_question_funding, it.shortName)
                } ?: stringResource(R.string.home_parity_question_center_zip),
                onClick = {
                    onChat(
                        if (center == null) ChatLaunchPrompt.FIND_REGIONAL_CENTER
                        else ChatLaunchPrompt.CENTER_FUNDING
                    )
                }
            )
        }
    }
}

@Composable
private fun QuestionRow(
    icon: ImageVector,
    tint: Color,
    text: String,
    onClick: () -> Unit
) {
    KiNDDCardSurface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun HomeInfoFooter(
    onAbout: () -> Unit,
    onFaq: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            KiNDDSecondaryCapsule(onClick = onAbout) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = KiNDDIndigo, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.about), color = KiNDDIndigo)
                }
            }
            KiNDDSecondaryCapsule(onClick = onFaq) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QuestionMark, contentDescription = null, tint = KiNDDIndigo, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.faq), color = KiNDDIndigo)
                }
            }
        }
    }
}

private data class HomeStep(
    @StringRes val titleRes: Int,
    val detail: String,
    @StringRes val actionRes: Int,
    val prompt: ChatLaunchPrompt,
    val showsCall: Boolean
)
