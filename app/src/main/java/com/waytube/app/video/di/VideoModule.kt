package com.waytube.app.video.di

import com.waytube.app.video.data.NewPipeVideoRepository
import com.waytube.app.video.domain.VideoRepository
import com.waytube.app.video.ui.VideoViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val videoModule = module {
    singleOf(::NewPipeVideoRepository) bind VideoRepository::class
    viewModelOf(::VideoViewModel)
}
