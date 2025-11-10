package com.waytube.app.channel.domain

import androidx.paging.PagingData
import com.waytube.app.common.domain.VideoItem
import kotlinx.coroutines.flow.Flow

interface ChannelRepository {
    suspend fun getChannel(id: String): Result<Channel>

    fun getVideoItems(id: String): Flow<PagingData<VideoItem>>
}
