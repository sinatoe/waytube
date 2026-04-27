package com.waytube.app.playlist.ui

import com.waytube.app.common.domain.VideoItem
import com.waytube.app.common.ui.pagination.PaginatedData
import com.waytube.app.playlist.domain.Playlist

sealed interface PlaylistBundle {
    data class Content(
        val playlist: Playlist,
        val videoItems: PaginatedData<VideoItem>
    ) : PlaylistBundle

    data object Unavailable : PlaylistBundle
}
