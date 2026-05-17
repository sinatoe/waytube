package com.waytube.app.common.ui.async

import app.cash.turbine.test
import com.waytube.app.common.domain.FetchError
import com.waytube.app.common.domain.FetchResult
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AsyncStateFlowTest {
    @Test
    fun `test sequential fetch responses`() = runTest {
        val resultIterator = iterator {
            repeat(2) {
                yield(FetchResult.Failure(FetchError.UNKNOWN))
                yield(FetchResult.Success(Unit))
            }
        }

        val refreshSignal = MutableSharedFlow<Unit>(
            extraBufferCapacity = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST
        )

        val flow = asyncStateFlow(
            refreshSignal = refreshSignal,
            fetch = resultIterator::next
        )

        flow.test {
            assertEquals(AsyncState.Loading, awaitItem())

            assertEquals(AsyncState.Error(FetchError.UNKNOWN), awaitItem())

            refreshSignal.tryEmit(Unit)

            assertEquals(AsyncState.Loading, awaitItem())

            assertEquals(AsyncState.Loaded(data = Unit, isRefreshing = false), awaitItem())

            refreshSignal.tryEmit(Unit)

            assertEquals(AsyncState.Loaded(data = Unit, isRefreshing = true), awaitItem())

            assertEquals(AsyncState.Loaded(data = Unit, isRefreshing = false), awaitItem())

            refreshSignal.tryEmit(Unit)

            assertEquals(AsyncState.Loaded(data = Unit, isRefreshing = true), awaitItem())

            assertEquals(AsyncState.Loaded(data = Unit, isRefreshing = false), awaitItem())

            expectNoEvents()
        }
    }
}
