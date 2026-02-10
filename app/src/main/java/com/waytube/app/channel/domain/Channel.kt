package com.waytube.app.channel.domain

sealed interface Channel {
    data class Content(
        val id: String,
        val url: String,
        val name: String,
        val avatarUrl: String,
        val bannerUrl: String?,
        val subscriberCount: Long?
    ) : Channel

    data object Unavailable : Channel
}
