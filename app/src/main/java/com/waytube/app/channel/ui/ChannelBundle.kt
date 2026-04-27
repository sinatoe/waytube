package com.waytube.app.channel.ui

import com.waytube.app.channel.domain.Channel
import com.waytube.app.common.domain.VideoItem
import com.waytube.app.common.ui.pagination.PaginatedData

sealed interface ChannelBundle {
    data class Content(
        val channel: Channel,
        val videoItems: PaginatedData<VideoItem>
    ) : ChannelBundle

    data object Unavailable : ChannelBundle
}
