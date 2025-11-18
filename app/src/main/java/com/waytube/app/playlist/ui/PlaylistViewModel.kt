package com.waytube.app.playlist.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.waytube.app.common.ui.UiState
import com.waytube.app.playlist.domain.Playlist
import com.waytube.app.playlist.domain.PlaylistRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistViewModel(
    private val id: String,
    private val repository: PlaylistRepository
) : ViewModel() {
    private val fetchTrigger = MutableSharedFlow<Unit>()

    val playlistState = fetchTrigger
        .onStart { emit(Unit) }
        .transformLatest {
            emit(UiState.Loading)
            emit(
                repository.getPlaylist(id).fold(
                    onSuccess = { UiState.Data(it) },
                    onFailure = { UiState.Error(it) }
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = UiState.Loading
        )

    val videoItems = playlistState
        .map { ((it as? UiState.Data)?.data as? Playlist.Content)?.id }
        .distinctUntilChanged()
        .flatMapLatest { id ->
            if (id != null) repository.getVideoItems(id) else flowOf(PagingData.empty())
        }
        .cachedIn(viewModelScope)

    fun retry() {
        viewModelScope.launch { fetchTrigger.emit(Unit) }
    }
}
