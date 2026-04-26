package com.waytube.app.channel.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waytube.app.channel.domain.ChannelRepository
import com.waytube.app.common.domain.flatMap
import com.waytube.app.common.domain.map
import com.waytube.app.common.ui.async.AsyncState
import com.waytube.app.common.ui.async.asyncStateFlow
import com.waytube.app.common.ui.pagination.paginatedDataFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class ChannelViewModel(
    private val id: String,
    private val repository: ChannelRepository
) : ViewModel() {
    val bundleState = asyncStateFlow {
        repository.getChannel(id).flatMap { channel ->
            repository.getVideoItems(channel.id).map { page -> channel to page }
        }
    }
        .flatMapLatest { state ->
            when (state) {
                is AsyncState.Loading, is AsyncState.Error -> flowOf(state)

                is AsyncState.Loaded -> {
                    val (channel, page) = state.data

                    paginatedDataFlow(page).map { videoItems ->
                        AsyncState.Loaded(
                            data = ChannelBundle(channel, videoItems),
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
