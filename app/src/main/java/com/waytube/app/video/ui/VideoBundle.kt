package com.waytube.app.video.ui

import com.waytube.app.video.domain.Video
import com.waytube.app.video.domain.VideoRestriction

sealed interface VideoBundle {
    data class Content(
        val video: Video,
        val playbackState: VideoPlaybackState
    ) : VideoBundle

    data class Unavailable(
        val restriction: VideoRestriction?
    ) : VideoBundle
}
