package com.chla.kindd.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chla.kindd.data.api.KINDDApi
import com.chla.kindd.data.api.LLMRequest
import com.chla.kindd.data.models.ChatMessage
import com.chla.kindd.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: ChatFailure? = null
)

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
    private val handledInitialPromptKeys = mutableSetOf<String>()

    fun sendInitialPrompt(key: String, resolvedText: String) {
        if (com.chla.kindd.ui.chat.ChatLaunchPrompt.fromRouteValue(key) == null) return
        if (!handledInitialPromptKeys.add(key)) return
        sendMessage(resolvedText)
    }

    fun sendMessage(content: String) {
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

        viewModelScope.launch {
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
                    content = response.answer
                )

                _uiState.update {
                    it.copy(
                        messages = it.messages + assistantMessage,
                        isLoading = false
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = ChatFailure.REQUEST_FAILED
                    )
                }
            }
        }
    }

    fun clearChat() {
        _uiState.update { it.copy(messages = emptyList()) }
    }
}
