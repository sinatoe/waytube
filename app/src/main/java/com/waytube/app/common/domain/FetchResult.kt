package com.waytube.app.common.domain

sealed interface FetchResult<out T> {
    data class Success<T>(val data: T) : FetchResult<T>

    data class Failure(val error: FetchError) : FetchResult<Nothing>
}

inline fun <T, R> FetchResult<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (FetchError) -> R
): R = when (this) {
    is FetchResult.Success -> onSuccess(data)
    is FetchResult.Failure -> onFailure(error)
}

inline fun <T, R> FetchResult<T>.flatMap(transform: (T) -> FetchResult<R>): FetchResult<R> =
    when (this) {
        is FetchResult.Success -> transform(data)
        is FetchResult.Failure -> this
    }

inline fun <T, R> FetchResult<T>.map(transform: (T) -> R): FetchResult<R> =
    flatMap { FetchResult.Success(transform(it)) }
