package eu.vendeli.tgbot.types

import eu.vendeli.tgbot.interfaces.Button

data class KeyboardButton(
    val text: String,
    val requestContact: Boolean? = null,
    val requestLocation: Boolean? = null,
    val requestPoll: KeyboardButtonPollType? = null,
    val webApp: WebAppInfo? = null,
) : Button
