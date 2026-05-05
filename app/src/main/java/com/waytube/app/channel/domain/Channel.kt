package com.waytube.app.channel.domain

data class Channel(
    val id: String,
    val url: String,
    val name: String,
    val avatarUrl: String,
    val subscriberCount: Long?
)
