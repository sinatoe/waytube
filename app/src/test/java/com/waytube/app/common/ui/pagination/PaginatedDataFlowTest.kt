package com.waytube.app.common.ui.pagination

import app.cash.turbine.test
import com.waytube.app.common.domain.FetchError
import com.waytube.app.common.domain.FetchResult
import com.waytube.app.common.domain.Page
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PaginatedDataFlowTest {
    @Test
    fun `test sequential paginated fetch responses`() = runTest {
        val nextResultIterator = iterator {
            yield(FetchResult.Failure(FetchError.UNKNOWN))
            yield(FetchResult.Success(Page(items = listOf(2), next = null)))
        }

        val resultIterator = iterator {
            yield(FetchResult.Failure(FetchError.UNKNOWN))
            yield(
                FetchResult.Success(
                    Page(
                        items = listOf(1),
                        next = nextResultIterator::next
                    )
                )
            )
        }

        val loadSignal = MutableSharedFlow<Unit>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

        val flow = paginatedDataFlow(
            loadSignal = loadSignal,
            fetch = resultIterator::next
        )

        flow.test {
            assertEquals(
                PaginatedData(items = emptyList<Int>(), state = PaginatedData.State.Idle),
                awaitItem()
            )

            loadSignal.tryEmit(Unit)

            assertEquals(
                PaginatedData(items = emptyList<Int>(), state = PaginatedData.State.Loading),
                awaitItem()
            )

            assertEquals(
                PaginatedData(
                    items = emptyList<Int>(),
                    state = PaginatedData.State.Error(FetchError.UNKNOWN)
                ),
                awaitItem()
            )

            loadSignal.tryEmit(Unit)

            assertEquals(
                PaginatedData(items = emptyList<Int>(), state = PaginatedData.State.Loading),
                awaitItem()
            )

            assertEquals(
                PaginatedData(items = listOf(1), state = PaginatedData.State.Idle),
                awaitItem()
            )

            loadSignal.tryEmit(Unit)

            assertEquals(
                PaginatedData(items = listOf(1), state = PaginatedData.State.Loading),
                awaitItem()
            )

            assertEquals(
                PaginatedData(
                    items = listOf(1),
                    state = PaginatedData.State.Error(FetchError.UNKNOWN)
                ),
                awaitItem()
            )

            loadSignal.tryEmit(Unit)

            assertEquals(
                PaginatedData(items = listOf(1), state = PaginatedData.State.Loading),
                awaitItem()
            )

            assertEquals(
                PaginatedData(items = listOf(1, 2), state = PaginatedData.State.Done),
                awaitItem()
            )

            awaitComplete()
        }
    }
}
