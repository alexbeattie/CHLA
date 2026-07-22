package com.navigator.kindd.ui.chat

import androidx.annotation.StringRes
import com.navigator.kindd.R

enum class ChatLaunchPrompt(
    val routeValue: String,
    @StringRes val promptResId: Int
) {
    JUST_DIAGNOSED("JUST_DIAGNOSED", R.string.chat_prompt_just_diagnosed),
    FIRST_STEPS("FIRST_STEPS", R.string.chat_prompt_first_steps),
    WAITING_INTAKE("WAITING_INTAKE", R.string.chat_prompt_waiting_intake),
    RECEIVING_SERVICES("RECEIVING_SERVICES", R.string.chat_prompt_receiving_services),
    FIND_ABA_NEARBY("FIND_ABA_NEARBY", R.string.chat_prompt_find_aba_nearby),
    CENTER_FUNDING("CENTER_FUNDING", R.string.chat_prompt_center_funding),
    FIND_REGIONAL_CENTER("FIND_REGIONAL_CENTER", R.string.chat_prompt_find_regional_center);

    companion object {
        fun fromRouteValue(value: String?): ChatLaunchPrompt? =
            entries.firstOrNull { prompt -> prompt.routeValue == value }
    }
}
