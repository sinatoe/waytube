package com.waytube.app.playlist.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waytube.app.R
import com.waytube.app.common.domain.VideoItem
import com.waytube.app.common.ui.action.shareText
import com.waytube.app.common.ui.async.AsyncState
import com.waytube.app.common.ui.async.AsyncStateScaffold
import com.waytube.app.common.ui.element.VideoItemCard
import com.waytube.app.common.ui.formatting.toCompactString
import com.waytube.app.common.ui.formatting.toPluralCount
import com.waytube.app.common.ui.menu.ItemMenuSheet
import com.waytube.app.common.ui.menu.MenuAction
import com.waytube.app.common.ui.menu.MoreOptionsMenu
import com.waytube.app.common.ui.pagination.PaginatedData
import com.waytube.app.common.ui.pagination.paginatedDataItems
import com.waytube.app.common.ui.theming.AppTheme
import com.waytube.app.playlist.domain.Playlist
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
fun PlaylistScreen(
    viewModel: PlaylistViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
    onNavigateToChannel: (String) -> Unit
) {
    PlaylistScreenContent(
        bundleState = viewModel.bundleState.collectAsStateWithLifecycle().value,
        onRefreshBundle = viewModel::refreshBundle,
        onShare = LocalContext.current::shareText,
        onNavigateBack = onNavigateBack,
        onNavigateToVideo = onNavigateToVideo,
        onNavigateToChannel = onNavigateToChannel
    )
}

@Composable
private fun PlaylistScreenContent(
    bundleState: AsyncState<PlaylistBundle>,
    onRefreshBundle: () -> Unit,
    onShare: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToVideo: (String) -> Unit,
    onNavigateToChannel: (String) -> Unit
) {
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

    AsyncStateScaffold(
        state = bundleState,
        onRefresh = onRefreshBundle,
        title = stringResource(R.string.label_playlist),
        onNavigateBack = onNavigateBack,
        actions = { bundle ->
            when (bundle) {
                is PlaylistBundle.Content -> {
                    MoreOptionsMenu(
                        actions = listOf(
                            MenuAction(
                                label = stringResource(R.string.label_share),
                                iconPainter = painterResource(R.drawable.ic_share),
                                onClick = { onShare(bundle.playlist.url) }
                            )
                        )
                    )
                }

                is PlaylistBundle.Unavailable -> {}
            }
        }
    ) { bundle, contentPadding ->
        when (bundle) {
            is PlaylistBundle.Content -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding
                ) {
                    item {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = bundle.playlist.title,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleLarge
                            )

                            Text(
                                text = bundle.playlist.channelName,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = pluralStringResource(
                                    R.plurals.video_count,
                                    bundle.playlist.videoCount.toPluralCount(),
                                    bundle.playlist.videoCount.toCompactString()
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    paginatedDataItems(bundle.videoItems) { item ->
                        VideoItemCard(
                            item = item,
                            onClick = { onNavigateToVideo(item.id) },
                            onLongClick = { selectedItem = item }
                        )
                    }
                }
            }

            PlaylistBundle.Unavailable -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.message_playlist_unavailable),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun PlaylistScreenContentPreview() {
    AppTheme {
        PlaylistScreenContent(
            bundleState = AsyncState.Loaded(
                data = PlaylistBundle.Content(
                    playlist = Playlist(
                        id = "",
                        url = "",
                        title = "Example playlist",
                        channelName = "Example channel",
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
                isRefreshing = false
            ),
            onRefreshBundle = {},
            onShare = {},
            onNavigateBack = {},
            onNavigateToVideo = {},
            onNavigateToChannel = {}
        )
    }
}
