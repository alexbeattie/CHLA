package com.chla.kindd.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chla.kindd.data.api.KINDDApi
import com.chla.kindd.data.api.LLMRequest
import com.chla.kindd.data.models.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val api: KINDDApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

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
                val response = withContext(Dispatchers.IO) {
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
            } catch (e: Exception) {
                val errorMessage = ChatMessage(
                    role = ChatMessage.Role.ASSISTANT,
                    content = "I'm sorry, I encountered an error. Please try again."
                )

                _uiState.update {
                    it.copy(
                        messages = it.messages + errorMessage,
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun clearChat() {
        _uiState.update { it.copy(messages = emptyList()) }
    }
}
