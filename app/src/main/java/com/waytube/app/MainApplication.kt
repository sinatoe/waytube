package com.waytube.app

import android.app.Application
import com.waytube.app.channel.di.channelModule
import com.waytube.app.navigation.di.navigationModule
import com.waytube.app.network.di.networkModule
import com.waytube.app.playback.di.playbackModule
import com.waytube.app.playlist.di.playlistModule
import com.waytube.app.preferences.di.preferencesModule
import com.waytube.app.search.di.searchModule
import com.waytube.app.video.di.videoModule
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.schabi.newpipe.extractor.NewPipe

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@MainApplication)
            androidLogger()
            modules(
                channelModule,
                navigationModule,
                networkModule,
                playbackModule,
                playlistModule,
                preferencesModule,
                searchModule,
                videoModule
            )
        }

        NewPipe.init(get())
    }
}
