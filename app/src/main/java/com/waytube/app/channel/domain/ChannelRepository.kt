package com.waytube.app.channel.domain

import com.waytube.app.common.domain.FetchResult
import com.waytube.app.common.domain.Page
import com.waytube.app.common.domain.VideoItem

interface ChannelRepository {
    suspend fun getChannel(id: String): FetchResult<Channel>

    suspend fun getVideoItems(id: String): FetchResult<Page<VideoItem>>
}
