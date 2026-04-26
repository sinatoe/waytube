package com.waytube.app.common.data

import com.waytube.app.common.domain.FetchError
import com.waytube.app.common.domain.FetchResult
import com.waytube.app.common.domain.Identifiable
import com.waytube.app.common.domain.Page
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.exceptions.AgeRestrictedContentException
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

suspend fun <T> fetch(block: () -> T): FetchResult<T> =
    withContext(Dispatchers.IO) {
        try {
            FetchResult.Success(block())
        } catch (e: Throwable) {
            FetchResult.Failure(
                when (e) {
                    is AgeRestrictedContentException -> FetchError.AGE_RESTRICTED
                    is ContentNotAvailableException -> FetchError.CONTENT_UNAVAILABLE
                    is IOException -> FetchError.NETWORK
                    else -> FetchError.UNKNOWN
                }
            )
        }
    }

suspend fun <T : InfoItem, R : Identifiable> ListExtractor<T>.paginate(
    treatErrorAsEmpty: (Throwable) -> Boolean = { false },
    transform: (T) -> R?
): FetchResult<Page<R>> {
    val deduplicationSet = ConcurrentHashMap.newKeySet<String>()

    suspend fun load(block: () -> ListExtractor.InfoItemsPage<T>): FetchResult<Page<R>> =
        fetch {
            try {
                val extractorPage = block()

                Page(
                    items = extractorPage.items
                        .mapNotNull(transform)
                        .filter { deduplicationSet.add(it.id) },
                    next = extractorPage.nextPage?.let {
                        { load { getPage(it) } }
                    }
                )
            } catch (e: Throwable) {
                if (treatErrorAsEmpty(e)) {
                    Page(
                        items = emptyList(),
                        next = null
                    )
                } else throw e
            }
        }

    return load {
        fetchPage()
        initialPage
    }
}
