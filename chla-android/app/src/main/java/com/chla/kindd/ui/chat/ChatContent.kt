package com.chla.kindd.ui.chat

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.chla.kindd.R
import com.chla.kindd.data.api.AssistantResponseReportReason
import com.chla.kindd.data.models.ChatMessage
import com.chla.kindd.ui.screens.ChatUiState
import com.chla.kindd.ui.screens.ResponseReportStatus
import com.chla.kindd.ui.screens.ResponseReportUiState
import com.chla.kindd.ui.theme.KiNDDAiGradient
import com.chla.kindd.ui.theme.KiNDDCardSurface
import com.chla.kindd.ui.theme.KiNDDIndigo
import com.chla.kindd.ui.theme.KiNDDPrimaryActionGradient
import com.chla.kindd.ui.theme.KiNDDViolet
import com.chla.kindd.ui.theme.kinddTopWash

private data class PromptCapsule(
    @StringRes val labelRes: Int,
    @StringRes val promptRes: Int,
    val icon: ImageVector,
    val tag: String
)

private data class ResponseReportStrings(
    val title: String,
    val prompt: String,
    val unsafeReason: String,
    val inaccurateReason: String,
    val otherReason: String,
    val submit: String,
    val submitting: String,
    val success: String,
    val failure: String,
    val terminalFailure: String,
    val retry: String,
    val done: String,
    val cancel: String
) {
    fun labelFor(reason: AssistantResponseReportReason) = when (reason) {
        AssistantResponseReportReason.UNSAFE_OR_INAPPROPRIATE -> unsafeReason
        AssistantResponseReportReason.INACCURATE_OR_MISLEADING -> inaccurateReason
        AssistantResponseReportReason.OTHER -> otherReason
    }
}

private val promptCapsules = listOf(
    PromptCapsule(
        R.string.chat_prompt_near_me,
        R.string.suggestion_find_providers,
        Icons.Default.LocationOn,
        "chat_prompt_near_me"
    ),
    PromptCapsule(
        R.string.chat_prompt_research,
        R.string.chat_prompt_research_query,
        Icons.Default.Search,
        "chat_prompt_research"
    ),
    PromptCapsule(
        R.string.chat_prompt_assessment,
        R.string.suggestion_eligibility,
        Icons.Default.Checklist,
        "chat_prompt_assessment"
    ),
    PromptCapsule(
        R.string.chat_prompt_centers,
        R.string.suggestion_regional_center,
        Icons.Default.QuestionAnswer,
        "chat_prompt_centers"
    )
)

@Composable
fun ChatContent(
    uiState: ChatUiState,
    onSend: (String) -> Unit,
    onRetry: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    onReportResponse: (String, AssistantResponseReportReason) -> Unit = { _, _ -> },
    onDismissResponseReport: () -> Unit = {}
) {
    var inputText by rememberSaveable { mutableStateOf("") }
    var showClearConfirmation by rememberSaveable { mutableStateOf(false) }
    var reportMessage by remember { mutableStateOf<ChatMessage?>(null) }
    val listState = rememberLazyListState()
    val responseReportStrings = responseReportStrings()

    LaunchedEffect(uiState.messages.size, uiState.error, uiState.isLoading) {
        val target = uiState.messages.size +
            (if (uiState.error != null) 1 else 0) +
            (if (uiState.isLoading) 1 else 0)
        if (target > 0) listState.animateScrollToItem(target - 1)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("chat_grouped_canvas")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .background(kinddTopWash())
        )
        Column(modifier = Modifier.fillMaxSize()) {
            ChatToolbar(
                hasMessages = uiState.messages.isNotEmpty(),
                onRequestClear = { showClearConfirmation = true }
            )
            ChatPromptCapsules(
                enabled = !uiState.isLoading,
                onSend = onSend
            )
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.messages.isEmpty()) {
                    item { ChatWelcomeCard() }
                } else {
                    itemsIndexed(uiState.messages) { index, message ->
                        val hasTerminalReportFailure =
                            uiState.responseReport.messageId == message.id &&
                                uiState.responseReport.status ==
                                ResponseReportStatus.TERMINAL_FAILURE
                        ChatMessageCard(
                            message = message,
                            index = index,
                            canReport = !hasTerminalReportFailure,
                            onReport = { reportMessage = message }
                        )
                    }
                }
                if (uiState.isLoading) {
                    item { ChatLoadingCard() }
                }
                if (uiState.error != null) {
                    item { ChatErrorCard(onRetry = onRetry) }
                }
            }
            ChatComposer(
                value = inputText,
                onValueChange = { inputText = it },
                loading = uiState.isLoading,
                onSend = {
                    val message = inputText.trim()
                    if (message.isNotEmpty() && !uiState.isLoading) {
                        inputText = ""
                        onSend(message)
                    }
                }
            )
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            modifier = Modifier.testTag("chat_clear_confirmation"),
            title = { Text(stringResource(R.string.chat_clear_title)) },
            text = { Text(stringResource(R.string.chat_clear_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation = false
                        onClear()
                    },
                    modifier = Modifier.testTag("chat_confirm_clear")
                ) {
                    Text(
                        text = stringResource(R.string.chat_clear_confirm),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    reportMessage?.let { message ->
        val reportState = if (uiState.responseReport.messageId == message.id) {
            uiState.responseReport
        } else {
            ResponseReportUiState()
        }
        ChatResponseReportDialog(
            state = reportState,
            strings = responseReportStrings,
            onSubmit = { reason -> onReportResponse(message.id, reason) },
            onDismiss = {
                if (reportState.status != ResponseReportStatus.SUBMITTING) {
                    reportMessage = null
                    onDismissResponseReport()
                }
            }
        )
    }
}

@Composable
private fun ChatToolbar(
    hasMessages: Boolean,
    onRequestClear: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("chat_toolbar"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.size(48.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = KiNDDViolet,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = stringResource(R.string.chat_title),
                modifier = Modifier.testTag("chat_toolbar_title"),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            if (hasMessages) {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("chat_overflow")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.chat_more_actions)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.clear_chat)) },
                        leadingIcon = {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            onRequestClear()
                        },
                        modifier = Modifier.testTag("chat_clear_action")
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatPromptCapsules(
    enabled: Boolean,
    onSend: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_prompt_capsules"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(promptCapsules, key = PromptCapsule::tag) { prompt ->
            val fullPrompt = stringResource(prompt.promptRes)
            Surface(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .alpha(if (enabled) 1f else 0.54f)
                    .testTag(prompt.tag)
                    .clickable(enabled = enabled, role = Role.Button) { onSend(fullPrompt) }
                    .semantics {
                        role = Role.Button
                        if (!enabled) disabled()
                    },
                shape = RoundedCornerShape(percent = 50),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(
                        imageVector = prompt.icon,
                        contentDescription = null,
                        tint = KiNDDIndigo,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(prompt.labelRes),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatWelcomeCard() {
    KiNDDCardSurface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_welcome_card"),
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(KiNDDAiGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Text(
                text = stringResource(R.string.chat_welcome_heading),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.chat_welcome_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChatMessageCard(
    message: ChatMessage,
    index: Int,
    canReport: Boolean,
    onReport: () -> Unit
) {
    val isUser = message.isUser
    val bubbleShape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isUser) Modifier
                else Modifier.testTag("chat_assistant_message_$index")
            ),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(KiNDDAiGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = stringResource(R.string.chat_assistant_message),
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
            }
            Spacer(Modifier.width(7.dp))
        }
        Box(
            modifier = Modifier
                .then(
                    if (isUser) Modifier.widthIn(max = 310.dp)
                    else Modifier.weight(1f)
                )
                .then(
                    if (isUser) Modifier
                    else Modifier.border(
                        width = 1.dp,
                        color = KiNDDViolet.copy(alpha = 0.48f),
                        shape = bubbleShape
                    )
                )
                .clip(bubbleShape)
                .background(
                    if (isUser) KiNDDPrimaryActionGradient
                    else Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .testTag(if (isUser) "chat_user_message_$index" else "chat_assistant_edge_$index")
        ) {
            if (isUser) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SafeMarkdownText(
                        markdown = message.content,
                        modifier = Modifier.testTag("chat_assistant_markdown_$index"),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    if (
                        message.role == ChatMessage.Role.ASSISTANT &&
                        !message.responseFingerprint.isNullOrBlank() &&
                        canReport
                    ) {
                        TextButton(
                            onClick = onReport,
                            modifier = Modifier
                                .align(Alignment.End)
                                .heightIn(min = 48.dp)
                                .testTag("chat_report_response_$index")
                        ) {
                            Text(stringResource(R.string.chat_report_response))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatResponseReportDialog(
    state: ResponseReportUiState,
    strings: ResponseReportStrings,
    onSubmit: (AssistantResponseReportReason) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedReason by remember { mutableStateOf<AssistantResponseReportReason?>(null) }
    val isSubmitting = state.status == ResponseReportStatus.SUBMITTING
    val isSuccess = state.status == ResponseReportStatus.SUCCESS
    val isTerminalFailure = state.status == ResponseReportStatus.TERMINAL_FAILURE
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("chat_report_dialog"),
        title = { Text(strings.title) },
        text = {
            if (isSuccess) {
                Text(
                    text = strings.success,
                    modifier = Modifier
                        .testTag("chat_report_success")
                        .semantics { liveRegion = LiveRegionMode.Polite }
                )
            } else if (isTerminalFailure) {
                ReportStatusText(
                    text = strings.terminalFailure,
                    tag = "chat_report_terminal_failure"
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(strings.prompt)
                    AssistantResponseReportReason.entries.forEach { reason ->
                        val selected = reason == selectedReason
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .selectable(
                                    selected = selected,
                                    enabled = !isSubmitting,
                                    role = Role.RadioButton,
                                    onClick = { selectedReason = reason }
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = null,
                                enabled = !isSubmitting
                            )
                            Text(
                                text = strings.labelFor(reason),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    when (state.status) {
                        ResponseReportStatus.SUBMITTING -> ReportStatusText(
                            text = strings.submitting,
                            tag = "chat_report_submitting"
                        )
                        ResponseReportStatus.FAILURE -> ReportStatusText(
                            text = strings.failure,
                            tag = "chat_report_failure"
                        )
                        else -> Unit
                    }
                }
            }
        },
        confirmButton = {
            if (isSuccess || isTerminalFailure) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(strings.done)
                }
            } else {
                TextButton(
                    onClick = { selectedReason?.let(onSubmit) },
                    enabled = selectedReason != null && !isSubmitting,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("chat_submit_report")
                ) {
                    Text(
                        if (state.status == ResponseReportStatus.FAILURE) strings.retry
                        else if (isSubmitting) strings.submitting
                        else strings.submit
                    )
                }
            }
        },
        dismissButton = {
            if (!isSuccess && !isTerminalFailure) {
                TextButton(
                    onClick = onDismiss,
                    enabled = !isSubmitting,
                    modifier = Modifier.heightIn(min = 48.dp)
                ) {
                    Text(strings.cancel)
                }
            }
        }
    )
}

@Composable
private fun ReportStatusText(text: String, tag: String) {
    Text(
        text = text,
        modifier = Modifier
            .testTag(tag)
            .semantics { liveRegion = LiveRegionMode.Polite },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun responseReportStrings() = ResponseReportStrings(
    title = stringResource(R.string.chat_report_title),
    prompt = stringResource(R.string.chat_report_prompt),
    unsafeReason = stringResource(R.string.chat_report_reason_unsafe),
    inaccurateReason = stringResource(R.string.chat_report_reason_inaccurate),
    otherReason = stringResource(R.string.chat_report_reason_other),
    submit = stringResource(R.string.chat_report_submit),
    submitting = stringResource(R.string.chat_report_submitting),
    success = stringResource(R.string.chat_report_success),
    failure = stringResource(R.string.chat_report_failure),
    terminalFailure = stringResource(R.string.chat_report_terminal_failure),
    retry = stringResource(R.string.retry),
    done = stringResource(R.string.done),
    cancel = stringResource(R.string.cancel)
)

@Composable
private fun ChatLoadingCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_loading"),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            color = KiNDDIndigo,
            strokeWidth = 2.dp
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ChatErrorCard(onRetry: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_error")
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(KiNDDAiGradient),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(13.dp)
            )
        }
        Spacer(Modifier.width(7.dp))
        Surface(
            modifier = Modifier
                .weight(1f)
                .border(
                    width = 1.dp,
                    color = KiNDDViolet.copy(alpha = 0.48f),
                    shape = shape
                ),
            shape = shape,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    text = stringResource(R.string.chat_request_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(
                    onClick = onRetry,
                    modifier = Modifier.testTag("chat_retry")
                ) {
                    Text(
                        text = stringResource(R.string.retry),
                        color = KiNDDIndigo
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatComposer(
    value: String,
    onValueChange: (String) -> Unit,
    loading: Boolean,
    onSend: () -> Unit
) {
    val canSend = value.isNotBlank() && !loading
    val sendLabel = stringResource(R.string.send)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_composer"),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input"),
                enabled = !loading,
                placeholder = { Text(stringResource(R.string.chat_composer_placeholder)) },
                shape = RoundedCornerShape(24.dp),
                maxLines = 4,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (canSend) KiNDDPrimaryActionGradient
                        else Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.34f),
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.34f)
                            )
                        )
                    )
                    .clickable(enabled = canSend, role = Role.Button, onClick = onSend)
                    .testTag("chat_send")
                    .semantics {
                        contentDescription = sendLabel
                        role = Role.Button
                        if (!canSend) disabled()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        tint = if (canSend) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
