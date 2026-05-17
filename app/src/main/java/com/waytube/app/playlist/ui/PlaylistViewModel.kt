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
    private val bundleRefreshSignal = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val videoItemsLoadSignal = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val responseState = asyncStateFlow(bundleRefreshSignal) { repository.getPlaylist(id) }
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            replay = 1
        )

    private val videoItems = responseState
        .filterIsInstance<AsyncState.Loaded<PlaylistResponse.Content>>()
        .map { it.data.videoItemsPage }
        .distinctUntilChanged()
        .flatMapLatest { paginatedDataFlow(loadSignal = videoItemsLoadSignal, page = it) }
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            replay = 1
        )

    val bundleState = responseState
        .flatMapLatestData { state ->
            when (state) {
                is PlaylistResponse.Content -> {
                    videoItems.map { videoItems ->
                        PlaylistBundle.Content(
                            playlist = state.playlist,
                            videoItems = videoItems
                        )
                    }
                }

                PlaylistResponse.Unavailable -> {
                    flowOf(PlaylistBundle.Unavailable)
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = AsyncState.Loading
        )

    fun refreshBundle() {
        bundleRefreshSignal.tryEmit(Unit)
    }

    fun loadVideoItems() {
        videoItemsLoadSignal.tryEmit(Unit)
    }
}
