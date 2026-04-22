package com.waytube.app.playlist.domain

data class Playlist(
    val id: String,
    val url: String,
    val title: String,
    val channelName: String,
    val thumbnailUrl: String,
    val videoCount: Long
)
