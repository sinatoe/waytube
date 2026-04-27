package com.waytube.app.channel.data

import com.waytube.app.channel.domain.Channel
import com.waytube.app.channel.domain.ChannelRepository
import com.waytube.app.channel.domain.ChannelResponse
import com.waytube.app.common.data.fetch
import com.waytube.app.common.data.mapping.toVideoItem
import com.waytube.app.common.data.paginate
import com.waytube.app.common.domain.FetchResult
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfo
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.stream.StreamInfoItem

class NewPipeChannelRepository : ChannelRepository {
    override suspend fun getChannel(id: String): FetchResult<ChannelResponse> =
        fetch {
            try {
                val channel = ChannelInfo
                    .getInfo(
                        ServiceList.YouTube,
                        ServiceList.YouTube.channelLHFactory.getUrl(id)
                    )
                    .toChannel()

                val videoItemsPage = ServiceList.YouTube
                    .getChannelTabExtractorFromId(channel.id, ChannelTabs.VIDEOS)
                    .paginate { (it as? StreamInfoItem)?.toVideoItem() }

                ChannelResponse.Content(
                    channel = channel,
                    videoItemsPage = videoItemsPage
                )
            } catch (_: ContentNotAvailableException) {
                ChannelResponse.Unavailable
            }
        }
}

private fun ChannelInfo.toChannel() = Channel(
    id = id,
    url = url,
    name = name,
    avatarUrl = avatars.maxBy { it.height }.url,
    bannerUrl = banners.maxByOrNull { it.height }?.url,
    subscriberCount = subscriberCount.takeIf { it != -1L }
)
