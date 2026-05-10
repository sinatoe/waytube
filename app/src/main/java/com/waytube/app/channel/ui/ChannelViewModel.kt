package com.waytube.app.channel.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waytube.app.channel.domain.ChannelRepository
import com.waytube.app.channel.domain.ChannelResponse
import com.waytube.app.common.ui.async.AsyncState
import com.waytube.app.common.ui.async.asyncStateFlow
import com.waytube.app.common.ui.async.flatMapLoaded
import com.waytube.app.common.ui.pagination.paginatedDataFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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

    val bundleState = asyncStateFlow(bundleRefreshSignal) { repository.getChannel(id) }
        .flatMapLoaded { response ->
            when (response) {
                is ChannelResponse.Content -> {
                    paginatedDataFlow(response.videoItemsPage).map { videoItems ->
                        ChannelBundle.Content(response.channel, videoItems)
                    }
                }

                ChannelResponse.Unavailable -> flowOf(ChannelBundle.Unavailable)
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
}
