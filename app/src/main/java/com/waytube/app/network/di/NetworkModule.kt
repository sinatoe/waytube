package com.waytube.app.network.di

import com.waytube.app.network.data.NewPipeDownloader
import okhttp3.OkHttpClient
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.schabi.newpipe.extractor.downloader.Downloader

val networkModule = module {
    singleOf(::OkHttpClient)
    singleOf(::NewPipeDownloader) bind Downloader::class
}
