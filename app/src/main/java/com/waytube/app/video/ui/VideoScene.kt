package com.waytube.app.video.ui

import androidx.media3.common.Player
import com.waytube.app.common.ui.async.AsyncState
import com.waytube.app.video.domain.Video

sealed interface VideoScene {
    data class Preview(
        val state: AsyncState<VideoPreview>
    ) : VideoScene

    data class Playback(
        val video: Video,
        val player: Player,
        val stop: () -> Unit
    ) : VideoScene
}
