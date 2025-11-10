package com.waytube.app.common.ui

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>

    data class Error(val exception: Throwable) : UiState<Nothing>

    data class Data<T>(val data: T) : UiState<T>
}
