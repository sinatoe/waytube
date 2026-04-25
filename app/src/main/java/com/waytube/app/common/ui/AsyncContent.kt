package com.waytube.app.common.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.waytube.app.R
import com.waytube.app.common.domain.FetchError

@Composable
fun <T> AsyncContent(
    state: AsyncState<T>,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable (AsyncState.Loaded<T>) -> Unit
) {
    when (state) {
        is AsyncState.Loading -> {
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = Alignment.Center
            ) {
                StateMessage(
                    text = stringResource(
                        when (state.error) {
                            FetchError.AGE_RESTRICTED -> R.string.message_content_age_restricted
                            FetchError.CONTENT_UNAVAILABLE -> R.string.message_content_unavailable
                            FetchError.NETWORK -> R.string.message_content_network_error
                            FetchError.UNKNOWN -> R.string.message_content_load_error
                        }
                    ),
                    onRetry = state.retry
                )
            }
        }

        is AsyncState.Loaded -> {
            content(state)
        }
    }
}
