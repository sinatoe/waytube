package com.waytube.app.playlist.domain

import com.waytube.app.common.domain.Page
import com.waytube.app.common.domain.VideoItem

sealed interface PlaylistResponse {
    data class Content(
        val playlist: Playlist,
        val videoItemsPage: Page<VideoItem>
    ) : PlaylistResponse

    data object Unavailable : PlaylistResponse
}
