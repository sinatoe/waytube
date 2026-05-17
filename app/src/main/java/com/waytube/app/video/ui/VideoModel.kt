package com.waytube.app.video.ui

import androidx.media3.common.Player
import com.waytube.app.common.ui.async.AsyncState
import com.waytube.app.video.domain.SkipSegment
import com.waytube.app.video.domain.Video
import com.waytube.app.video.domain.VideoRestriction

sealed interface VideoModel {
    data class Playback(
        val video: Video,
        val player: Player,
        val state: VideoPlaybackState,
        val skipSegmentsState: AsyncState<List<SkipSegment>>?
    ) : VideoModel

    data class Overview(val video: Video) : VideoModel

    data class Unavailable(val restriction: VideoRestriction?) : VideoModel
}
