package com.waytube.app.common.ui.pagination

import app.cash.turbine.test
import com.waytube.app.common.domain.FetchError
import com.waytube.app.common.domain.FetchResult
import com.waytube.app.common.domain.Page
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class PaginatedDataFlowTest {
    @Test
    fun `test sequential paginated fetch responses`() = runTest {
        val nextResultIterator = iterator {
            yield(FetchResult.Failure(FetchError.UNKNOWN))
            yield(FetchResult.Success(Page(items = listOf(Unit), next = null)))
        }

        val resultIterator = iterator {
            yield(FetchResult.Failure(FetchError.UNKNOWN))
            yield(
                FetchResult.Success(
                    Page(
                        items = listOf(Unit),
                        next = nextResultIterator::next
                    )
                )
            )
        }

        val flow = paginatedDataFlow(resultIterator::next)

        flow.test {
            awaitItem().let { data ->
                assertTrue(data.items.isEmpty())
                assertTrue(
                    (data.state as? PaginatedData.State.HasMore.Idle)
                        ?.also { it.load() } != null
                )
            }

            assertTrue(awaitItem().state is PaginatedData.State.HasMore.Loading)

            assertTrue(
                (awaitItem().state as? PaginatedData.State.Error)
                    ?.also { it.retry() } != null
            )

            assertTrue(awaitItem().state is PaginatedData.State.HasMore.Loading)

            awaitItem().let { data ->
                assertTrue(data.items.size == 1)
                assertTrue(
                    (data.state as? PaginatedData.State.HasMore.Idle)
                        ?.also { it.load() } != null
                )
            }

            assertTrue(awaitItem().state is PaginatedData.State.HasMore.Loading)

            assertTrue(
                (awaitItem().state as? PaginatedData.State.Error)
                    ?.also { it.retry() } != null
            )

            assertTrue(awaitItem().state is PaginatedData.State.HasMore.Loading)

            awaitItem().let { data ->
                assertTrue(data.items.size == 2)
                assertTrue(data.state is PaginatedData.State.Done)
            }

            expectNoEvents()
        }
    }
}
