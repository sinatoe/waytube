package com.waytube.app

import android.app.Application
import com.waytube.app.channel.di.channelModule
import com.waytube.app.network.di.networkModule
import com.waytube.app.search.di.searchModule
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
                networkModule,
                searchModule
            )
        }

        NewPipe.init(get())
    }
}
