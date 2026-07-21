package com.chla.kindd.ui.chat

import com.chla.kindd.R
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ChatPromptContractTest {

    @Test
    fun researchCapsuleDoesNotSendTheInsuranceQuestion() {
        val owner = Class.forName("com.chla.kindd.ui.chat.ChatContentKt")
        val promptsField = owner.getDeclaredField("promptCapsules").apply {
            isAccessible = true
        }
        val prompts = promptsField.get(null) as List<*>
        val researchPrompt = requireNotNull(prompts[1])
        val promptResourceId = researchPrompt.javaClass
            .getDeclaredMethod("getPromptRes")
            .invoke(researchPrompt) as Int

        assertNotEquals(
            "The Research capsule must have a dedicated research question",
            R.string.suggestion_insurance,
            promptResourceId
        )
    }
}
