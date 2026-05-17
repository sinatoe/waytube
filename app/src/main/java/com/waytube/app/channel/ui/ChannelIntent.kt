package com.waytube.app.channel.ui

sealed interface ChannelIntent {
    data object Refresh : ChannelIntent

    data object LoadVideoItems : ChannelIntent
}
