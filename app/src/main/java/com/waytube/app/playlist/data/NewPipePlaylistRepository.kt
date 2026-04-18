package com.waytube.app.playlist.data

import com.waytube.app.common.data.paginate
import com.waytube.app.common.data.toVideoItem
import com.waytube.app.common.domain.Page
import com.waytube.app.common.domain.VideoItem
import com.waytube.app.playlist.domain.Playlist
import com.waytube.app.playlist.domain.PlaylistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.playlist.PlaylistInfo

class NewPipePlaylistRepository : PlaylistRepository {
    override suspend fun getPlaylist(id: String): Result<Playlist> =
        runCatching {
            try {
                val info = withContext(Dispatchers.IO) {
                    PlaylistInfo.getInfo(
                        ServiceList.YouTube,
                        ServiceList.YouTube.playlistLHFactory.getUrl(id)
                    )
                }

                info.toPlaylist()
            } catch (_: ContentNotAvailableException) {
                Playlist.Unavailable
            }
        }

    override suspend fun getVideoItems(id: String): Result<Page<VideoItem>> =
        ServiceList.YouTube
            .getPlaylistExtractor(id, emptyList(), null)
            .paginate { it.toVideoItem() }
}

private fun PlaylistInfo.toPlaylist(): Playlist = Playlist.Content(
    id = id,
    url = url,
    title = name,
    channelName = uploaderName,
    thumbnailUrl = thumbnails.maxBy { it.height }.url,
    videoCount = streamCount
)
