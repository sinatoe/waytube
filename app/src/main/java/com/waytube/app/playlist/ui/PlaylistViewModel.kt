package com.waytube.app.playlist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waytube.app.common.ui.async.AsyncState
import com.waytube.app.common.ui.async.asyncStateFlow
import com.waytube.app.common.ui.async.flatMapLoaded
import com.waytube.app.common.ui.pagination.paginatedDataFlow
import com.waytube.app.playlist.domain.PlaylistRepository
import com.waytube.app.playlist.domain.PlaylistResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistViewModel(
    private val id: String,
    private val repository: PlaylistRepository
) : ViewModel() {
    val bundleState = asyncStateFlow { repository.getPlaylist(id) }
        .flatMapLoaded { response ->
            when (response) {
                is PlaylistResponse.Content -> {
                    paginatedDataFlow(response.videoItemsPage).map { videoItems ->
                        PlaylistBundle.Content(
                            playlist = response.playlist,
                            videoItems = videoItems
                        )
                    }
                }

                PlaylistResponse.Unavailable -> flowOf(PlaylistBundle.Unavailable)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = AsyncState.Loading
        )
}
