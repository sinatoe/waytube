package com.waytube.app.playlist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waytube.app.common.ui.async.AsyncState
import com.waytube.app.common.ui.async.asyncStateFlow
import com.waytube.app.common.ui.async.flatMapLatestData
import com.waytube.app.common.ui.pagination.paginatedDataFlow
import com.waytube.app.playlist.domain.PlaylistRepository
import com.waytube.app.playlist.domain.PlaylistResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistViewModel(
    private val id: String,
    private val repository: PlaylistRepository
) : ViewModel() {
    private val refreshSignal = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val videoItemsLoadSignal = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val responseState = asyncStateFlow(refreshSignal) { repository.getPlaylist(id) }
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            replay = 1
        )

    private val videoItems = responseState
        .filterIsInstance<AsyncState.Loaded<PlaylistResponse.Content>>()
        .map { it.data.videoItemsPage }
        .distinctUntilChanged()
        .flatMapLatest { page ->
            paginatedDataFlow(
                loadSignal = videoItemsLoadSignal,
                page = page
            )
        }
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            replay = 1
        )

    val modelState = responseState
        .flatMapLatestData { response ->
            when (response) {
                is PlaylistResponse.Content -> {
                    videoItems.map { videoItems ->
                        PlaylistModel.Content(
                            playlist = response.playlist,
                            videoItems = videoItems
                        )
                    }
                }

                PlaylistResponse.Unavailable -> {
                    flowOf(PlaylistModel.Unavailable)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = AsyncState.Loading
        )

    fun handleIntent(intent: PlaylistIntent) {
        when (intent) {
            is PlaylistIntent.Refresh -> refreshSignal.tryEmit(Unit)
            is PlaylistIntent.LoadVideoItems -> videoItemsLoadSignal.tryEmit(Unit)
        }
    }
}
