package com.waytube.app.video.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.waytube.app.R
import com.waytube.app.common.ui.async.AsyncContent
import com.waytube.app.common.ui.async.AsyncState
import com.waytube.app.common.ui.element.StateMessage
import com.waytube.app.common.ui.theming.AppColorScheme
import com.waytube.app.video.domain.VideoResponse
import com.waytube.app.video.domain.VideoRestriction

@Composable
fun VideoScreen(viewModel: VideoViewModel) {
    VideoScreenContent(
        videoResponseState = viewModel.videoResponseState.collectAsStateWithLifecycle()::value,
        player = viewModel.player.collectAsStateWithLifecycle(initialValue = null)::value,
        isPlaying = viewModel.isPlaying.collectAsStateWithLifecycle()::value,
    )
}

@Composable
private fun VideoScreenContent(
    videoResponseState: () -> AsyncState<VideoResponse>?,
    player: () -> Player?,
    isPlaying: () -> Boolean
) {
    MaterialTheme(colorScheme = AppColorScheme.Dark) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.scrim,
            contentColor = MaterialTheme.colorScheme.onSurface,
            contentWindowInsets = WindowInsets.displayCutout
        ) { contentPadding ->
            videoResponseState()?.let { state ->
                AsyncContent(
                    state = state,
                    contentPadding = contentPadding
                ) { (response) ->
                    when (response) {
                        is VideoResponse.Content -> {
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

                        is VideoResponse.Unavailable -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(contentPadding),
                                contentAlignment = Alignment.Center
                            ) {
                                StateMessage(
                                    text = stringResource(
                                        when (response.restriction) {
                                            VideoRestriction.AGE -> R.string.message_video_age_restricted
                                            VideoRestriction.MEMBERS_ONLY -> R.string.message_video_members_only
                                            VideoRestriction.PRIVATE -> R.string.message_video_private
                                            VideoRestriction.REGION -> R.string.message_video_region_blocked
                                            null -> R.string.message_video_unavailable
                                        }
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
