package com.waytube.app.common.ui.async

import com.waytube.app.common.domain.FetchError

sealed interface AsyncState<out T> {
    data object Loading : AsyncState<Nothing>

    data class Loaded<T>(
        val data: T,
        val isRefreshing: Boolean,
        val refresh: () -> Unit
    ) : AsyncState<T>

    data class Error(
        val error: FetchError,
        val retry: () -> Unit
    ) : AsyncState<Nothing>
}
