package com.waytube.app.common.data

import com.waytube.app.common.domain.ChannelItem
import com.waytube.app.common.domain.PlaylistItem
import com.waytube.app.common.domain.VideoItem
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeChannelLinkHandlerFactory
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubePlaylistLinkHandlerFactory
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeStreamLinkHandlerFactory
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toKotlinInstant

fun StreamInfoItem.toVideoItem(): VideoItem? {
    val id = YoutubeStreamLinkHandlerFactory.getInstance().getId(url)
    val channelId = uploaderUrl?.let { url ->
        YoutubeChannelLinkHandlerFactory.getInstance().getId(url)
    }
    val thumbnailUrl = thumbnails.maxBy { it.height }.url

    return when (streamType) {
        StreamType.VIDEO_STREAM -> VideoItem.Regular(
            id = id,
            url = url,
            title = name,
            channelId = channelId,
            channelName = uploaderName,
            thumbnailUrl = thumbnailUrl,
            duration = duration.seconds,
            viewCount = viewCount,
            uploadedAt = uploadDate?.offsetDateTime()?.toInstant()?.toKotlinInstant()
        )

        StreamType.LIVE_STREAM -> VideoItem.Live(
            id = id,
            url = url,
            title = name,
            channelId = channelId,
            channelName = uploaderName,
            thumbnailUrl = thumbnailUrl,
            viewerCount = viewCount
        )

        else -> null
    }
}

fun ChannelInfoItem.toChannelItem(): ChannelItem = ChannelItem(
    id = YoutubeChannelLinkHandlerFactory.getInstance().getId(url),
    url = url,
    name = name,
    avatarUrl = thumbnails.maxBy { it.height }.url,
    subscriberCount = subscriberCount.takeIf { it != -1L }
)

fun PlaylistInfoItem.toPlaylistItem(): PlaylistItem? =
    if (playlistType == PlaylistInfo.PlaylistType.NORMAL) {
        PlaylistItem(
            id = YoutubePlaylistLinkHandlerFactory.getInstance().getId(url),
            url = url,
            title = name,
            channelId = uploaderUrl?.let { url ->
                YoutubeChannelLinkHandlerFactory.getInstance().getId(url)
            },
            channelName = uploaderName,
            thumbnailUrl = thumbnails.maxBy { it.height }.url,
            videoCount = streamCount
        )
    } else null
