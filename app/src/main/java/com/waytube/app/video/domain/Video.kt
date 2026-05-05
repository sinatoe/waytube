package com.waytube.app.video.domain

import kotlin.time.Instant

sealed interface Video {
    val id: String
    val url: String
    val title: String
    val thumbnailUrl: String
    val descriptionHtml: String
    val approvalRatio: Float?
    val channelId: String
    val channelName: String

    data class Regular(
        override val id: String,
        override val url: String,
        override val title: String,
        override val thumbnailUrl: String,
        override val descriptionHtml: String,
        override val approvalRatio: Float?,
        override val channelId: String,
        override val channelName: String,
        val dashManifestUrl: String,
        val viewCount: Long,
        val uploadedAt: Instant
    ) : Video

    data class Live(
        override val id: String,
        override val url: String,
        override val title: String,
        override val thumbnailUrl: String,
        override val descriptionHtml: String,
        override val approvalRatio: Float?,
        override val channelId: String,
        override val channelName: String,
        val hlsPlaylistUrl: String,
        val watchingCount: Long
    ) : Video
}
