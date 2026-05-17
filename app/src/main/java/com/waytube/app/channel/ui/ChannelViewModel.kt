package com.waytube.app.channel.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waytube.app.channel.domain.ChannelRepository
import com.waytube.app.channel.domain.ChannelResponse
import com.waytube.app.common.ui.async.AsyncState
import com.waytube.app.common.ui.async.asyncStateFlow
import com.waytube.app.common.ui.async.flatMapLatestData
import com.waytube.app.common.ui.pagination.paginatedDataFlow
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
class ChannelViewModel(
    private val id: String,
    private val repository: ChannelRepository
) : ViewModel() {
    private val bundleRefreshSignal = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val videoItemsLoadSignal = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val responseState = asyncStateFlow(bundleRefreshSignal) { repository.getChannel(id) }
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            replay = 1
        )

    private val videoItems = responseState
        .filterIsInstance<AsyncState.Loaded<ChannelResponse.Content>>()
        .map { it.data.videoItemsPage }
        .distinctUntilChanged()
        .flatMapLatest { paginatedDataFlow(loadSignal = videoItemsLoadSignal, page = it) }
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            replay = 1
        )

    val bundleState = responseState
        .flatMapLatestData { data ->
            when (data) {
                is ChannelResponse.Content -> {
                    videoItems.map { videoItems ->
                        ChannelBundle.Content(
                            channel = data.channel,
                            videoItems = videoItems
                        )
                    }
                }

                ChannelResponse.Unavailable -> {
                    flowOf(ChannelBundle.Unavailable)
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
