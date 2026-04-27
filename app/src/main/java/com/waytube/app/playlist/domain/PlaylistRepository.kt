package com.waytube.app.playlist.domain

import com.waytube.app.common.domain.FetchResult

interface PlaylistRepository {
    suspend fun getPlaylist(id: String): FetchResult<PlaylistResponse>
}
