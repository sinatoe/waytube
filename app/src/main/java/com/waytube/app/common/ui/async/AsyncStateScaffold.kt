package com.waytube.app.common.ui.async

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.waytube.app.R
import com.waytube.app.common.domain.FetchError
import com.waytube.app.common.ui.element.BackButton
import com.waytube.app.common.ui.element.RetryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> AsyncStateScaffold(
    state: AsyncState<T>,
    title: String,
    onNavigateBack: () -> Unit,
    actions: @Composable (T) -> Unit = {},
    content: @Composable (T, PaddingValues) -> Unit
) {
    val topAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                scrollBehavior = topAppBarScrollBehavior,
                navigationIcon = {
                    BackButton(onClick = onNavigateBack)
                },
                title = {
                    Text(text = title)
                },
                actions = {
                    (state as? AsyncState.Loaded)?.let { (data) ->
                        actions(data)
                    }
                }
            )
        }
    ) { contentPadding ->
        when (state) {
            AsyncState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is AsyncState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(
                            when (state.error) {
                                FetchError.NETWORK -> R.string.message_content_network_error
                                FetchError.IP_ADDRESS_BLOCKED ->
                                    R.string.message_content_ip_address_blocked

                                FetchError.UNKNOWN -> R.string.message_content_load_error
                            }
                        ),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium
                    )

                    RetryButton(onClick = state.retry)
                }
            }

            is AsyncState.Loaded -> {
                val pullToRefreshState = rememberPullToRefreshState()

                PullToRefreshBox(
                    state = pullToRefreshState,
                    isRefreshing = state.isRefreshing,
                    onRefresh = state.refresh,
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = pullToRefreshState,
                            isRefreshing = state.isRefreshing,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(contentPadding)
                        )
                    }
                ) {
                    content(state.data, contentPadding)
                }
            }
        }
    }
}
