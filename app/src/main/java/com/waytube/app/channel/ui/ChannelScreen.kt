package com.waytube.app.channel.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
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
import com.waytube.app.channel.domain.Channel
import com.waytube.app.common.domain.VideoItem
import com.waytube.app.common.ui.element.BackButton
import com.waytube.app.common.ui.element.PullToRefreshLayout
import com.waytube.app.common.ui.element.StyledImage
import com.waytube.app.common.ui.element.VideoItemCard
import com.waytube.app.common.ui.action.rememberNavigationBackAction
import com.waytube.app.common.ui.action.shareText
import com.waytube.app.common.ui.async.AsyncContent
import com.waytube.app.common.ui.async.AsyncState
import com.waytube.app.common.ui.formatting.toCompactString
import com.waytube.app.common.ui.formatting.toPluralCount
import com.waytube.app.common.ui.menu.ItemMenuSheet
import com.waytube.app.common.ui.menu.MenuAction
import com.waytube.app.common.ui.menu.MoreOptionsMenu
import com.waytube.app.common.ui.pagination.PaginatedData
import com.waytube.app.common.ui.pagination.paginatedItems
import com.waytube.app.common.ui.theming.AppTheme
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
        bundleState = viewModel.bundleState.collectAsStateWithLifecycle()::value,
        onShare = LocalContext.current::shareText,
        onPlayVideo = onPlayVideo
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChannelScreenContent(
    bundleState: () -> AsyncState<ChannelBundle>,
    onShare: (String) -> Unit,
    onPlayVideo: (String) -> Unit
) {
    val topAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    var selectedItem by retain { mutableStateOf<VideoItem?>(null) }

    selectedItem?.let { item ->
        ItemMenuSheet(
            actions = listOf(
                MenuAction(
                    label = stringResource(R.string.label_share),
                    iconPainter = painterResource(R.drawable.ic_share),
                    onClick = { onShare(item.url) }
                )
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
                    Text(text = stringResource(R.string.label_channel))
                },
                actions = {
                    (bundleState() as? AsyncState.Loaded)?.data?.channel?.let { channel ->
                        MoreOptionsMenu(
                            actions = listOf(
                                MenuAction(
                                    label = stringResource(R.string.label_share),
                                    iconPainter = painterResource(R.drawable.ic_share),
                                    onClick = { onShare(channel.url) }
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
        ) { (data, isRefreshing, refresh) ->
            PullToRefreshLayout(
                isRefreshing = isRefreshing,
                onRefresh = refresh,
                contentPadding = contentPadding
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding
                ) {
                    item {
                        ChannelScreenCard(channel = data.channel)
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
private fun ChannelScreenCard(channel: Channel) {
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
    AppTheme {
        ChannelScreenContent(
            bundleState = {
                AsyncState.Loaded(
                    data = ChannelBundle(
                        channel = Channel(
                            id = "",
                            url = "",
                            name = "Example channel",
                            avatarUrl = "",
                            bannerUrl = "",
                            subscriberCount = 1_234_567
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
                    isRefreshing = false,
                    refresh = {}
                )
            },
            onShare = {},
            onPlayVideo = {}
        )
    }
}
