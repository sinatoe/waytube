package com.waytube.app.video.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waytube.app.R
import com.waytube.app.common.ui.AppTheme
import com.waytube.app.common.ui.UiState
import com.waytube.app.video.domain.Video

@Composable
fun VideoNowPlayingBar(
    viewModel: VideoViewModel,
    onClick: () -> Unit
) {
    VideoNowPlayingBarContent(
        videoState = viewModel.videoState.collectAsStateWithLifecycle()::value,
        onClick = onClick,
        onStop = viewModel::stop
    )
}

@Composable
private fun VideoNowPlayingBarContent(
    videoState: () -> UiState<Video>?,
    onClick: () -> Unit,
    onStop: () -> Unit
) {
    BottomAppBar(
        modifier = Modifier.clickable(onClick = onClick),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        when (val state = videoState()) {
            null -> {
                Spacer(modifier = Modifier.weight(1f))
            }

            is UiState.Loading -> {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 3.dp
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_error),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = stringResource(R.string.message_video_load_error),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            is UiState.Data -> {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            when (state.data) {
                                is Video.Unavailable -> R.drawable.ic_error
                                is Video.Content -> R.drawable.ic_play_circle
                            }
                        ),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    when (val video = state.data) {
                        is Video.Unavailable -> {
                            Text(
                                text = stringResource(
                                    when (video.reason) {
                                        Video.Unavailable.Reason.AGE_RESTRICTED ->
                                            R.string.message_video_age_restricted

                                        Video.Unavailable.Reason.BOT_FLAGGED ->
                                            R.string.message_video_bot_flagged

                                        Video.Unavailable.Reason.MEMBERS_ONLY ->
                                            R.string.message_video_members_only

                                        Video.Unavailable.Reason.UNSUPPORTED ->
                                            R.string.message_video_unsupported

                                        null -> R.string.message_video_unavailable
                                    }
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        is Video.Content -> {
                            Text(
                                text = video.title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = video.channelName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        IconButton(onClick = onStop) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(R.string.cd_stop)
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun VideoNowPlayingBarPreview() {
    AppTheme {
        VideoNowPlayingBarContent(
            videoState = {
                UiState.Data(
                    Video.Content.Regular(
                        id = "",
                        title = "Example video",
                        channelName = "Example channel",
                        thumbnailUrl = "",
                        dashManifestUrl = ""
                    )
                )
            },
            onClick = {},
            onStop = {}
        )
    }
}
