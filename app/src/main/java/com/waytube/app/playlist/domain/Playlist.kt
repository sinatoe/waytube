package com.waytube.app.playlist.domain

sealed interface Playlist {
    data class Content(
        val id: String,
        val url: String,
        val title: String,
        val channelName: String,
        val thumbnailUrl: String,
        val videoCount: Long
    ) : Playlist

    data object Unavailable : Playlist
}
