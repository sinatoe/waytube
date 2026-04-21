package com.waytube.app.common.ui

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AsyncStateTest {
    @Test
    fun `test sequential fetch responses`() = runTest {
        val resultIterator = iterator {
            repeat(2) {
                yield(Result.failure(Exception()))
                yield(Result.success(Unit))
            }
        }

        val flow = AsyncState.createFlow(resultIterator::next)

        flow.test {
            assertEquals(AsyncState.Loading, awaitItem())

            assertTrue(
                (awaitItem() as? AsyncState.Error)?.also { it.retry() } != null
            )

            assertEquals(AsyncState.Loading, awaitItem())

            assertTrue(
                ((awaitItem() as? AsyncState.Loaded)
                    ?.refreshState as? AsyncState.Loaded.RefreshState.Idle)
                    ?.also { it.refresh() } != null
            )

            assertTrue(
                (awaitItem() as? AsyncState.Loaded)
                    ?.refreshState is AsyncState.Loaded.RefreshState.Refreshing
            )

            assertTrue(
                ((awaitItem() as? AsyncState.Loaded)
                    ?.refreshState as? AsyncState.Loaded.RefreshState.Error)
                    ?.also { it.retry() } != null
            )

            assertTrue(
                (awaitItem() as? AsyncState.Loaded)
                    ?.refreshState is AsyncState.Loaded.RefreshState.Refreshing
            )

            assertTrue(
                (awaitItem() as? AsyncState.Loaded)
                    ?.refreshState is AsyncState.Loaded.RefreshState.Idle
            )

            expectNoEvents()
        }
    }
}
