package com.waytube.app.channel.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.waytube.app.channel.domain.ChannelRepository
import com.waytube.app.common.ui.UiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ChannelViewModel(
    private val id: String,
    private val repository: ChannelRepository
) : ViewModel() {
    private val fetchTrigger = MutableSharedFlow<Unit>()

    val channelState = fetchTrigger
        .onStart { emit(Unit) }
        .transformLatest {
            emit(UiState.Loading)
            emit(
                repository.getChannel(id).fold(
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

    val videoItems = channelState
        .map { (it as? UiState.Data)?.data?.id }
        .flatMapLatest { id ->
            if (id != null) repository.getVideoItems(id) else flowOf(PagingData.empty())
        }
        .cachedIn(viewModelScope)

    fun retry() {
        viewModelScope.launch { fetchTrigger.emit(Unit) }
    }
}
