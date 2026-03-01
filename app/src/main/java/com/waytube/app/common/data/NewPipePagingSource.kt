package com.waytube.app.common.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.waytube.app.common.domain.Identifiable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.InfoItem
import org.schabi.newpipe.extractor.ListExtractor
import org.schabi.newpipe.extractor.Page
import java.util.concurrent.ConcurrentHashMap

class NewPipePagingSource<T : InfoItem, R : Identifiable>(
    private val extractor: ListExtractor<T>,
    private val transform: (T) -> R?,
    private val onLoadError: ((Throwable) -> LoadResult<Page, R>?)? = null
) : PagingSource<Page, R>() {
    private val deduplicationSet = ConcurrentHashMap.newKeySet<String>()

    override suspend fun load(params: LoadParams<Page>): LoadResult<Page, R> =
        try {
            val page = withContext(Dispatchers.IO) {
                params.key?.let(extractor::getPage) ?: run {
                    extractor.fetchPage()
                    extractor.initialPage
                }
            }

            val data = page.items
                .mapNotNull(transform)
                .filter { deduplicationSet.add(it.id) }

            LoadResult.Page(
                data = data,
                prevKey = null,
                nextKey = page.nextPage
            )
        } catch (e: Throwable) {
            onLoadError?.invoke(e) ?: LoadResult.Error(e)
        }

    override fun getRefreshKey(state: PagingState<Page, R>): Page? = null

    companion object {
        fun <T : InfoItem, R : Identifiable> createFlow(
            extractorFactory: () -> ListExtractor<T>,
            transform: (T) -> R?,
            onLoadError: ((Throwable) -> LoadResult<Page, R>?)? = null
        ): Flow<PagingData<R>> {
            val pager = Pager(
                config = PagingConfig(pageSize = 20, enablePlaceholders = false),
                pagingSourceFactory = {
                    NewPipePagingSource(
                        extractor = extractorFactory(),
                        transform = transform,
                        onLoadError = onLoadError
                    )
                }
            )

            return pager.flow
        }
    }
}
