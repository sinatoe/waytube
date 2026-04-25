package com.waytube.app.channel.ui

import com.waytube.app.channel.domain.Channel
import com.waytube.app.common.domain.VideoItem
import com.waytube.app.common.ui.PaginatedData

data class ChannelBundle(
    val channel: Channel,
    val videoItems: PaginatedData<VideoItem>
)
