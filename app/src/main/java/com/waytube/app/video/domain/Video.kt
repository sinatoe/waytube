package com.waytube.app.video.domain

sealed interface Video {
    sealed interface Content : Video {
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
        ) : Content

        data class Live(
            override val id: String,
            override val title: String,
            override val channelName: String,
            override val thumbnailUrl: String,
            val hlsPlaylistUrl: String
        ) : Content
    }

    data class Unavailable(val reason: Reason?) : Video {
        enum class Reason {
            AGE_RESTRICTED,
            BOT_FLAGGED,
            MEMBERS_ONLY,
            UNSUPPORTED
        }
    }
}
