package com.navigator.kindd.ui.screens

import com.navigator.kindd.data.api.AssistantResponseReportReason
import com.navigator.kindd.data.api.AssistantResponseReportRequest
import com.navigator.kindd.data.api.AssistantResponseReportResponse
import com.navigator.kindd.data.api.KINDDApi
import com.navigator.kindd.data.api.LLMRequest
import com.navigator.kindd.data.api.LLMResponse
import com.navigator.kindd.testing.MainDispatcherRule
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resume
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelReportTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun reportResponse_isSingleFlight_andSuccessRetiresTheReportedResponse() =
        runTest(mainDispatcherRule.testDispatcher) {
            val reportRequests = mutableListOf<AssistantResponseReportRequest>()
            val pendingReports = mutableListOf<Continuation<AssistantResponseReportResponse>>()
            val viewModel = ChatViewModel(
                pendingReportApi(reportRequests, pendingReports),
                mainDispatcherRule.testDispatcher
            )
            viewModel.sendMessage("private question")
            runCurrent()
            val message = viewModel.uiState.value.messages.single { it.isAssistant }
            val historyBeforeReport = viewModel.uiState.value.messages

            viewModel.reportResponse(
                message.id,
                AssistantResponseReportReason.INACCURATE_OR_MISLEADING
            )
            viewModel.reportResponse(
                message.id,
                AssistantResponseReportReason.OTHER
            )
            runCurrent()

            assertEquals(1, reportRequests.size)
            assertEquals("An assistant answer", reportRequests.single().reportedResponse)
            assertEquals("android", reportRequests.single().platform)
            assertEquals("opaque-signed-token", reportRequests.single().responseFingerprint)
            assertEquals(
                ResponseReportUiState(message.id, ResponseReportStatus.SUBMITTING),
                viewModel.uiState.value.responseReport
            )
            assertEquals(historyBeforeReport, viewModel.uiState.value.messages)

            pendingReports.single().resume(AssistantResponseReportResponse(17, "received"))
            runCurrent()

            assertEquals(
                ResponseReportUiState(message.id, ResponseReportStatus.SUCCESS),
                viewModel.uiState.value.responseReport
            )
            assertEquals(
                historyBeforeReport.map { it.id to it.content },
                viewModel.uiState.value.messages.map { it.id to it.content }
            )
            assertNull(
                viewModel.uiState.value.messages.single { it.id == message.id }
                    .responseFingerprint
            )

            viewModel.dismissResponseReport()
            viewModel.reportResponse(message.id, AssistantResponseReportReason.OTHER)
            runCurrent()

            assertEquals(1, reportRequests.size)
            assertEquals(ResponseReportUiState(), viewModel.uiState.value.responseReport)
        }

    @Test
    fun reportResponse_exposesRetryableFailure_withoutChangingChatHistory() =
        runTest(mainDispatcherRule.testDispatcher) {
            var reportAttempts = 0
            val viewModel = ChatViewModel(
                reportFailureApi { reportAttempts += 1 },
                mainDispatcherRule.testDispatcher
            )
            viewModel.sendMessage("private question")
            runCurrent()
            val message = viewModel.uiState.value.messages.single { it.isAssistant }
            val historyBeforeReport = viewModel.uiState.value.messages

            viewModel.reportResponse(message.id, AssistantResponseReportReason.OTHER)
            runCurrent()

            assertEquals(
                ResponseReportUiState(message.id, ResponseReportStatus.FAILURE),
                viewModel.uiState.value.responseReport
            )
            assertEquals(
                "opaque-signed-token",
                viewModel.uiState.value.messages.single { it.isAssistant }.responseFingerprint
            )
            assertEquals(historyBeforeReport, viewModel.uiState.value.messages)

            viewModel.reportResponse(message.id, AssistantResponseReportReason.OTHER)
            runCurrent()

            assertEquals(2, reportAttempts)
            assertEquals(
                ResponseReportUiState(message.id, ResponseReportStatus.FAILURE),
                viewModel.uiState.value.responseReport
            )
            assertEquals(
                "opaque-signed-token",
                viewModel.uiState.value.messages.single { it.isAssistant }.responseFingerprint
            )
        }

    @Test
    fun reportResponse_retiresFingerprintAfterTerminalInvalidCapabilityResponse() =
        runTest(mainDispatcherRule.testDispatcher) {
            var reportAttempts = 0
            val viewModel = ChatViewModel(
                terminalReportFailureApi { reportAttempts += 1 },
                mainDispatcherRule.testDispatcher
            )
            viewModel.sendMessage("private question")
            runCurrent()
            val message = viewModel.uiState.value.messages.single { it.isAssistant }

            viewModel.reportResponse(message.id, AssistantResponseReportReason.OTHER)
            runCurrent()

            assertEquals(1, reportAttempts)
            assertEquals(
                ResponseReportUiState(message.id, ResponseReportStatus.TERMINAL_FAILURE),
                viewModel.uiState.value.responseReport
            )
            assertNull(
                viewModel.uiState.value.messages.single { it.id == message.id }
                    .responseFingerprint
            )

            viewModel.dismissResponseReport()
            viewModel.reportResponse(message.id, AssistantResponseReportReason.OTHER)
            runCurrent()

            assertEquals(1, reportAttempts)
            assertEquals(ResponseReportUiState(), viewModel.uiState.value.responseReport)
        }

    @Test
    fun reportResponse_retiresFingerprintAfterDuplicateAlreadyReportedResponse() =
        runTest(mainDispatcherRule.testDispatcher) {
            var reportAttempts = 0
            val viewModel = ChatViewModel(
                duplicateReportFailureApi { reportAttempts += 1 },
                mainDispatcherRule.testDispatcher
            )
            viewModel.sendMessage("private question")
            runCurrent()
            val message = viewModel.uiState.value.messages.single { it.isAssistant }

            viewModel.reportResponse(message.id, AssistantResponseReportReason.OTHER)
            runCurrent()

            assertEquals(1, reportAttempts)
            assertEquals(
                ResponseReportUiState(message.id, ResponseReportStatus.TERMINAL_FAILURE),
                viewModel.uiState.value.responseReport
            )
            assertNull(
                viewModel.uiState.value.messages.single { it.id == message.id }
                    .responseFingerprint
            )

            viewModel.dismissResponseReport()
            viewModel.reportResponse(message.id, AssistantResponseReportReason.OTHER)
            runCurrent()

            assertEquals(1, reportAttempts)
            assertEquals(ResponseReportUiState(), viewModel.uiState.value.responseReport)
        }

    @Test
    fun reportResponse_ignoresUserAndUnknownMessages() =
        runTest(mainDispatcherRule.testDispatcher) {
            val requests = mutableListOf<AssistantResponseReportRequest>()
            val viewModel = ChatViewModel(capturingReportApi(requests), mainDispatcherRule.testDispatcher)
            viewModel.sendMessage("private question")
            runCurrent()
            val userMessage = viewModel.uiState.value.messages.single { it.isUser }
            val assistantMessage = viewModel.uiState.value.messages.single { it.isAssistant }

            viewModel.reportResponse(userMessage.id, AssistantResponseReportReason.OTHER)
            viewModel.reportResponse("unknown", AssistantResponseReportReason.OTHER)
            viewModel.reportResponse(assistantMessage.id, AssistantResponseReportReason.OTHER)
            runCurrent()

            assertEquals(emptyList<AssistantResponseReportRequest>(), requests)
            assertEquals(ResponseReportUiState(), viewModel.uiState.value.responseReport)
        }

    private fun pendingReportApi(
        requests: MutableList<AssistantResponseReportRequest>,
        pending: MutableList<Continuation<AssistantResponseReportResponse>>
    ): KINDDApi = proxyApi { method, args ->
        when (method) {
            "askLLM" -> {
                val request = args!![0] as LLMRequest
                LLMResponse(
                    query = request.query,
                    answer = "An assistant answer",
                    responseFingerprint = "opaque-signed-token"
                )
            }
            "reportAssistantResponse" -> {
                requests += args!![0] as AssistantResponseReportRequest
                @Suppress("UNCHECKED_CAST")
                pending += args!![1] as Continuation<AssistantResponseReportResponse>
                COROUTINE_SUSPENDED
            }
            else -> throw UnsupportedOperationException(method)
        }
    }

    private fun reportFailureApi(onReport: () -> Unit): KINDDApi = proxyApi { method, args ->
        when (method) {
            "askLLM" -> {
                val request = args!![0] as LLMRequest
                LLMResponse(
                    query = request.query,
                    answer = "An assistant answer",
                    responseFingerprint = "opaque-signed-token"
                )
            }
            "reportAssistantResponse" -> {
                onReport()
                throw IllegalStateException("private backend text")
            }
            else -> throw UnsupportedOperationException(method)
        }
    }

    private fun terminalReportFailureApi(onReport: () -> Unit): KINDDApi =
        proxyApi { method, args ->
            when (method) {
                "askLLM" -> {
                    val request = args!![0] as LLMRequest
                    LLMResponse(
                        query = request.query,
                        answer = "An assistant answer",
                        responseFingerprint = "opaque-signed-token"
                    )
                }
                "reportAssistantResponse" -> {
                    onReport()
                    val body =
                        """{"code":"invalid_response_fingerprint","detail":"Expired."}"""
                            .toResponseBody("application/json".toMediaType())
                    throw HttpException(
                        Response.error<AssistantResponseReportResponse>(400, body)
                    )
                }
                else -> throw UnsupportedOperationException(method)
            }
        }

    private fun duplicateReportFailureApi(onReport: () -> Unit): KINDDApi =
        proxyApi { method, args ->
            when (method) {
                "askLLM" -> {
                    val request = args!![0] as LLMRequest
                    LLMResponse(
                        query = request.query,
                        answer = "An assistant answer",
                        responseFingerprint = "opaque-signed-token"
                    )
                }
                "reportAssistantResponse" -> {
                    onReport()
                    val body =
                        """{"detail":"This assistant response has already been reported."}"""
                            .toResponseBody("application/json".toMediaType())
                    throw HttpException(
                        Response.error<AssistantResponseReportResponse>(429, body)
                    )
                }
                else -> throw UnsupportedOperationException(method)
            }
        }

    private fun capturingReportApi(requests: MutableList<AssistantResponseReportRequest>): KINDDApi =
        proxyApi { method, args ->
            when (method) {
                "askLLM" -> {
                    val request = args!![0] as LLMRequest
                    LLMResponse(query = request.query, answer = "An assistant answer")
                }
                "reportAssistantResponse" -> {
                    requests += args!![0] as AssistantResponseReportRequest
                    AssistantResponseReportResponse(17, "received")
                }
                else -> throw UnsupportedOperationException(method)
            }
        }

    private fun proxyApi(block: (String, Array<out Any?>?) -> Any?): KINDDApi =
        Proxy.newProxyInstance(
            KINDDApi::class.java.classLoader,
            arrayOf(KINDDApi::class.java)
        ) { _, method, args -> block(method.name, args) } as KINDDApi
}
