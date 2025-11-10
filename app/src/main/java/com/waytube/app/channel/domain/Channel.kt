package com.waytube.app.channel.domain

data class Channel(
    val id: String,
    val name: String,
    val avatarUrl: String,
    val bannerUrl: String?,
    val subscriberCount: Long?
)
