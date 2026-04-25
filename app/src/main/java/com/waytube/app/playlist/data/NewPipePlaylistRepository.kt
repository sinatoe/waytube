package com.waytube.app.playlist.data

import com.waytube.app.common.data.fetch
import com.waytube.app.common.data.paginate
import com.waytube.app.common.data.toVideoItem
import com.waytube.app.common.domain.FetchResult
import com.waytube.app.common.domain.Page
import com.waytube.app.common.domain.VideoItem
import com.waytube.app.playlist.domain.Playlist
import com.waytube.app.playlist.domain.PlaylistRepository
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.playlist.PlaylistInfo

class NewPipePlaylistRepository : PlaylistRepository {
    override suspend fun getPlaylist(id: String): FetchResult<Playlist> =
        fetch {
            PlaylistInfo
                .getInfo(
                    ServiceList.YouTube,
                    ServiceList.YouTube.playlistLHFactory.getUrl(id)
                )
                .toPlaylist()
        }

    override suspend fun getVideoItems(id: String): FetchResult<Page<VideoItem>> =
        ServiceList.YouTube
            .getPlaylistExtractor(id, emptyList(), null)
            .paginate { it.toVideoItem() }
}

private fun PlaylistInfo.toPlaylist(): Playlist = Playlist(
    id = id,
    url = url,
    title = name,
    channelName = uploaderName,
    thumbnailUrl = thumbnails.maxBy { it.height }.url,
    videoCount = streamCount
)
