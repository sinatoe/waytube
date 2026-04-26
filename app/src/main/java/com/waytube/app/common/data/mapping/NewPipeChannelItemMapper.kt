package com.waytube.app.common.data.mapping

import com.waytube.app.common.domain.ChannelItem
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfoItem

fun ChannelInfoItem.toChannelItem(): ChannelItem =
    ChannelItem(
        id = ServiceList.YouTube.channelLHFactory.getId(url),
        url = url,
        name = name,
        avatarUrl = thumbnails.maxBy { it.height }.url,
        subscriberCount = subscriberCount.takeIf { it != -1L }
    )
