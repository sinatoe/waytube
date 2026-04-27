package com.waytube.app.playlist.data

import com.waytube.app.common.data.fetch
import com.waytube.app.common.data.mapping.toVideoItem
import com.waytube.app.common.data.paginate
import com.waytube.app.common.domain.FetchResult
import com.waytube.app.playlist.domain.Playlist
import com.waytube.app.playlist.domain.PlaylistRepository
import com.waytube.app.playlist.domain.PlaylistResponse
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.playlist.PlaylistInfo

class NewPipePlaylistRepository : PlaylistRepository {
    override suspend fun getPlaylist(id: String): FetchResult<PlaylistResponse> =
        fetch {
            try {
                val playlist = PlaylistInfo
                    .getInfo(
                        ServiceList.YouTube,
                        ServiceList.YouTube.playlistLHFactory.getUrl(id)
                    )
                    .toPlaylist()

                val videoItemsPage = ServiceList.YouTube
                    .getPlaylistExtractor(id, emptyList(), null)
                    .paginate { it.toVideoItem() }

                PlaylistResponse.Content(
                    playlist = playlist,
                    videoItemsPage = videoItemsPage
                )
            } catch (_: ContentNotAvailableException) {
                PlaylistResponse.Unavailable
            }
        }
}

private fun PlaylistInfo.toPlaylist(): Playlist = Playlist(
    id = id,
    url = url,
    title = name,
    channelName = uploaderName,
    thumbnailUrl = thumbnails.maxBy { it.height }.url,
    videoCount = streamCount
)
