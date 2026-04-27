package com.waytube.app.channel.domain

import com.waytube.app.common.domain.FetchResult

interface ChannelRepository {
    suspend fun getChannel(id: String): FetchResult<ChannelResponse>
}
