package com.chla.kindd.ui.chat

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.chla.kindd.data.api.KINDDApi
import com.chla.kindd.data.api.LLMRequest
import com.chla.kindd.data.api.LLMResponse
import com.chla.kindd.data.models.ChatMessage
import com.chla.kindd.ui.screens.ChatFailure
import com.chla.kindd.ui.screens.ChatScreen
import com.chla.kindd.ui.screens.ChatUiState
import com.chla.kindd.ui.screens.ChatViewModel
import com.chla.kindd.ui.theme.KINDDTheme
import java.lang.reflect.Proxy
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ChatContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyChat_usesLiteralSheetHierarchy_andAccessibleDisabledComposer() {
        composeRule.setContent {
            KINDDTheme {
                ChatScreen(viewModel = ChatViewModel(successApi(), Dispatchers.Unconfined))
            }
        }

        composeRule.onNodeWithTag("chat_grouped_canvas").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_toolbar").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_prompt_capsules").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_welcome_card").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_composer").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_solid_top_app_bar").assertDoesNotExist()

        val send = composeRule.onNodeWithTag("chat_send")
            .assertIsDisplayed()
            .assertIsNotEnabled()
            .fetchSemanticsNode()
        val minimumPixels = 48f * composeRule.density.density
        assertTrue(send.boundsInRoot.width >= minimumPixels)
        assertTrue(send.boundsInRoot.height >= minimumPixels)
        assertTrue(
            send.config.getOrNull(SemanticsProperties.ContentDescription)
                .orEmpty()
                .isNotEmpty()
        )
    }

    @Test
    fun messageFlow_usesGradientAndNeutralCards_retainsSanitizedError_andConfirmsClear() {
        var clearCount = 0
        composeRule.setContent {
            KINDDTheme {
                ChatContent(
                    uiState = ChatUiState(
                        messages = listOf(
                            ChatMessage(
                                role = ChatMessage.Role.USER,
                                content = "A private question"
                            ),
                            ChatMessage(
                                role = ChatMessage.Role.ASSISTANT,
                                content = "A helpful answer"
                            )
                        ),
                        error = ChatFailure.REQUEST_FAILED
                    ),
                    onSend = {},
                    onClear = { clearCount += 1 }
                )
            }
        }

        composeRule.onNodeWithTag("chat_user_message_0").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_assistant_message_1").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_error").assertIsDisplayed()
        composeRule.onNodeWithText("We couldn't get a response. Please try again.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("private backend text").assertDoesNotExist()

        composeRule.onNodeWithTag("chat_overflow").performClick()
        composeRule.onNodeWithTag("chat_clear_action").performClick()
        composeRule.onNodeWithTag("chat_clear_confirmation").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(0, clearCount) }
        composeRule.onNodeWithTag("chat_confirm_clear").performClick()
        composeRule.runOnIdle { assertEquals(1, clearCount) }
    }

    @Test
    fun composer_oneTapSendsExactlyOnce_thenShowsLoadingAndDisablesSend() {
        var uiState by mutableStateOf(ChatUiState())
        val sends = mutableListOf<String>()
        composeRule.setContent {
            KINDDTheme {
                ChatContent(
                    uiState = uiState,
                    onSend = { message ->
                        sends += message
                        uiState = uiState.copy(isLoading = true)
                    },
                    onClear = {}
                )
            }
        }

        composeRule.onNodeWithTag("chat_input").performTextInput("  Help me find services  ")
        composeRule.onNodeWithTag("chat_send").performClick()

        composeRule.runOnIdle { assertEquals(listOf("Help me find services"), sends) }
        composeRule.onNodeWithTag("chat_loading").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_send").assertIsNotEnabled()
    }

    private fun successApi(): KINDDApi =
        Proxy.newProxyInstance(
            KINDDApi::class.java.classLoader,
            arrayOf(KINDDApi::class.java)
        ) { _, method, args ->
            if (method.name == "askLLM") {
                val request = args!![0] as LLMRequest
                LLMResponse(query = request.query, answer = "answer")
            } else {
                throw UnsupportedOperationException(method.name)
            }
        } as KINDDApi
}
