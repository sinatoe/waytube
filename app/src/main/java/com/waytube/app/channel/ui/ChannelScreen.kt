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
import com.waytube.app.channel.domain.Channel
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

@Composable
fun ChannelScreen(viewModel: ChannelViewModel) {
    val channelState by viewModel.channelState.collectAsStateWithLifecycle()

    ChannelScreenContent(
        channelState = { channelState },
        videoItems = viewModel.videoItems.collectAsLazyPagingItems(),
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChannelScreenContent(
    channelState: () -> UiState<Channel>,
    videoItems: LazyPagingItems<VideoItem>,
    onRetry: () -> Unit
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = contentPadding
                ) {
                    item {
                        ChannelScreenCard(channel = state.data)
                    }

                    pagingItems(videoItems) { item ->
                        VideoItemCard(
                            item = item,
                            onClick = { /* TODO */ }
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
