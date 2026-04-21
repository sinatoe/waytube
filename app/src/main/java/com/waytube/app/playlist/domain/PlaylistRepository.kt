package com.waytube.app.playlist.domain

import com.waytube.app.common.domain.Page
import com.waytube.app.common.domain.VideoItem

interface PlaylistRepository {
    suspend fun getPlaylist(id: String): Result<Playlist>

    suspend fun getVideoItems(id: String): Result<Page<VideoItem>>
}
