package com.waytube.app.playlist.ui

import com.waytube.app.common.domain.VideoItem
import com.waytube.app.common.ui.PaginatedData
import com.waytube.app.playlist.domain.Playlist

data class PlaylistBundle(
    val playlist: Playlist,
    val videoItems: PaginatedData<VideoItem>
)
