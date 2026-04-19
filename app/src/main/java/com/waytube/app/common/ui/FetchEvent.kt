package com.waytube.app.common.ui

sealed interface FetchEvent<out T> {
    data object Loading : FetchEvent<Nothing>

    data class Success<T>(val data: T) : FetchEvent<T>

    data class Failure(val exception: Throwable) : FetchEvent<Nothing>

    companion object {
        fun <T> fromResult(result: Result<T>): FetchEvent<T> =
            result.fold(
                onSuccess = ::Success,
                onFailure = ::Failure
            )
    }
}
