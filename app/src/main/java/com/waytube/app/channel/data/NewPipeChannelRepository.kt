package com.waytube.app.channel.data

import androidx.paging.PagingData
import com.waytube.app.channel.domain.Channel
import com.waytube.app.channel.domain.ChannelRepository
import com.waytube.app.common.data.NewPipePagingSource
import com.waytube.app.common.data.toVideoItem
import com.waytube.app.common.domain.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeChannelLinkHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfoItem

class NewPipeChannelRepository : ChannelRepository {
    override suspend fun getChannel(id: String): Result<Channel> =
        runCatching {
            try {
                val info = withContext(Dispatchers.IO) {
                    ChannelInfo.getInfo(
                        ServiceList.YouTube,
                        YoutubeChannelLinkHandlerFactory.getInstance().getUrl(id)
                    )
                }

                info.toChannel()
            } catch (_: ContentNotAvailableException) {
                Channel.Unavailable
            }
        }

    override fun getVideoItems(id: String): Flow<PagingData<VideoItem>> =
        NewPipePagingSource.createFlow(
            extractor = ServiceList.YouTube.getChannelTabExtractorFromId(id, ChannelTabs.VIDEOS),
            transform = { item ->
                (item as? StreamInfoItem)?.takeIf { it.viewCount != -1L }?.toVideoItem()
            }
        )
}

private fun ChannelInfo.toChannel() = Channel.Content(
    id = id,
    name = name,
    avatarUrl = avatars.maxBy { it.height }.url,
    bannerUrl = banners.maxByOrNull { it.height }?.url,
    subscriberCount = subscriberCount.takeIf { it != -1L }
)
