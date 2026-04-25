package com.waytube.app.playlist.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waytube.app.R
import com.waytube.app.common.domain.VideoItem
import com.waytube.app.common.ui.AppTheme
import com.waytube.app.common.ui.AsyncContent
import com.waytube.app.common.ui.AsyncState
import com.waytube.app.common.ui.BackButton
import com.waytube.app.common.ui.ItemMenuSheet
import com.waytube.app.common.ui.MenuAction
import com.waytube.app.common.ui.MoreOptionsMenu
import com.waytube.app.common.ui.PaginatedData
import com.waytube.app.common.ui.PullToRefreshLayout
import com.waytube.app.common.ui.StyledImage
import com.waytube.app.common.ui.VideoItemCard
import com.waytube.app.common.ui.paginatedItems
import com.waytube.app.common.ui.rememberNavigationBackAction
import com.waytube.app.common.ui.shareText
import com.waytube.app.common.ui.toCompactString
import com.waytube.app.common.ui.toPluralCount
import com.waytube.app.playlist.domain.Playlist
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
fun PlaylistScreen(
    viewModel: PlaylistViewModel,
    onPlayVideo: (String) -> Unit,
    onNavigateToChannel: (String) -> Unit
) {
    PlaylistScreenContent(
        bundleState = viewModel.bundleState.collectAsStateWithLifecycle()::value,
        onShare = LocalContext.current::shareText,
        onPlayVideo = onPlayVideo,
        onNavigateToChannel = onNavigateToChannel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistScreenContent(
    bundleState: () -> AsyncState<PlaylistBundle>,
    onShare: (String) -> Unit,
    onPlayVideo: (String) -> Unit,
    onNavigateToChannel: (String) -> Unit
) {
    val topAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    var selectedItem by retain { mutableStateOf<VideoItem?>(null) }

    selectedItem?.let { item ->
        ItemMenuSheet(
            actions = listOfNotNull(
                MenuAction(
                    label = stringResource(R.string.label_share),
                    iconPainter = painterResource(R.drawable.ic_share),
                    onClick = { onShare(item.url) }
                ),
                item.channelId?.let { id ->
                    MenuAction(
                        label = stringResource(R.string.label_go_to_channel),
                        iconPainter = painterResource(R.drawable.ic_person),
                        onClick = { onNavigateToChannel(id) }
                    )
                }
            ),
            onDismissRequest = { selectedItem = null }
        )
    }

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
                actions = {
                    (bundleState() as? AsyncState.Loaded)?.data?.playlist?.let { playlist ->
                        MoreOptionsMenu(
                            actions = listOf(
                                MenuAction(
                                    label = stringResource(R.string.label_share),
                                    iconPainter = painterResource(R.drawable.ic_share),
                                    onClick = { onShare(playlist.url) }
                                )
                            )
                        )
                    }
                },
                scrollBehavior = topAppBarScrollBehavior
            )
        }
    ) { contentPadding ->
        AsyncContent(
            state = bundleState(),
            contentPadding = contentPadding
        ) { (data, refreshState) ->
            PullToRefreshLayout(
                refreshState = refreshState,
                contentPadding = contentPadding
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding
                ) {
                    item {
                        PlaylistScreenCard(playlist = data.playlist)
                    }

                    paginatedItems(data.videoItems) { item ->
                        VideoItemCard(
                            item = item,
                            onClick = { onPlayVideo(item.id) },
                            onLongClick = { selectedItem = item }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistScreenCard(playlist: Playlist) {
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

@PreviewLightDark
@Composable
private fun PlaylistScreenContentPreview() {
    AppTheme {
        PlaylistScreenContent(
            bundleState = {
                AsyncState.Loaded(
                    data = PlaylistBundle(
                        playlist = Playlist(
                            id = "",
                            url = "",
                            title = "Example playlist",
                            channelName = "Example channel",
                            thumbnailUrl = "",
                            videoCount = 123
                        ),
                        videoItems = PaginatedData(
                            items = (1..10).map { n ->
                                VideoItem.Regular(
                                    id = n.toString(),
                                    url = "",
                                    title = "Example video",
                                    channelId = "",
                                    channelName = "Example channel",
                                    thumbnailUrl = "",
                                    duration = 12.minutes + 34.seconds,
                                    viewCount = 1_234_567L,
                                    uploadedAt = Clock.System.now() - 14.days
                                )
                            },
                            state = PaginatedData.State.Done
                        )
                    ),
                    refreshState = AsyncState.Loaded.RefreshState.Idle(refresh = {})
                )
            },
            onShare = {},
            onPlayVideo = {},
            onNavigateToChannel = {}
        )
    }
}
