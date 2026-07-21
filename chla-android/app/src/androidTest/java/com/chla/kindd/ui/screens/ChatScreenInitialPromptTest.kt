package com.chla.kindd.ui.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.chla.kindd.data.api.KINDDApi
import com.chla.kindd.data.api.LLMRequest
import com.chla.kindd.data.api.LLMResponse
import com.chla.kindd.ui.chat.ChatLaunchPrompt
import com.chla.kindd.ui.theme.KINDDTheme
import java.lang.reflect.Proxy
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ChatScreenInitialPromptTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun identicalTypedPrompt_dispatchesOncePerSheetPresentation() {
        val requests = mutableListOf<LLMRequest>()
        val viewModel = ChatViewModel(api(requests), Dispatchers.Unconfined)
        var sheetVisible by mutableStateOf(true)
        var unrelatedRecomposition by mutableIntStateOf(0)

        composeRule.setContent {
            KINDDTheme {
                Text(unrelatedRecomposition.toString())
                if (sheetVisible) {
                    ChatScreen(
                        initialPrompt = ChatLaunchPrompt.JUST_DIAGNOSED,
                        viewModel = viewModel
                    )
                }
            }
        }

        composeRule.waitUntil { requests.size == 1 }
        composeRule.runOnIdle { unrelatedRecomposition += 1 }
        composeRule.waitForIdle()
        assertEquals("recomposition must not redispatch", 1, requests.size)

        composeRule.runOnIdle { sheetVisible = false }
        composeRule.waitForIdle()
        composeRule.runOnIdle { sheetVisible = true }
        composeRule.waitUntil { requests.size == 2 }

        assertEquals(
            listOf(
                ChatLaunchPrompt.JUST_DIAGNOSED,
                ChatLaunchPrompt.JUST_DIAGNOSED
            ).map { prompt ->
                InstrumentationRegistry.getInstrumentation().targetContext
                    .getString(prompt.promptResId)
            },
            requests.map(LLMRequest::query)
        )
    }

    private fun api(requests: MutableList<LLMRequest>): KINDDApi =
        Proxy.newProxyInstance(
            KINDDApi::class.java.classLoader,
            arrayOf(KINDDApi::class.java)
        ) { _, method, args ->
            if (method.name == "askLLM") {
                val request = args!![0] as LLMRequest
                requests += request
                LLMResponse(query = request.query, answer = "answer")
            } else {
                throw UnsupportedOperationException(method.name)
            }
        } as KINDDApi
}
