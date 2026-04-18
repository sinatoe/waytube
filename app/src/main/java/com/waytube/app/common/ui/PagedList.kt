package com.waytube.app.common.ui

data class PagedList<T>(
    val items: List<T>,
    val state: State
) {
    sealed interface State {
        data class HasMore(val isLoading: Boolean) : State

        data class Error(val exception: Throwable) : State

        data object Done : State
    }
}
