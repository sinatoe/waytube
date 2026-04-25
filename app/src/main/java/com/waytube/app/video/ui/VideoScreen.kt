package com.waytube.app.video.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.waytube.app.common.ui.AppColorScheme
import com.waytube.app.common.ui.AsyncContent
import com.waytube.app.common.ui.AsyncState
import com.waytube.app.video.domain.Video

@Composable
fun VideoScreen(viewModel: VideoViewModel) {
    VideoScreenContent(
        videoState = viewModel.videoState.collectAsStateWithLifecycle()::value,
        player = viewModel.player.collectAsStateWithLifecycle(initialValue = null)::value,
        isPlaying = viewModel.isPlaying.collectAsStateWithLifecycle()::value,
    )
}

@Composable
private fun VideoScreenContent(
    videoState: () -> AsyncState<Video>?,
    player: () -> Player?,
    isPlaying: () -> Boolean
) {
    MaterialTheme(colorScheme = AppColorScheme.Dark) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.scrim,
            contentColor = MaterialTheme.colorScheme.onSurface,
            contentWindowInsets = WindowInsets.displayCutout
        ) { contentPadding ->
            videoState()?.let { state ->
                AsyncContent(
                    state = state,
                    contentPadding = contentPadding
                ) {
                    player()?.let { player ->
                        AndroidView(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(contentPadding)
                                .then(
                                    if (isPlaying()) Modifier.keepScreenOn() else Modifier
                                ),
                            factory = { context ->
                                PlayerView(context).apply {
                                    this.player = player
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
