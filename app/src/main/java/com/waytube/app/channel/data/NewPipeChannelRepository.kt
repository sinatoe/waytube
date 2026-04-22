package com.waytube.app.channel.data

import com.waytube.app.channel.domain.Channel
import com.waytube.app.channel.domain.ChannelRepository
import com.waytube.app.common.data.paginate
import com.waytube.app.common.data.toVideoItem
import com.waytube.app.common.domain.Page
import com.waytube.app.common.domain.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.stream.StreamInfoItem

class NewPipeChannelRepository : ChannelRepository {
    override suspend fun getChannel(id: String): Result<Channel> =
        runCatching {
            val info = withContext(Dispatchers.IO) {
                ChannelInfo.getInfo(
                    ServiceList.YouTube,
                    ServiceList.YouTube.channelLHFactory.getUrl(id)
                )
            }

            info.toChannel()
        }

    override suspend fun getVideoItems(id: String): Result<Page<VideoItem>> =
        ServiceList.YouTube
            .getChannelTabExtractorFromId(id, ChannelTabs.VIDEOS)
            .paginate { (it as? StreamInfoItem)?.toVideoItem() }
}

private fun ChannelInfo.toChannel() = Channel(
    id = id,
    url = url,
    name = name,
    avatarUrl = avatars.maxBy { it.height }.url,
    bannerUrl = banners.maxByOrNull { it.height }?.url,
    subscriberCount = subscriberCount.takeIf { it != -1L }
)
