package com.waytube.app.common.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.waytube.app.R
import com.waytube.app.common.domain.Identifiable

fun <T : Identifiable> LazyListScope.paginatedItems(
    data: PaginatedData<T>,
    itemContent: @Composable (T) -> Unit
) {
    items(
        items = data.items,
        key = { it.id }
    ) {
        itemContent(it)
    }

    when (val state = data.state) {
        is PaginatedData.State.HasMore -> {
            item {
                if (state is PaginatedData.State.Idle) {
                    LaunchedEffect(Unit) {
                        state.load()
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        is PaginatedData.State.Error -> {
            item {
                StateMessage(
                    text = stringResource(R.string.message_paging_load_error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    onRetry = state.retry
                )
            }
        }

        is PaginatedData.State.Done -> {
            if (data.items.isEmpty()) {
                item {
                    StateMessage(
                        text = stringResource(R.string.message_paging_empty),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    )
                }
            }
        }
    }
}
