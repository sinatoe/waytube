package com.waytube.app.common.data.mapping

import com.waytube.app.common.domain.VideoItem
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.ContentAvailability
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toKotlinInstant

private val StreamInfoItem.id: String
    get() = ServiceList.YouTube.streamLHFactory.getId(url)

private val StreamInfoItem.channelId: String?
    get() = uploaderUrl?.let(ServiceList.YouTube.channelLHFactory::getId)

private val StreamInfoItem.thumbnailUrl: String
    get() = thumbnails.maxBy { it.height }.url

fun StreamInfoItem.toVideoItem(): VideoItem? =
    when {
        contentAvailability != ContentAvailability.AVAILABLE -> null

        streamType == StreamType.VIDEO_STREAM -> VideoItem.Regular(
            id = id,
            url = url,
            title = name,
            channelId = channelId,
            channelName = uploaderName,
            thumbnailUrl = thumbnailUrl,
            duration = duration.seconds,
            viewCount = viewCount,
            uploadedAt = uploadDate?.instant?.toKotlinInstant()
        )

        streamType == StreamType.LIVE_STREAM -> VideoItem.Live(
            id = id,
            url = url,
            title = name,
            channelId = channelId,
            channelName = uploaderName,
            thumbnailUrl = thumbnailUrl,
            watchingCount = viewCount
        )

        else -> null
    }
