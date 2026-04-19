package com.waytube.app.playlist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waytube.app.common.ui.AsyncState
import com.waytube.app.common.ui.PaginatedData
import com.waytube.app.playlist.domain.Playlist
import com.waytube.app.playlist.domain.PlaylistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistViewModel(
    private val id: String,
    private val repository: PlaylistRepository
) : ViewModel() {
    val playlistState = AsyncState
        .createFlow { repository.getPlaylist(id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = AsyncState.Loading
        )

    val videoItems = playlistState
        .map { ((it as? AsyncState.Loaded)?.data as? Playlist.Content)?.id }
        .distinctUntilChanged()
        .flatMapLatest { id ->
            if (id != null) {
                PaginatedData.createFlow { repository.getVideoItems(id) }
            } else flowOf(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = null
        )
}
