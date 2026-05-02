package com.waytube.app.video.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import com.waytube.app.R
import com.waytube.app.common.ui.action.rememberNavigationBackAction
import com.waytube.app.common.ui.async.AsyncContent
import com.waytube.app.common.ui.element.BackButton
import com.waytube.app.common.ui.element.StateMessage
import com.waytube.app.common.ui.element.StyledImage
import com.waytube.app.video.domain.VideoRestriction

@Composable
fun VideoScreen(viewModel: VideoViewModel) {
    when (val scene = viewModel.scene.collectAsStateWithLifecycle().value) {
        is VideoScene.Preview -> {
            VideoPreviewSceneContent(scene = scene)
        }

        is VideoScene.Playback -> {
            VideoPlaybackSceneContent(scene = scene)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoPreviewSceneContent(scene: VideoScene.Preview) {
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
            state = scene.state,
            contentPadding = contentPadding
        ) { (preview) ->
            when (preview) {
                is VideoPreview.Content -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        StyledImage(
                            data = preview.video.thumbnailUrl,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9)
                                .clickable { preview.play() }
                        )
                    }
                }

                is VideoPreview.Unavailable -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(contentPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        StateMessage(
                            text = stringResource(
                                when (preview.restriction) {
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

@Composable
private fun VideoPlaybackSceneContent(scene: VideoScene.Playback) {
    BackHandler {
        scene.stop()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .displayCutoutPadding(),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    this.player = scene.player
                }
            },
            update = { view ->
                view.player = scene.player
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
