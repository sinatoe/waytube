package com.waytube.app.video.ui

import androidx.media3.common.Player
import com.waytube.app.video.domain.Video
import com.waytube.app.video.domain.VideoRestriction

sealed interface VideoBundle {
    data class Playback(
        val video: Video,
        val player: Player,
        val state: VideoPlaybackState
    ) : VideoBundle

    data class Overview(val video: Video) : VideoBundle

    data class Unavailable(val restriction: VideoRestriction?) : VideoBundle
}
