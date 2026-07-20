package com.chla.kindd.ui.chat

import androidx.annotation.StringRes
import com.chla.kindd.R

enum class ChatLaunchPrompt(
    val routeValue: String,
    @StringRes val promptResId: Int
) {
    JUST_DIAGNOSED("JUST_DIAGNOSED", R.string.chat_prompt_just_diagnosed),
    WAITING_INTAKE("WAITING_INTAKE", R.string.chat_prompt_waiting_intake),
    RECEIVING_SERVICES("RECEIVING_SERVICES", R.string.chat_prompt_receiving_services);

    companion object {
        fun fromRouteValue(value: String?): ChatLaunchPrompt? =
            entries.firstOrNull { prompt -> prompt.routeValue == value }
    }
}
