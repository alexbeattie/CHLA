package com.chla.kindd.ui.chat

import com.chla.kindd.R
import com.chla.kindd.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatLaunchPromptTest {

    @Test
    fun routeValues_areTheThreeFixedKeysOnly() {
        assertEquals(
            listOf("JUST_DIAGNOSED", "WAITING_INTAKE", "RECEIVING_SERVICES"),
            ChatLaunchPrompt.entries.map(ChatLaunchPrompt::routeValue)
        )
        assertEquals(
            listOf(
                R.string.chat_prompt_just_diagnosed,
                R.string.chat_prompt_waiting_intake,
                R.string.chat_prompt_receiving_services
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
