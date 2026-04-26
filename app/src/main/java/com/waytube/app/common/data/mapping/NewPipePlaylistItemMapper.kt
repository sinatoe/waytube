package com.waytube.app.common.data.mapping

import com.waytube.app.common.domain.PlaylistItem
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem

fun PlaylistInfoItem.toPlaylistItem(): PlaylistItem? =
    when (playlistType) {
        PlaylistInfo.PlaylistType.NORMAL -> PlaylistItem(
            id = ServiceList.YouTube.playlistLHFactory.getId(url),
            url = url,
            title = name,
            channelId = uploaderUrl?.let(ServiceList.YouTube.channelLHFactory::getId),
            channelName = uploaderName,
            thumbnailUrl = thumbnails.maxBy { it.height }.url,
            videoCount = streamCount
        )

        else -> null
    }
