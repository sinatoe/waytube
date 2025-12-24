package com.waytube.app.search.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopSearchBar
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.waytube.app.R
import com.waytube.app.common.domain.VideoItem
import com.waytube.app.common.ui.AppTheme
import com.waytube.app.common.ui.BackButton
import com.waytube.app.common.ui.ChannelItemCard
import com.waytube.app.common.ui.ItemMenuSheet
import com.waytube.app.common.ui.PlaylistItemCard
import com.waytube.app.common.ui.VideoItemCard
import com.waytube.app.common.ui.pagingItems
import com.waytube.app.common.ui.shareText
import com.waytube.app.search.domain.SearchFilter
import com.waytube.app.search.domain.SearchResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateToVideo: (String) -> Unit,
    onNavigateToChannel: (String) -> Unit,
    onNavigateToPlaylist: (String) -> Unit
) {
    val isQuerySubmitted by viewModel.isQuerySubmitted.collectAsStateWithLifecycle()
    val results = viewModel.results.collectAsLazyPagingItems()

    val textFieldState = rememberTextFieldState()

    LaunchedEffect(textFieldState.text) {
        viewModel.setSuggestionQuery(textFieldState.text.toString())
    }

    SearchScreenContent(
        textFieldState = textFieldState,
        suggestions = viewModel.suggestions.collectAsStateWithLifecycle()::value,
        selectedFilter = viewModel.selectedFilter.collectAsStateWithLifecycle()::value,
        results = { if (isQuerySubmitted) results else null },
        onTrySubmit = viewModel::trySubmit,
        onFilterClick = viewModel::toggleFilter,
        onShare = LocalContext.current::shareText,
        onNavigateToVideo = onNavigateToVideo,
        onNavigateToChannel = onNavigateToChannel,
        onNavigateToPlaylist = onNavigateToPlaylist
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchScreenContent(
    textFieldState: TextFieldState,
    suggestions: () -> SearchSuggestions,
    selectedFilter: () -> SearchFilter?,
    results: () -> LazyPagingItems<SearchResult>?,
    onTrySubmit: (String) -> Boolean,
    onFilterClick: (SearchFilter) -> Unit,
    onShare: (String) -> Unit,
    onNavigateToVideo: (String) -> Unit,
    onNavigateToChannel: (String) -> Unit,
    onNavigateToPlaylist: (String) -> Unit
) {
    val searchBarState = rememberSearchBarState()
    val scope = rememberCoroutineScope()

    var selectedMenuResult by remember { mutableStateOf<SearchResult?>(null) }

    val inputField = @Composable {
        SearchBarDefaults.InputField(
            textFieldState = textFieldState,
            searchBarState = searchBarState,
            onSearch = { query ->
                if (onTrySubmit(query)) {
                    scope.launch { searchBarState.animateToCollapsed() }
                }
            },
            leadingIcon = {
                if (searchBarState.targetValue == SearchBarValue.Expanded) {
                    BackButton(
                        onClick = {
                            scope.launch { searchBarState.animateToCollapsed() }
                        }
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_search),
                        contentDescription = null
                    )
                }
            },
            placeholder = {
                Text(text = stringResource(R.string.label_search))
            },
            trailingIcon = if (
                searchBarState.targetValue == SearchBarValue.Expanded
                && textFieldState.text.isNotEmpty()
            ) ({
                IconButton(onClick = textFieldState::clearText) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = stringResource(R.string.cd_clear)
                    )
                }
            }) else null
        )
    }

    selectedMenuResult?.let { result ->
        ItemMenuSheet(
            onDismissRequest = { selectedMenuResult = null },
            onShare = {
                onShare(
                    when (result) {
                        is SearchResult.Video -> result.item.url
                        is SearchResult.Channel -> result.item.url
                        is SearchResult.Playlist -> result.item.url
                    }
                )
            },
            onNavigateToChannel = when (result) {
                is SearchResult.Video -> result.item.channelId
                is SearchResult.Channel -> null
                is SearchResult.Playlist -> result.item.channelId
            }?.let { id -> { onNavigateToChannel(id) } }
        )
    }

    ExpandedFullScreenSearchBar(
        state = searchBarState,
        inputField = inputField,
        windowInsets = {
            SearchBarDefaults.fullScreenWindowInsets.only(
                WindowInsetsSides.Horizontal + WindowInsetsSides.Top
            )
        }
    ) {
        LazyColumn(
            contentPadding = SearchBarDefaults.fullScreenWindowInsets
                .only(WindowInsetsSides.Bottom)
                .asPaddingValues()
        ) {
            suggestions().run {
                items(items) { suggestion ->
                    ListItem(
                        modifier = Modifier.clickable {
                            if (onTrySubmit(suggestion)) {
                                textFieldState.setTextAndPlaceCursorAtEnd(suggestion)
                                scope.launch { searchBarState.animateToCollapsed() }
                            }
                        },
                        leadingContent = {
                            Icon(
                                painter = painterResource(
                                    when (type) {
                                        SearchSuggestions.Type.HISTORY -> R.drawable.ic_history
                                        SearchSuggestions.Type.REMOTE -> R.drawable.ic_search
                                    }
                                ),
                                contentDescription = null
                            )
                        },
                        headlineContent = {
                            Text(
                                text = suggestion,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopSearchBar(
                state = searchBarState,
                inputField = inputField
            )
        }
    ) { contentPadding ->
        results()?.let { results ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding
            ) {
                item {
                    LazyRow(
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(SearchFilter.entries) { filter ->
                            val isSelected = selectedFilter() == filter

                            FilterChip(
                                selected = isSelected,
                                onClick = { onFilterClick(filter) },
                                leadingIcon = if (isSelected) ({
                                    Icon(
                                        painter = painterResource(R.drawable.ic_check),
                                        contentDescription = stringResource(R.string.cd_selected),
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }) else null,
                                label = {
                                    Text(
                                        text = stringResource(
                                            when (filter) {
                                                SearchFilter.VIDEOS -> R.string.label_videos
                                                SearchFilter.CHANNELS -> R.string.label_channels
                                                SearchFilter.PLAYLISTS -> R.string.label_playlists
                                            }
                                        )
                                    )
                                }
                            )
                        }
                    }
                }

                pagingItems(results) { result ->
                    when (result) {
                        is SearchResult.Video -> {
                            VideoItemCard(
                                item = result.item,
                                onClick = { onNavigateToVideo(result.id) },
                                onLongClick = { selectedMenuResult = result }
                            )
                        }

                        is SearchResult.Channel -> {
                            ChannelItemCard(
                                item = result.item,
                                onClick = { onNavigateToChannel(result.id) },
                                onLongClick = { selectedMenuResult = result }
                            )
                        }

                        is SearchResult.Playlist -> {
                            PlaylistItemCard(
                                item = result.item,
                                onClick = { onNavigateToPlaylist(result.id) },
                                onLongClick = { selectedMenuResult = result }
                            )
                        }
                    }
                }
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun SearchScreenPreview() {
    val results = MutableStateFlow(
        PagingData.from<SearchResult>(
            (1..10).map { n ->
                SearchResult.Video(
                    VideoItem.Regular(
                        id = n.toString(),
                        url = "",
                        title = "Example video",
                        channelId = "",
                        channelName = "Example channel",
                        thumbnailUrl = "",
                        duration = 12.minutes + 34.seconds,
                        viewCount = 1_234_567,
                        uploadedAt = Clock.System.now() - 14.days
                    )
                )
            }
        )
    ).collectAsLazyPagingItems()

    AppTheme {
        SearchScreenContent(
            textFieldState = rememberTextFieldState(initialText = "example query"),
            suggestions = {
                SearchSuggestions(
                    items = (1..10).map { n -> "example suggestion $n" },
                    type = SearchSuggestions.Type.HISTORY
                )
            },
            selectedFilter = { SearchFilter.VIDEOS },
            results = { results },
            onTrySubmit = { true },
            onFilterClick = {},
            onShare = {},
            onNavigateToVideo = {},
            onNavigateToChannel = {},
            onNavigateToPlaylist = {}
        )
    }
}
