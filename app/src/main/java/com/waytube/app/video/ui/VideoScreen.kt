package com.waytube.app.video.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.waytube.app.R
import com.waytube.app.common.ui.action.rememberNavigationBackAction
import com.waytube.app.common.ui.async.AsyncContent
import com.waytube.app.common.ui.async.AsyncState
import com.waytube.app.common.ui.element.BackButton
import com.waytube.app.common.ui.element.StateMessage
import com.waytube.app.video.domain.VideoResponse
import com.waytube.app.video.domain.VideoRestriction

@Composable
fun VideoScreen(viewModel: VideoViewModel) {
    VideoScreenContent(
        videoResponseState = viewModel.videoResponseState.collectAsStateWithLifecycle()::value
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoScreenContent(
    videoResponseState: () -> AsyncState<VideoResponse>
) {
    val topAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    BackButton(onClick = rememberNavigationBackAction())
                },
                title = {
                    Text(text = stringResource(R.string.label_video))
                },
                scrollBehavior = topAppBarScrollBehavior
            )
        }
    ) { contentPadding ->
        AsyncContent(
            state = videoResponseState(),
            contentPadding = contentPadding
        ) { (response) ->
            when (response) {
                is VideoResponse.Content -> {

                }

                is VideoResponse.Unavailable -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        StateMessage(
                            text = stringResource(
                                when (response.restriction) {
                                    VideoRestriction.AGE -> R.string.message_video_age_restricted
                                    VideoRestriction.MEMBERS_ONLY -> R.string.message_video_members_only
                                    VideoRestriction.PRIVATE -> R.string.message_video_private
                                    VideoRestriction.REGION -> R.string.message_video_region_blocked
                                    null -> R.string.message_video_unavailable
                                }
                            )
                        )
                    }
                }
            }
        }
    }
}
