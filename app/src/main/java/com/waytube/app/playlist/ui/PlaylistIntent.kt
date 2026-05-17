package com.waytube.app.playlist.ui

sealed interface PlaylistIntent {
    data object Refresh : PlaylistIntent

    data object LoadVideoItems : PlaylistIntent
}
