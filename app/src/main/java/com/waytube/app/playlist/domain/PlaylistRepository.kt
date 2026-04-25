package com.waytube.app.playlist.domain

import com.waytube.app.common.domain.FetchResult
import com.waytube.app.common.domain.Page
import com.waytube.app.common.domain.VideoItem

interface PlaylistRepository {
    suspend fun getPlaylist(id: String): FetchResult<Playlist>

    suspend fun getVideoItems(id: String): FetchResult<Page<VideoItem>>
}
