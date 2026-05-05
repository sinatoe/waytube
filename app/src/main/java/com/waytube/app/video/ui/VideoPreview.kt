package com.waytube.app.video.ui

import com.waytube.app.video.domain.Video
import com.waytube.app.video.domain.VideoRestriction

sealed interface VideoPreview {
    data class Content(
        val video: Video,
        val play: () -> Unit
    ) : VideoPreview

    data class Unavailable(
        val restriction: VideoRestriction?
    ) : VideoPreview
}
