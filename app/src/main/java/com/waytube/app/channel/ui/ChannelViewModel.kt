package com.waytube.app.channel.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waytube.app.channel.domain.Channel
import com.waytube.app.channel.domain.ChannelRepository
import com.waytube.app.common.ui.PagedListLoader
import com.waytube.app.common.ui.UiState
import com.waytube.app.common.ui.UiStateLoader
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ChannelViewModel(
    private val id: String,
    private val repository: ChannelRepository
) : ViewModel() {
    private val channelLoader = UiStateLoader()

    val videoItemsLoader = PagedListLoader()

    val channelState = channelLoader
        .bind { repository.getChannel(id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = UiState.Loading
        )

    val videoItems = channelState
        .map { ((it as? UiState.Data)?.data as? Channel.Content)?.id }
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
        viewModelScope.launch { channelLoader.retry() }
    }

    fun loadVideoItems() {
        viewModelScope.launch { videoItemsLoader.load() }
    }
}
