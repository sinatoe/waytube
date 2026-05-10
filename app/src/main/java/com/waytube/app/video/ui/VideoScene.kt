package com.waytube.app.video.ui

import androidx.media3.common.Player
import com.waytube.app.common.ui.async.AsyncState
import com.waytube.app.video.domain.Video
import com.waytube.app.video.domain.VideoResponse

sealed interface VideoScene {
    data class Preview(
        val responseState: AsyncState<VideoResponse>
    ) : VideoScene

    data class Playback(
        val video: Video,
        val player: Player
    ) : VideoScene
}
