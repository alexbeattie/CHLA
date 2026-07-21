package com.chla.kindd.ui.screens

import com.chla.kindd.data.api.KINDDApi
import com.chla.kindd.data.api.LLMRequest
import com.chla.kindd.data.api.LLMResponse
import com.chla.kindd.testing.MainDispatcherRule
import com.chla.kindd.ui.chat.ChatLaunchPrompt
import java.lang.reflect.Proxy
import java.util.concurrent.CancellationException
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
            viewModel.sendInitialPrompt(prompt.routeValue, "reopened ${prompt.routeValue}")
        }
        runCurrent()

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

    private fun api(requests: MutableList<LLMRequest>): KINDDApi = proxyApi { request ->
        requests += request
        LLMResponse(query = request.query, answer = "answer")
    }

    private fun apiFailure(throwable: Throwable): KINDDApi = proxyApi { throw throwable }

    private fun proxyApi(block: (LLMRequest) -> LLMResponse): KINDDApi =
        Proxy.newProxyInstance(
            KINDDApi::class.java.classLoader,
            arrayOf(KINDDApi::class.java)
        ) { _, method, args ->
            if (method.name == "askLLM") block(args!![0] as LLMRequest)
            else throw UnsupportedOperationException(method.name)
        } as KINDDApi
}
