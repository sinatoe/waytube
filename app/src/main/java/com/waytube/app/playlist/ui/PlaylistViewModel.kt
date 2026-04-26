package com.waytube.app.playlist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waytube.app.common.domain.flatMap
import com.waytube.app.common.domain.map
import com.waytube.app.common.ui.async.AsyncState
import com.waytube.app.common.ui.async.asyncStateFlow
import com.waytube.app.common.ui.pagination.paginatedDataFlow
import com.waytube.app.playlist.domain.PlaylistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistViewModel(
    private val id: String,
    private val repository: PlaylistRepository
) : ViewModel() {
    val bundleState = asyncStateFlow {
        repository.getPlaylist(id).flatMap { playlist ->
            repository.getVideoItems(playlist.id).map { page -> playlist to page }
        }
    }
        .flatMapLatest { state ->
            when (state) {
                is AsyncState.Loading, is AsyncState.Error -> flowOf(state)

                is AsyncState.Loaded -> {
                    val (playlist, page) = state.data

                    paginatedDataFlow(page).map { videoItems ->
                        AsyncState.Loaded(
                            data = PlaylistBundle(playlist, videoItems),
                            isRefreshing = state.isRefreshing,
                            refresh = state.refresh
                        )
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = AsyncState.Loading
        )
}
