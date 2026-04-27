package com.waytube.app.channel.domain

import com.waytube.app.common.domain.Page
import com.waytube.app.common.domain.VideoItem

sealed interface ChannelResponse {
    data class Content(
        val channel: Channel,
        val videoItemsPage: Page<VideoItem>
    ) : ChannelResponse

    data object Unavailable : ChannelResponse
}
