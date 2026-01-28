package com.waytube.app.channel.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.waytube.app.R
import com.waytube.app.channel.domain.Channel
import com.waytube.app.common.domain.VideoItem
import com.waytube.app.common.ui.AppTheme
import com.waytube.app.common.ui.BackButton
import com.waytube.app.common.ui.ItemMenuSheet
import com.waytube.app.common.ui.StateMessage
import com.waytube.app.common.ui.StyledImage
import com.waytube.app.common.ui.UiState
import com.waytube.app.common.ui.VideoItemCard
import com.waytube.app.common.ui.pagingItems
import com.waytube.app.common.ui.rememberNavigationBackAction
import com.waytube.app.common.ui.shareText
import com.waytube.app.common.ui.toCompactString
import com.waytube.app.common.ui.toPluralCount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
fun ChannelScreen(
    viewModel: ChannelViewModel,
    onPlayVideo: (String) -> Unit
) {
    ChannelScreenContent(
        channelState = viewModel.channelState.collectAsStateWithLifecycle()::value,
        videoItems = viewModel.videoItems.collectAsLazyPagingItems(),
        onRetry = viewModel::retry,
        onShare = LocalContext.current::shareText,
        onPlayVideo = onPlayVideo
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChannelScreenContent(
    channelState: () -> UiState<Channel>,
    videoItems: LazyPagingItems<VideoItem>,
    onRetry: () -> Unit,
    onShare: (String) -> Unit,
    onPlayVideo: (String) -> Unit
) {
    val topAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    var selectedMenuItem by remember { mutableStateOf<VideoItem?>(null) }

    selectedMenuItem?.let { item ->
        ItemMenuSheet(
            onDismissRequest = { selectedMenuItem = null },
            onShare = { onShare(item.url) }
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
                    Text(text = stringResource(R.string.label_channel))
                },
                scrollBehavior = topAppBarScrollBehavior
            )
        }
    ) { contentPadding ->
        when (val state = channelState()) {
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
                        text = stringResource(R.string.message_channel_load_error),
                        onRetry = onRetry
                    )
                }
            }

            is UiState.Data -> {
                when (val channel = state.data) {
                    is Channel.Unavailable -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(contentPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            StateMessage(
                                text = stringResource(R.string.message_channel_unavailable)
                            )
                        }
                    }

                    is Channel.Content -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = contentPadding
                        ) {
                            item {
                                ChannelScreenCard(channel = channel)
                            }

                            pagingItems(videoItems) { item ->
                                VideoItemCard(
                                    item = item,
                                    onClick = { onPlayVideo(item.id) },
                                    onLongClick = { selectedMenuItem = item }
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
private fun ChannelScreenCard(channel: Channel.Content) {
    Card(modifier = Modifier.padding(8.dp)) {
        channel.bannerUrl?.let { bannerUrl ->
            StyledImage(
                data = bannerUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f)
                    .clip(CardDefaults.shape)
            )
        }

        Row(
            modifier = Modifier
                .padding(12.dp)
                .padding(end = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StyledImage(
                data = channel.avatarUrl,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge
                )

                channel.subscriberCount?.let { subscriberCount ->
                    Text(
                        text = pluralStringResource(
                            R.plurals.subscriber_count,
                            subscriberCount.toPluralCount(),
                            subscriberCount.toCompactString()
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ChannelScreenContentPreview() {
    val videoItems = MutableStateFlow(
        PagingData.from<VideoItem>(
            (1..10).map { n ->
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
            }
        )
    ).collectAsLazyPagingItems()

    AppTheme {
        ChannelScreenContent(
            channelState = {
                UiState.Data(
                    Channel.Content(
                        id = "",
                        name = "Example channel",
                        avatarUrl = "",
                        bannerUrl = "",
                        subscriberCount = 1_234_567
                    )
                )
            },
            videoItems = videoItems,
            onRetry = {},
            onShare = {},
            onPlayVideo = {}
        )
    }
}
