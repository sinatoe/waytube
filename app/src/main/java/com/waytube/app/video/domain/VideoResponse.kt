package com.waytube.app.video.domain

sealed interface VideoResponse {
    data class Content(val video: Video) : VideoResponse

    data class Unavailable(val restriction: VideoRestriction?) : VideoResponse
}
