package com.waytube.app.common.ui.async

import app.cash.turbine.test
import com.waytube.app.common.domain.FetchError
import com.waytube.app.common.domain.FetchResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

        val flow = asyncStateFlow(resultIterator::next)

        flow.test {
            assertEquals(AsyncState.Loading, awaitItem())

            assertTrue(
                (awaitItem() as? AsyncState.Error)?.also { it.retry() } != null
            )

            assertEquals(AsyncState.Loading, awaitItem())

            assertTrue(
                (awaitItem() as? AsyncState.Loaded)?.also { it.refresh() }?.isRefreshing == false
            )

            assertTrue(
                (awaitItem() as? AsyncState.Loaded)?.isRefreshing == true
            )

            assertTrue(
                (awaitItem() as? AsyncState.Loaded)?.also { it.refresh() }?.isRefreshing == false
            )

            assertTrue(
                (awaitItem() as? AsyncState.Loaded)?.isRefreshing == true
            )

            assertTrue(
                (awaitItem() as? AsyncState.Loaded)?.isRefreshing == false
            )

            expectNoEvents()
        }
    }
}
