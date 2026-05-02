package com.waytube.app.playback.di

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
import com.waytube.app.playback.data.CoilBitmapLoader
import com.waytube.app.playback.service.PlaybackService
import com.waytube.app.playback.ui.PlaybackManager
import kotlinx.coroutines.CoroutineScope
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val playbackModule = module {
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
    single {
        MediaController.Builder(
            androidContext(),
            SessionToken(
                androidContext(),
                ComponentName(androidContext(), PlaybackService::class.java)
            )
        )
    }
    singleOf(::PlaybackManager)
}
