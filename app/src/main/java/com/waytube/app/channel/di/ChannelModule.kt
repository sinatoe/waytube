package com.waytube.app.channel.di

import com.waytube.app.channel.data.NewPipeChannelRepository
import com.waytube.app.channel.domain.ChannelRepository
import com.waytube.app.channel.ui.ChannelViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val channelModule = module {
    singleOf(::NewPipeChannelRepository) bind ChannelRepository::class
    viewModelOf(::ChannelViewModel)
}
