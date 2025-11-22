package com.waytube.app.video.di

import android.content.ComponentName
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.waytube.app.video.data.CoilBitmapLoader
import com.waytube.app.video.data.NewPipeVideoRepository
import com.waytube.app.video.domain.VideoRepository
import com.waytube.app.video.service.VideoSessionService
import com.waytube.app.video.ui.VideoViewModel
import kotlinx.coroutines.CoroutineScope
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val videoModule = module {
    singleOf(::NewPipeVideoRepository) bind VideoRepository::class
    factory<Player> @OptIn(UnstableApi::class) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                15_000,
                30_000,
                5_000,
                5_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        ExoPlayer.Builder(androidContext())
            .setAudioAttributes(audioAttributes, true)
            .setLoadControl(loadControl)
            .setHandleAudioBecomingNoisy(true)
            .setUsePlatformDiagnostics(false)
            .build()
    }
    factory<BitmapLoader> { (scope: CoroutineScope) ->
        CoilBitmapLoader(androidContext(), scope)
    }
    factory {
        MediaController.Builder(
            androidContext(),
            SessionToken(
                androidContext(),
                ComponentName(androidContext(), VideoSessionService::class.java)
            )
        ).buildAsync()
    }
    viewModelOf(::VideoViewModel)
}
