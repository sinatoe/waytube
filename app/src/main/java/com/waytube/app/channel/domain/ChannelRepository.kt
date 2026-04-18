package com.waytube.app.channel.domain

import com.waytube.app.common.domain.Page
import com.waytube.app.common.domain.VideoItem

interface ChannelRepository {
    suspend fun getChannel(id: String): Result<Channel>

    suspend fun getVideoItems(id: String): Result<Page<VideoItem>>
}
