package com.chla.kindd.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chla.kindd.BuildConfig
import com.chla.kindd.data.api.KINDDApi
import com.chla.kindd.data.api.AssistantResponseReportReason
import com.chla.kindd.data.api.AssistantResponseReportRequest
import com.chla.kindd.data.api.LLMRequest
import com.chla.kindd.data.models.ChatMessage
import com.chla.kindd.di.IoDispatcher
import com.google.gson.JsonParser
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import retrofit2.HttpException

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: ChatFailure? = null,
    val responseReport: ResponseReportUiState = ResponseReportUiState()
)

data class ResponseReportUiState(
    val messageId: String? = null,
    val status: ResponseReportStatus = ResponseReportStatus.IDLE
)

enum class ResponseReportStatus {
    IDLE,
    SUBMITTING,
    SUCCESS,
    FAILURE,
    TERMINAL_FAILURE
}

enum class ChatFailure {
    REQUEST_FAILED
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val api: KINDDApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    private var requestJob: Job? = null
    private var requestGeneration = 0L
    private var reportJob: Job? = null
    private var reportGeneration = 0L

    fun sendInitialPrompt(key: String, resolvedText: String): Boolean {
        if (com.chla.kindd.ui.chat.ChatLaunchPrompt.fromRouteValue(key) == null) return false
        return trySendMessage(resolvedText)
    }

    fun sendMessage(content: String) {
        trySendMessage(content)
    }

    private fun trySendMessage(content: String): Boolean {
        if (_uiState.value.isLoading) return false

        val userMessage = ChatMessage(
            role = ChatMessage.Role.USER,
            content = content
        )

        _uiState.update { 
            it.copy(
                messages = it.messages + userMessage,
                isLoading = true,
                error = null
            )
        }

        startRequest(content)
        return true
    }

    fun retryLastMessage() {
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.error == null) return
        val question = currentState.messages.lastOrNull(ChatMessage::isUser)?.content ?: return
        _uiState.update { it.copy(isLoading = true, error = null) }
        startRequest(question)
    }

    private fun startRequest(content: String) {
        val generation = ++requestGeneration
        requestJob = viewModelScope.launch {
            try {
                val locale = Locale.getDefault().language
                val response = withContext(ioDispatcher) {
                    api.askLLM(
                        LLMRequest(
                            query = content,
                            locale = locale
                        )
                    )
                }

                val assistantMessage = ChatMessage(
                    role = ChatMessage.Role.ASSISTANT,
                    content = response.answer,
                    responseFingerprint = response.responseFingerprint
                )

                if (generation == requestGeneration) {
                    _uiState.update {
                        it.copy(
                            messages = it.messages + assistantMessage,
                            isLoading = false
                        )
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                if (generation == requestGeneration) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = ChatFailure.REQUEST_FAILED
                        )
                    }
                }
            }
        }
    }

    fun reportResponse(messageId: String, reason: AssistantResponseReportReason) {
        if (_uiState.value.responseReport.status == ResponseReportStatus.SUBMITTING) return
        val response = _uiState.value.messages
            .firstOrNull { it.id == messageId && it.isAssistant }
            ?: return
        val responseFingerprint = response.responseFingerprint
            ?.takeIf(String::isNotBlank)
            ?: return
        val generation = ++reportGeneration
        _uiState.update {
            it.copy(
                responseReport = ResponseReportUiState(
                    messageId = messageId,
                    status = ResponseReportStatus.SUBMITTING
                )
            )
        }
        reportJob = viewModelScope.launch {
            try {
                withContext(ioDispatcher) {
                    api.reportAssistantResponse(
                        AssistantResponseReportRequest(
                            reason = reason,
                            reportedResponse = response.content,
                            locale = Locale.getDefault().language.ifBlank { "und" },
                            appVersion = BuildConfig.VERSION_NAME,
                            responseFingerprint = responseFingerprint
                        )
                    )
                }
                if (generation == reportGeneration) {
                    _uiState.update {
                        it.copy(
                            messages = it.messages.withoutResponseFingerprint(messageId),
                            responseReport = ResponseReportUiState(
                                messageId = messageId,
                                status = ResponseReportStatus.SUCCESS
                            )
                        )
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                if (generation == reportGeneration) {
                    val terminalFailure = error.isTerminalResponseReportFailure()
                    _uiState.update {
                        it.copy(
                            messages = if (terminalFailure) {
                                it.messages.withoutResponseFingerprint(messageId)
                            } else {
                                it.messages
                            },
                            responseReport = ResponseReportUiState(
                                messageId = messageId,
                                status = if (terminalFailure) {
                                    ResponseReportStatus.TERMINAL_FAILURE
                                } else {
                                    ResponseReportStatus.FAILURE
                                }
                            )
                        )
                    }
                }
            }
        }
    }

    fun dismissResponseReport() {
        if (_uiState.value.responseReport.status == ResponseReportStatus.SUBMITTING) return
        _uiState.update { it.copy(responseReport = ResponseReportUiState()) }
    }

    fun clearChat() {
        requestGeneration += 1
        requestJob?.cancel()
        requestJob = null
        reportGeneration += 1
        reportJob?.cancel()
        reportJob = null
        _uiState.value = ChatUiState()
    }
}

private fun List<ChatMessage>.withoutResponseFingerprint(messageId: String): List<ChatMessage> =
    map { message ->
        if (message.id == messageId) message.copy(responseFingerprint = null) else message
    }

private fun Throwable.isTerminalResponseReportFailure(): Boolean {
    val httpError = this as? HttpException ?: return false
    val errorBody = httpError.response()?.errorBody() ?: return false
    return runCatching {
        val error = JsonParser().parse(errorBody.string()).asJsonObject
        when (httpError.code()) {
            400 -> error.get("code")?.asString == "invalid_response_fingerprint"
            429 -> error.get("detail")?.asString ==
                "This assistant response has already been reported."
            else -> false
        }
    }.getOrDefault(false)
}
