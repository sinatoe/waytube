package com.waytube.app.common.ui

import com.waytube.app.common.domain.FetchError
import com.waytube.app.common.domain.FetchResult
import com.waytube.app.common.domain.fold

sealed interface FetchEvent<out T> {
    data object Loading : FetchEvent<Nothing>

    data class Success<T>(val data: T) : FetchEvent<T>

    data class Failure(val error: FetchError) : FetchEvent<Nothing>

    companion object {
        fun <T> fromResult(result: FetchResult<T>): FetchEvent<T> =
            result.fold(
                onSuccess = ::Success,
                onFailure = ::Failure
            )
    }
}
