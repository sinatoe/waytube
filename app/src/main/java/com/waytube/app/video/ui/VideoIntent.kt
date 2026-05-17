package com.waytube.app.video.ui

sealed interface VideoIntent {
    data object Refresh : VideoIntent

    data object StartPlayback : VideoIntent

    data object StopPlayback : VideoIntent
}
