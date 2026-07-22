package com.navigator.kindd.ui.screens

import com.navigator.kindd.data.api.KINDDApi
import com.navigator.kindd.data.api.LLMRequest
import com.navigator.kindd.data.api.LLMResponse
import com.navigator.kindd.testing.MainDispatcherRule
import com.navigator.kindd.ui.chat.ChatLaunchPrompt
import java.lang.reflect.Proxy
import java.util.concurrent.CancellationException
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.resume
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelInitialPromptTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun eachFixedKey_isAcceptedForEachSheetPresentation() = runTest(mainDispatcherRule.testDispatcher) {
        val requests = mutableListOf<LLMRequest>()
        val viewModel = ChatViewModel(api(requests), mainDispatcherRule.testDispatcher)

        ChatLaunchPrompt.entries.forEach { prompt ->
            viewModel.sendInitialPrompt(prompt.routeValue, "localized ${prompt.routeValue}")
            runCurrent()
            viewModel.sendInitialPrompt(prompt.routeValue, "reopened ${prompt.routeValue}")
            runCurrent()
        }

        assertEquals(ChatLaunchPrompt.entries.size * 2, requests.size)
        assertEquals(
            ChatLaunchPrompt.entries.flatMap {
                listOf("localized ${it.routeValue}", "reopened ${it.routeValue}")
            },
            requests.map(LLMRequest::query)
        )
    }

    @Test
    fun absentOrUnknownRouteKey_sendsNoAutomaticPrompt() =
        runTest(mainDispatcherRule.testDispatcher) {
            val requests = mutableListOf<LLMRequest>()
            val viewModel = ChatViewModel(api(requests), mainDispatcherRule.testDispatcher)

            viewModel.sendInitialPrompt("", "should not send")
            viewModel.sendInitialPrompt("not-a-fixed-key", "should not send")
            runCurrent()

            assertEquals(emptyList<LLMRequest>(), requests)
            assertEquals(emptyList<Any>(), viewModel.uiState.value.messages)
        }

    @Test
    fun failuresExposeOnlySanitizedCategory_andCancellationIsRethrown() =
        runTest(mainDispatcherRule.testDispatcher) {
            val failure = ChatViewModel(apiFailure(IllegalStateException("private backend text")), mainDispatcherRule.testDispatcher)
            failure.sendMessage("private question")
            runCurrent()

            assertEquals(ChatFailure.REQUEST_FAILED, failure.uiState.value.error)
            assertTrue(failure.uiState.value.messages.none { it.content.contains("private backend text") })

            val cancelled = ChatViewModel(apiFailure(CancellationException("cancel")), mainDispatcherRule.testDispatcher)
            cancelled.sendMessage("question")
            runCurrent()
            assertNull(cancelled.uiState.value.error)
        }

    @Test
    fun sendMessage_ignoresAdditionalQuestionsWhileARequestIsInFlight() =
        runTest(mainDispatcherRule.testDispatcher) {
            val requests = mutableListOf<LLMRequest>()
            val viewModel = ChatViewModel(api(requests), mainDispatcherRule.testDispatcher)

            viewModel.sendMessage("first question")
            viewModel.sendMessage("second question")
            runCurrent()

            assertEquals(listOf("first question"), requests.map(LLMRequest::query))
            assertEquals(
                listOf("first question", "answer"),
                viewModel.uiState.value.messages.map { it.content }
            )
        }

    @Test
    fun sendInitialPrompt_acknowledgesOnlyPromptsAcceptedForDispatch() =
        runTest(mainDispatcherRule.testDispatcher) {
            val requests = mutableListOf<LLMRequest>()
            val pendingReplies = mutableListOf<Continuation<LLMResponse>>()
            val viewModel = ChatViewModel(
                pendingApi(requests, pendingReplies),
                mainDispatcherRule.testDispatcher
            )

            viewModel.sendMessage("request already in flight")
            runCurrent()

            assertEquals(
                false,
                viewModel.sendInitialPrompt(
                    ChatLaunchPrompt.JUST_DIAGNOSED.routeValue,
                    "quick prompt"
                )
            )

            pendingReplies.single().resume(
                LLMResponse(query = "request already in flight", answer = "first answer")
            )
            runCurrent()

            assertEquals(
                true,
                viewModel.sendInitialPrompt(
                    ChatLaunchPrompt.JUST_DIAGNOSED.routeValue,
                    "quick prompt"
                )
            )
            runCurrent()

            assertEquals(
                listOf("request already in flight", "quick prompt"),
                requests.map(LLMRequest::query)
            )
        }

    @Test
    fun clearChat_resetsEveryStateAndPreventsThePendingReplyFromReappearing() =
        runTest(mainDispatcherRule.testDispatcher) {
            val requests = mutableListOf<LLMRequest>()
            val pendingReplies = mutableListOf<Continuation<LLMResponse>>()
            val viewModel = ChatViewModel(
                pendingApi(requests, pendingReplies),
                mainDispatcherRule.testDispatcher
            )

            viewModel.sendMessage("question to remove")
            runCurrent()
            assertTrue(viewModel.uiState.value.isLoading)
            assertEquals(listOf("question to remove"), requests.map(LLMRequest::query))

            viewModel.clearChat()

            assertEquals(ChatUiState(), viewModel.uiState.value)
            pendingReplies.single().resume(
                LLMResponse(query = "question to remove", answer = "late answer")
            )
            runCurrent()
            assertEquals(ChatUiState(), viewModel.uiState.value)
        }

    @Test
    fun retryLastMessage_reusesThePreservedQuestionWithoutAddingAnotherUserBubble() =
        runTest(mainDispatcherRule.testDispatcher) {
            val requests = mutableListOf<LLMRequest>()
            val viewModel = ChatViewModel(
                proxyApi { request ->
                    requests += request
                    throw IllegalStateException("private backend text")
                },
                mainDispatcherRule.testDispatcher
            )
            viewModel.sendMessage("question worth preserving")
            runCurrent()

            viewModel.retryLastMessage()
            runCurrent()

            assertEquals(
                listOf("question worth preserving", "question worth preserving"),
                requests.map(LLMRequest::query)
            )
            assertEquals(
                listOf("question worth preserving"),
                viewModel.uiState.value.messages.map { it.content }
            )
            assertEquals(ChatFailure.REQUEST_FAILED, viewModel.uiState.value.error)
        }

    private fun api(requests: MutableList<LLMRequest>): KINDDApi = proxyApi { request ->
        requests += request
        LLMResponse(query = request.query, answer = "answer")
    }

    private fun apiFailure(throwable: Throwable): KINDDApi = proxyApi { throw throwable }

    private fun pendingApi(
        requests: MutableList<LLMRequest>,
        pendingReplies: MutableList<Continuation<LLMResponse>>
    ): KINDDApi =
        Proxy.newProxyInstance(
            KINDDApi::class.java.classLoader,
            arrayOf(KINDDApi::class.java)
        ) { _, method, args ->
            if (method.name == "askLLM") {
                requests += args!![0] as LLMRequest
                @Suppress("UNCHECKED_CAST")
                pendingReplies += args[1] as Continuation<LLMResponse>
                COROUTINE_SUSPENDED
            } else {
                throw UnsupportedOperationException(method.name)
            }
        } as KINDDApi

    private fun proxyApi(block: (LLMRequest) -> LLMResponse): KINDDApi =
        Proxy.newProxyInstance(
            KINDDApi::class.java.classLoader,
            arrayOf(KINDDApi::class.java)
        ) { _, method, args ->
            if (method.name == "askLLM") block(args!![0] as LLMRequest)
            else throw UnsupportedOperationException(method.name)
        } as KINDDApi
}
