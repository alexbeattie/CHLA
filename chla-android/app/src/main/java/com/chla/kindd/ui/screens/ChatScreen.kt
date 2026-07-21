package com.chla.kindd.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.chla.kindd.ui.chat.ChatContent
import com.chla.kindd.ui.chat.ChatLaunchPrompt

@Composable
fun ChatScreen(
    initialPrompt: ChatLaunchPrompt? = null,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val resolvedInitialPrompt = initialPrompt?.let { prompt ->
        stringResource(prompt.promptResId)
    }
    var initialPromptDispatched by rememberSaveable(initialPrompt?.routeValue) {
        mutableStateOf(false)
    }

    LaunchedEffect(initialPrompt, resolvedInitialPrompt, uiState.isLoading) {
        if (!initialPromptDispatched && initialPrompt != null && resolvedInitialPrompt != null) {
            initialPromptDispatched = viewModel.sendInitialPrompt(
                initialPrompt.routeValue,
                resolvedInitialPrompt
            )
        }
    }

    ChatContent(
        uiState = uiState,
        onSend = viewModel::sendMessage,
        onRetry = viewModel::retryLastMessage,
        onClear = viewModel::clearChat,
        onReportResponse = viewModel::reportResponse,
        onDismissResponseReport = viewModel::dismissResponseReport
    )
}
