package com.waytube.app.video.ui

import androidx.media3.common.Player
import com.waytube.app.common.ui.async.AsyncState
import com.waytube.app.video.domain.SkipSegment
import com.waytube.app.video.domain.Video
import com.waytube.app.video.domain.VideoRestriction

sealed interface VideoModel {
    data class Playback(
        val video: Video,
        val session: VideoPlaybackSession,
        val skipSegmentsState: AsyncState<List<SkipSegment>>?
    ) : VideoModel

    data class Overview(val video: Video) : VideoModel

    data class Unavailable(val restriction: VideoRestriction?) : VideoModel
}

data class VideoPlaybackSession(
    val player: Player,
    val status: Status
) {
    enum class Status {
        BUFFERING,
        ERROR,
        PLAYING,
        PAUSED
    }
}
