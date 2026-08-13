package com.navigator.kindd.ui.chat

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.navigator.kindd.data.api.AssistantResponseReportReason
import com.navigator.kindd.data.models.ChatMessage
import com.navigator.kindd.ui.screens.ChatUiState
import com.navigator.kindd.ui.screens.ResponseReportStatus
import com.navigator.kindd.ui.screens.ResponseReportUiState
import com.navigator.kindd.ui.theme.KINDDTheme
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ChatResponseReportTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun onlyFingerprintedAssistantCardsExposeAnAccessibleReportAction() {
        composeRule.setContent {
            KINDDTheme {
                ChatContent(
                    uiState = reportableConversation(),
                    onSend = {},
                    onRetry = {},
                    onClear = {}
                )
            }
        }

        composeRule.onNodeWithTag("chat_report_response_0").assertDoesNotExist()
        composeRule.onNodeWithTag("chat_report_response_1").assertDoesNotExist()
        composeRule.onNodeWithTag("chat_report_response_2").assertDoesNotExist()
        val action = composeRule.onNodeWithTag("chat_report_response_3")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextEquals("Report response")
            .fetchSemanticsNode()
        val minimumPixels = 48f * composeRule.density.density
        assertTrue(action.boundsInRoot.height >= minimumPixels)
    }

    @Test
    fun reportDialogSubmitsTheSelectedFixedReasonOnceAndShowsSuccess() {
        var uiState by mutableStateOf(reportableConversation())
        val submissions = mutableListOf<Pair<String, AssistantResponseReportReason>>()
        composeRule.setContent {
            KINDDTheme {
                ChatContent(
                    uiState = uiState,
                    onSend = {},
                    onRetry = {},
                    onClear = {},
                    onReportResponse = { messageId, reason ->
                        submissions += messageId to reason
                        uiState = uiState.copy(
                            responseReport = ResponseReportUiState(
                                messageId,
                                ResponseReportStatus.SUBMITTING
                            )
                        )
                    },
                    onDismissResponseReport = {}
                )
            }
        }

        composeRule.onNodeWithTag("chat_report_response_3").performScrollTo().performClick()
        composeRule.onNodeWithTag("chat_report_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Unsafe or inappropriate").assertIsDisplayed()
        composeRule.onNodeWithText("Inaccurate or misleading").performClick()
        composeRule.onNodeWithText("Other").assertIsDisplayed()
        composeRule.onNodeWithTag("chat_submit_report").performClick()
        composeRule.onNodeWithTag("chat_submit_report").assertIsNotEnabled().performClick()

        composeRule.runOnIdle {
            assertEquals(
                listOf("assistant-id" to AssistantResponseReportReason.INACCURATE_OR_MISLEADING),
                submissions
            )
            uiState = uiState.copy(
                responseReport = ResponseReportUiState(
                    "assistant-id",
                    ResponseReportStatus.SUCCESS
                )
            )
        }
        composeRule.onNodeWithTag("chat_report_success").assertIsDisplayed()
    }

    @Test
    fun terminalFingerprintFailureRemovesReportActionAndRetryControl() {
        var uiState by mutableStateOf(reportableConversation())
        composeRule.setContent {
            KINDDTheme {
                ChatContent(
                    uiState = uiState,
                    onSend = {},
                    onRetry = {},
                    onClear = {}
                )
            }
        }

        composeRule.onNodeWithTag("chat_report_response_3").performScrollTo().performClick()
        composeRule.runOnIdle {
            uiState = uiState.copy(
                responseReport = ResponseReportUiState(
                    "assistant-id",
                    ResponseReportStatus.TERMINAL_FAILURE
                )
            )
        }

        composeRule.onNodeWithTag("chat_report_terminal_failure").assertIsDisplayed()
        composeRule.onNodeWithText("This response can no longer be reported.")
            .assertIsDisplayed()
        composeRule.onNodeWithTag("chat_submit_report").assertDoesNotExist()
        composeRule.onNodeWithTag("chat_report_response_3").assertDoesNotExist()
    }

    @Test
    fun spanishReportDialogAtNarrowWidthAndLargeTextRemainsUsable() {
        setNarrowLocalizedContent(
            locale = Locale.forLanguageTag("es"),
            widthDp = 320,
            fontScale = 1.3f
        ) {
            ChatContent(
                uiState = reportableConversation(),
                onSend = {},
                onRetry = {},
                onClear = {}
            )
        }

        val report = composeRule.onNodeWithTag("chat_report_response_3")
            .performScrollTo()
            .assertTextEquals("Reportar respuesta")
            .fetchSemanticsNode()
        assertTrue(report.boundsInRoot.left >= 0f)
        assertTrue(report.boundsInRoot.right <= 320f * composeRule.density.density)
        composeRule.onNodeWithTag("chat_report_response_3")
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.onNodeWithTag("chat_report_dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Inexacta o engañosa").performClick()
        composeRule.onNodeWithTag("chat_submit_report").assertIsDisplayed().performClick()
    }

    private fun reportableConversation() = ChatUiState(
        messages = listOf(
            ChatMessage(id = "user-id", role = ChatMessage.Role.USER, content = "A question"),
            ChatMessage(
                id = "system-id",
                role = ChatMessage.Role.SYSTEM,
                content = "A system message",
                responseFingerprint = "opaque-system-token"
            ),
            ChatMessage(
                id = "assistant-without-fingerprint",
                role = ChatMessage.Role.ASSISTANT,
                content = "An old assistant answer"
            ),
            ChatMessage(
                id = "assistant-id",
                role = ChatMessage.Role.ASSISTANT,
                content = "An assistant answer",
                responseFingerprint = "opaque-signed-token"
            )
        )
    )

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
}
