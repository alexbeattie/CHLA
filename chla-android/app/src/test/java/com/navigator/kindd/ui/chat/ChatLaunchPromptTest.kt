package com.navigator.kindd.ui.chat

import com.navigator.kindd.R
import com.navigator.kindd.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatLaunchPromptTest {

    @Test
    fun routeValues_areFixedNonLocalizedKeysOnly() {
        assertEquals(
            listOf(
                "JUST_DIAGNOSED",
                "FIRST_STEPS",
                "WAITING_INTAKE",
                "RECEIVING_SERVICES",
                "FIND_ABA_NEARBY",
                "CENTER_FUNDING",
                "FIND_REGIONAL_CENTER"
            ),
            ChatLaunchPrompt.entries.map(ChatLaunchPrompt::routeValue)
        )
        assertEquals(
            listOf(
                R.string.chat_prompt_just_diagnosed,
                R.string.chat_prompt_first_steps,
                R.string.chat_prompt_waiting_intake,
                R.string.chat_prompt_receiving_services,
                R.string.chat_prompt_find_aba_nearby,
                R.string.chat_prompt_center_funding,
                R.string.chat_prompt_find_regional_center
            ),
            ChatLaunchPrompt.entries.map(ChatLaunchPrompt::promptResId)
        )
        assertNull(ChatLaunchPrompt.fromRouteValue("full private prompt"))
        assertNull(ChatLaunchPrompt.fromRouteValue(null))
    }

    @Test
    fun chatRoutes_containOnlyTheFixedKey() {
        assertEquals("chat", Screen.Chat.route)
        assertEquals("chat?prompt={prompt}", Screen.Chat.destinationRoute)
        assertEquals("chat", Screen.Chat.createRoute(null))

        ChatLaunchPrompt.entries.forEach { prompt ->
            val route = Screen.Chat.createRoute(prompt)
            assertEquals("chat?prompt=${prompt.routeValue}", route)
            assertTrue(route.length < 50)
        }
    }
}
