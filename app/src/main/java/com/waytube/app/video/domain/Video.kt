package com.waytube.app.video.domain

sealed interface Video {
    val id: String
    val title: String
    val channelName: String
    val thumbnailUrl: String

    data class Regular(
        override val id: String,
        override val title: String,
        override val channelName: String,
        override val thumbnailUrl: String,
        val dashManifestUrl: String
    ) : Video

    data class Live(
        override val id: String,
        override val title: String,
        override val channelName: String,
        override val thumbnailUrl: String,
        val hlsPlaylistUrl: String
    ) : Video
}
