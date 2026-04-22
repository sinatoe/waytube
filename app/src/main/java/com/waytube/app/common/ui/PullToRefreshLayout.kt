package com.waytube.app.common.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun PullToRefreshLayout(
    refreshState: AsyncState.Loaded.RefreshState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing = refreshState is AsyncState.Loaded.RefreshState.Refreshing

    PullToRefreshBox(
        modifier = modifier,
        isRefreshing = isRefreshing,
        state = pullToRefreshState,
        onRefresh = {
            when (refreshState) {
                is AsyncState.Loaded.RefreshState.Idle -> refreshState.refresh()
                is AsyncState.Loaded.RefreshState.Error -> refreshState.retry()
                else -> {}
            }
        },
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(contentPadding)
            )
        },
    ) { content() }
}
