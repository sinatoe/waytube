package com.waytube.app.playlist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waytube.app.common.ui.PagedListLoader
import com.waytube.app.common.ui.UiState
import com.waytube.app.common.ui.UiStateLoader
import com.waytube.app.playlist.domain.Playlist
import com.waytube.app.playlist.domain.PlaylistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistViewModel(
    private val id: String,
    private val repository: PlaylistRepository
) : ViewModel() {
    private val playlistLoader = UiStateLoader()

    private val videoItemsLoader = PagedListLoader()

    val playlistState = playlistLoader
        .bind { repository.getPlaylist(id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = UiState.Loading
        )

    val videoItems = playlistState
        .map { ((it as? UiState.Data)?.data as? Playlist.Content)?.id }
        .distinctUntilChanged()
        .flatMapLatest { id ->
            if (id != null) {
                videoItemsLoader.bind { repository.getVideoItems(id) }
            } else flowOf(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = null
        )

    fun retry() {
        viewModelScope.launch { playlistLoader.retry() }
    }

    fun loadVideoItems() {
        viewModelScope.launch { videoItemsLoader.load() }
    }
}
