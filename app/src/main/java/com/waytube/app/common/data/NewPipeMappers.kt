package com.waytube.app.common.data

import com.waytube.app.common.domain.ChannelItem
import com.waytube.app.common.domain.Identifiable
import com.waytube.app.common.domain.Page
import com.waytube.app.common.domain.PlaylistItem
import com.waytube.app.common.domain.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.stream.ContentAvailability
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toKotlinInstant

suspend fun <T : InfoItem, R : Identifiable> ListExtractor<T>.paginate(
    treatErrorAsEmpty: (Throwable) -> Boolean = { false },
    transform: (T) -> R?
): Result<Page<R>> {
    val deduplicationSet = ConcurrentHashMap.newKeySet<String>()

    suspend fun load(source: suspend () -> ListExtractor.InfoItemsPage<T>): Result<Page<R>> =
        withContext(Dispatchers.IO) {
            runCatching {
                try {
                    val extractorPage = source()

                    Page(
                        items = extractorPage.items
                            .mapNotNull(transform)
                            .filter { deduplicationSet.add(it.id) },
                        next = extractorPage.nextPage?.let {
                            { load { getPage(it) } }
                        }
                    )
                } catch (e: Throwable) {
                    if (treatErrorAsEmpty(e)) {
                        Page(
                            items = emptyList(),
                            next = null
                        )
                    } else throw e
                }
            }
        }

    return load {
        fetchPage()
        initialPage
    }
}

fun StreamInfoItem.toVideoItem(): VideoItem? {
    if (contentAvailability != ContentAvailability.AVAILABLE) {
        return null
    }

    val id = ServiceList.YouTube.streamLHFactory.getId(url)
    val channelId = uploaderUrl?.let { url ->
        ServiceList.YouTube.channelLHFactory.getId(url)
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
            uploadedAt = uploadDate?.instant?.toKotlinInstant()
        )

        StreamType.LIVE_STREAM -> VideoItem.Live(
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
}

fun ChannelInfoItem.toChannelItem(): ChannelItem = ChannelItem(
    id = ServiceList.YouTube.channelLHFactory.getId(url),
    url = url,
    name = name,
    avatarUrl = thumbnails.maxBy { it.height }.url,
    subscriberCount = subscriberCount.takeIf { it != -1L }
)

fun PlaylistInfoItem.toPlaylistItem(): PlaylistItem? =
    if (playlistType == PlaylistInfo.PlaylistType.NORMAL) {
        PlaylistItem(
            id = ServiceList.YouTube.playlistLHFactory.getId(url),
            url = url,
            title = name,
            channelId = uploaderUrl?.let { url ->
                ServiceList.YouTube.channelLHFactory.getId(url)
            },
            channelName = uploaderName,
            thumbnailUrl = thumbnails.maxBy { it.height }.url,
            videoCount = streamCount
        )
    } else null
