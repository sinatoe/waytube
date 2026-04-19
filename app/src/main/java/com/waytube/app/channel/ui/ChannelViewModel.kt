package com.waytube.app.channel.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waytube.app.channel.domain.Channel
import com.waytube.app.channel.domain.ChannelRepository
import com.waytube.app.common.ui.AsyncState
import com.waytube.app.common.ui.PagedListLoader
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
    val videoItemsLoader = PagedListLoader()

    val channelState = AsyncState
        .createFlow { repository.getChannel(id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = AsyncState.Loading
        )

    val videoItems = channelState
        .map { ((it as? AsyncState.Loaded)?.data as? Channel.Content)?.id }
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

    fun loadVideoItems() {
        viewModelScope.launch { videoItemsLoader.load() }
    }
}
