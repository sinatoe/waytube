package com.waytube.app.video.ui

import androidx.media3.common.Player

sealed interface VideoPlaybackState {
    data class Idle(
        val play: () -> Unit
    ) : VideoPlaybackState

    data class Active(
        val player: Player,
        val stop: () -> Unit
    ) : VideoPlaybackState
}
