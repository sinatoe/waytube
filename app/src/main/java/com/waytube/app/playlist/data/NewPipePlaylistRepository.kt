package com.waytube.app.playlist.data

import androidx.paging.PagingData
import com.waytube.app.common.data.NewPipePagingSource
import com.waytube.app.common.data.toVideoItem
import com.waytube.app.common.domain.VideoItem
import com.waytube.app.playlist.domain.Playlist
import com.waytube.app.playlist.domain.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.playlist.PlaylistInfo
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubePlaylistLinkHandlerFactory

class NewPipePlaylistRepository : PlaylistRepository {
    override suspend fun getPlaylist(id: String): Result<Playlist> =
        runCatching {
            try {
                val info = withContext(Dispatchers.IO) {
                    PlaylistInfo.getInfo(
                        ServiceList.YouTube,
                        YoutubePlaylistLinkHandlerFactory.getInstance().getUrl(id)
                    )
                }

                info.toPlaylist()
            } catch (_: ContentNotAvailableException) {
                Playlist.Unavailable
            }
        }

    override fun getVideoItems(id: String): Flow<PagingData<VideoItem>> =
        NewPipePagingSource.createFlow(
            extractor = ServiceList.YouTube.getPlaylistExtractor(id, emptyList(), null),
            transform = { it.toVideoItem() }
        )
}

private fun PlaylistInfo.toPlaylist(): Playlist = Playlist.Content(
    id = id,
    title = name,
    channelName = uploaderName,
    thumbnailUrl = thumbnails.maxBy { it.height }.url,
    videoCount = streamCount
)
