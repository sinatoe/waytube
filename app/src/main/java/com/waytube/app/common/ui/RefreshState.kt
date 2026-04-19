package com.waytube.app.common.ui

sealed interface RefreshState {
    data object Loading : RefreshState

    data class Idle(val refresh: () -> Unit) : RefreshState

    data class Error(
        val exception: Throwable?,
        val retry: () -> Unit
    ) : RefreshState
}
