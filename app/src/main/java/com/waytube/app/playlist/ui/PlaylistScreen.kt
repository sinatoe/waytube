@file:OptIn(ExperimentalMaterial3Api::class)

package com.waytube.app.playlist.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.waytube.app.R
import com.waytube.app.common.domain.VideoItem
import com.waytube.app.common.ui.BackButton
import com.waytube.app.common.ui.StateMessage
import com.waytube.app.common.ui.StyledImage
import com.waytube.app.common.ui.UiState
import com.waytube.app.common.ui.VideoItemCard
import com.waytube.app.common.ui.pagingItems
import com.waytube.app.common.ui.rememberNavigationBackAction
import com.waytube.app.common.ui.toCompactString
import com.waytube.app.common.ui.toPluralCount
import com.waytube.app.playlist.domain.Playlist

@Composable
fun PlaylistScreen(
    viewModel: PlaylistViewModel,
    onNavigateToVideo: (String) -> Unit
) {
    val playlistState by viewModel.playlistState.collectAsStateWithLifecycle()
    val videoItems = viewModel.videoItems.collectAsLazyPagingItems()

    PlaylistScreenContent(
        playlistState = { playlistState },
        videoItems = videoItems,
        onRetry = viewModel::retry,
        onNavigateToVideo = onNavigateToVideo
    )
}

@Composable
private fun PlaylistScreenContent(
    playlistState: () -> UiState<Playlist>,
    videoItems: LazyPagingItems<VideoItem>,
    onRetry: () -> Unit,
    onNavigateToVideo: (String) -> Unit
) {
    val topAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    BackButton(onClick = rememberNavigationBackAction())
                },
                title = {
                    Text(text = stringResource(R.string.label_playlist))
                },
                scrollBehavior = topAppBarScrollBehavior
            )
        }
    ) { contentPadding ->
        when (val state = playlistState()) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                    contentAlignment = Alignment.Center
                ) {
                    StateMessage(
                        text = stringResource(R.string.message_playlist_load_error),
                        onRetry = onRetry
                    )
                }
            }

            is UiState.Data -> {
                when (val playlist = state.data) {
                    is Playlist.Unavailable -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(contentPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            StateMessage(
                                text = stringResource(R.string.message_playlist_unavailable)
                            )
                        }
                    }

                    is Playlist.Content -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = contentPadding
                        ) {
                            item {
                                PlaylistScreenCard(playlist = playlist)
                            }

                            pagingItems(videoItems) { item ->
                                VideoItemCard(
                                    item = item,
                                    onClick = { onNavigateToVideo(item.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistScreenCard(playlist: Playlist.Content) {
    Card(modifier = Modifier.padding(8.dp)) {
        StyledImage(
            data = playlist.thumbnailUrl,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9)
                .clip(CardDefaults.shape)
        )

        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = playlist.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = playlist.channelName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = pluralStringResource(
                    R.plurals.video_count,
                    playlist.videoCount.toPluralCount(),
                    playlist.videoCount.toCompactString()
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
