package com.waytube.app.video.ui

import androidx.media3.common.Player
import com.waytube.app.common.ui.async.AsyncState
import com.waytube.app.video.domain.SkipSegment
import com.waytube.app.video.domain.Video
import com.waytube.app.video.domain.VideoRestriction

sealed interface VideoBundle {
    data class Playback(
        val video: Video,
        val player: Player,
        val state: VideoPlaybackState,
        val skipSegmentsState: AsyncState<List<SkipSegment>>?
    ) : VideoBundle

    data class Overview(val video: Video) : VideoBundle

    data class Unavailable(val restriction: VideoRestriction?) : VideoBundle
}
