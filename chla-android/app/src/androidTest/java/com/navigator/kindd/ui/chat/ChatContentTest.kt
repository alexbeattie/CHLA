package com.navigator.kindd.ui.chat

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.navigator.kindd.data.api.KINDDApi
import com.navigator.kindd.data.api.LLMRequest
import com.navigator.kindd.data.api.LLMResponse
import com.navigator.kindd.data.models.ChatMessage
import com.navigator.kindd.ui.screens.ChatFailure
import com.navigator.kindd.ui.screens.ChatScreen
import com.navigator.kindd.ui.screens.ChatUiState
import com.navigator.kindd.ui.screens.ChatViewModel
import com.navigator.kindd.ui.theme.KINDDTheme
import java.lang.reflect.Proxy
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.Locale

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
    fun researchPrompt_sendsTheDedicatedLocalizedQuestionExactlyOnce() {
        val sends = mutableListOf<String>()
        composeRule.setContent {
            KINDDTheme {
                ChatContent(
                    uiState = ChatUiState(),
                    onSend = { sends += it },
                    onRetry = {},
                    onClear = {}
                )
            }
        }

        composeRule.onNodeWithTag("chat_prompt_research")
            .performScrollTo()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(
                listOf("Which genes have the strongest SFARI evidence for autism?"),
                sends
            )
        }
    }

    @Test
    fun loading_disablesSuggestionPromptsAsWellAsTheComposer() {
        composeRule.setContent {
            KINDDTheme {
                ChatContent(
                    uiState = ChatUiState(isLoading = true),
                    onSend = {},
                    onRetry = {},
                    onClear = {}
                )
            }
        }

        composeRule.onNodeWithTag("chat_prompt_near_me").assertIsNotEnabled()
        composeRule.onNodeWithTag("chat_prompt_research").assertIsNotEnabled()
        composeRule.onNodeWithTag("chat_send").assertIsNotEnabled()
    }

    @Test
    fun assistantMessage_rendersSafeContractMarkdownInsideAVioletEdgedCard() {
        composeRule.setContent {
            KINDDTheme {
                ChatContent(
                    uiState = ChatUiState(
                        messages = listOf(
                            ChatMessage(
                                role = ChatMessage.Role.ASSISTANT,
                                content = "**Next step:**\n- Read [KiNDD](https://kinddhelp.com)."
                            )
                        )
                    ),
                    onSend = {},
                    onRetry = {},
                    onClear = {}
                )
            }
        }

        composeRule.onNodeWithTag("chat_assistant_message_0").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_assistant_edge_0").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_assistant_markdown_0")
            .assertTextEquals("Next step:\n• Read KiNDD.")
    }

    @Test
    fun safeMarkdownLink_opensOnlyTheAnnotatedWebDestination() {
        var openedUrl: String? = null
        composeRule.setContent {
            KINDDTheme {
                SafeMarkdownText(
                    markdown = "[Source](https://kinddhelp.com)",
                    modifier = Modifier.testTag("safe_markdown_link"),
                    onOpenLink = { openedUrl = it }
                )
            }
        }

        composeRule.onNodeWithTag("safe_markdown_link").performClick()

        composeRule.runOnIdle {
            assertEquals("https://kinddhelp.com", openedUrl)
        }
    }

    @Test
    fun safeMarkdownLinks_exposeNamedTalkBackActionsForEachDestination() {
        val openedUrls = mutableListOf<String>()
        composeRule.setContent {
            KINDDTheme {
                SafeMarkdownText(
                    markdown = "[KiNDD](https://kinddhelp.org) and " +
                        "[DDS](https://www.dds.ca.gov)",
                    modifier = Modifier.testTag("safe_markdown_accessible_links"),
                    onOpenLink = openedUrls::add
                )
            }
        }

        val actions = composeRule.onNodeWithTag("safe_markdown_accessible_links")
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsActions.CustomActions)
            .orEmpty()

        assertEquals(listOf("Open KiNDD", "Open DDS"), actions.map { it.label })
        composeRule.runOnIdle { assertTrue(actions.last().action()) }
        assertEquals(listOf("https://www.dds.ca.gov"), openedUrls)
    }

    @Test
    fun messageFlow_usesGradientAndNeutralCards_retainsRetryableError_andConfirmsClear() {
        var clearCount = 0
        var retryCount = 0
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
                    onRetry = { retryCount += 1 },
                    onClear = { clearCount += 1 }
                )
            }
        }

        composeRule.onNodeWithTag("chat_user_message_0").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_assistant_message_1").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_error").assertIsDisplayed()
        composeRule.onNodeWithText("Your question wasn’t lost", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("private backend text").assertDoesNotExist()
        composeRule.onNodeWithTag("chat_retry").performClick()
        composeRule.runOnIdle { assertEquals(1, retryCount) }

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
                    onRetry = {},
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

    @Test
    fun spanishToolbar_at320DpAndLargeText_keepsTitleClearOfOverflow() {
        val sends = mutableListOf<String>()
        setNarrowLocalizedContent(
            locale = Locale.forLanguageTag("es"),
            widthDp = 320,
            fontScale = 1.3f
        ) {
            ChatContent(
                uiState = ChatUiState(
                    messages = listOf(
                        ChatMessage(
                            role = ChatMessage.Role.USER,
                            content = "Una pregunta"
                        )
                    )
                ),
                onSend = { sends += it },
                onRetry = {},
                onClear = {}
            )
        }

        composeRule.onNodeWithTag("chat_toolbar_title")
            .assertIsDisplayed()
            .assertTextEquals("Pregúntale a KiNDD")
        val titleBounds = composeRule.onNodeWithTag("chat_toolbar_title")
            .fetchSemanticsNode().boundsInRoot
        val overflowBounds = composeRule.onNodeWithTag("chat_overflow")
            .fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Spanish Ask title must not overlap the overflow action",
            titleBounds.right <= overflowBounds.left
        )
        composeRule.onNodeWithTag("chat_prompt_research")
            .performScrollTo()
            .performClick()
        composeRule.runOnIdle {
            assertEquals(
                listOf("¿Qué genes tienen la evidencia más sólida de SFARI para el autismo?"),
                sends
            )
        }
    }

    private fun setNarrowLocalizedContent(
        locale: Locale,
        widthDp: Int,
        fontScale: Float,
        content: @Composable () -> Unit
    ) {
        val baseContext = InstrumentationRegistry.getInstrumentation().targetContext
        val configuration = Configuration(baseContext.resources.configuration).apply {
            setLocale(locale)
            screenWidthDp = widthDp
            screenHeightDp = 720
            this.fontScale = fontScale
        }
        val localizedContext = baseContext.createConfigurationContext(configuration)
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides configuration,
                LocalDensity provides Density(density.density, fontScale)
            ) {
                KINDDTheme {
                    Box(
                        Modifier
                            .width(widthDp.dp)
                            .height(720.dp)
                    ) {
                        content()
                    }
                }
            }
        }
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
