package com.waytube.app.video.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import com.waytube.app.R
import com.waytube.app.common.ui.action.shareText
import com.waytube.app.common.ui.async.AsyncState
import com.waytube.app.common.ui.async.AsyncStateScaffold
import com.waytube.app.common.ui.element.StyledImage
import com.waytube.app.common.ui.formatting.toAbsoluteDateString
import com.waytube.app.common.ui.formatting.toCompactString
import com.waytube.app.common.ui.formatting.toPluralCount
import com.waytube.app.common.ui.menu.MenuAction
import com.waytube.app.common.ui.menu.MoreOptionsMenu
import com.waytube.app.common.ui.theming.AppTheme
import com.waytube.app.video.domain.Video
import com.waytube.app.video.domain.VideoRestriction
import kotlin.math.roundToInt

@Composable
fun VideoScreen(
    viewModel: VideoViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChannel: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    when (val bundleState = viewModel.bundleState.collectAsStateWithLifecycle().value) {
        is AsyncState.Loaded if (bundleState.data is VideoBundle.Playback) -> {
            val view = LocalView.current
            val activity = LocalActivity.current

            DisposableEffect(Unit) {
                val insetsController = activity?.let {
                    WindowCompat.getInsetsController(it.window, view)
                }

                insetsController?.apply {
                    hide(WindowInsetsCompat.Type.systemBars())
                    systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }

                onDispose {
                    insetsController?.show(WindowInsetsCompat.Type.systemBars())
                }
            }

            VideoPlaybackScreenContent(
                bundle = bundleState.data,
                onStop = viewModel::stop
            )
        }

        else -> {
            VideoOverviewScreenContent(
                bundleState = bundleState,
                scrollState = scrollState,
                onRefreshBundle = viewModel::refreshBundle,
                onPlay = viewModel::play,
                onShare = LocalContext.current::shareText,
                onNavigateBack = onNavigateBack,
                onNavigateToChannel = onNavigateToChannel
            )
        }
    }
}

@Composable
private fun VideoPlaybackScreenContent(
    bundle: VideoBundle.Playback,
    onStop: () -> Unit
) {
    BackHandler {
        onStop()
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
                    this.player = bundle.player
                }
            },
            update = { view ->
                view.player = bundle.player
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun VideoOverviewScreenContent(
    bundleState: AsyncState<VideoBundle>,
    scrollState: ScrollState,
    onRefreshBundle: () -> Unit,
    onPlay: () -> Unit,
    onShare: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToChannel: (String) -> Unit
) {
    AsyncStateScaffold(
        state = bundleState,
        onRefresh = onRefreshBundle,
        title = stringResource(R.string.label_video),
        onNavigateBack = onNavigateBack,
        actions = { bundle ->
            when (bundle) {
                is VideoBundle.Overview -> {
                    MoreOptionsMenu(
                        actions = listOf(
                            MenuAction(
                                label = stringResource(R.string.label_share),
                                iconPainter = painterResource(R.drawable.ic_share),
                                onClick = { onShare(bundle.video.url) }
                            ),
                            MenuAction(
                                label = stringResource(R.string.label_go_to_channel),
                                iconPainter = painterResource(R.drawable.ic_person),
                                onClick = { onNavigateToChannel(bundle.video.channelId) }
                            )
                        )
                    )
                }

                is VideoBundle.Unavailable, is VideoBundle.Playback -> {}
            }
        }
    ) { bundle, contentPadding ->
        when (bundle) {
            is VideoBundle.Overview -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(contentPadding)
                ) {
                    AppTheme(darkTheme = true) {
                        Surface(onClick = onPlay) {
                            Box(contentAlignment = Alignment.Center) {
                                StyledImage(
                                    data = bundle.video.thumbnailUrl,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(16f / 9)
                                )

                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(
                                            MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_play_arrow),
                                        contentDescription = stringResource(R.string.cd_play),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = bundle.video.title,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleLarge
                            )

                            Text(
                                text = listOf(
                                    bundle.video.channelName,
                                    when (bundle.video) {
                                        is Video.Regular -> {
                                            bundle.video.uploadedAt.toAbsoluteDateString()
                                        }

                                        is Video.Live -> {
                                            stringResource(R.string.label_live)
                                        }
                                    }
                                )
                                    .joinToString(stringResource(R.string.separator_bullet)),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                listOfNotNull(
                                    when (bundle.video) {
                                        is Video.Regular -> {
                                            pluralStringResource(
                                                R.plurals.view_count,
                                                bundle.video.viewCount.toPluralCount(),
                                                bundle.video.viewCount.toCompactString()
                                            )
                                        }

                                        is Video.Live -> {
                                            pluralStringResource(
                                                R.plurals.watching_count,
                                                bundle.video.watchingCount.toPluralCount(),
                                                bundle.video.watchingCount.toCompactString()
                                            )
                                        }
                                    },
                                    bundle.video.approvalRatio?.let { ratio ->
                                        stringResource(
                                            R.string.label_approval_percentage,
                                            (ratio * 100).roundToInt()
                                        )
                                    }
                                )
                                    .joinToString(stringResource(R.string.separator_bullet)),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = AnnotatedString.fromHtml(bundle.video.descriptionHtml),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            is VideoBundle.Unavailable -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(
                            when (bundle.restriction) {
                                VideoRestriction.AGE -> R.string.message_video_age_restricted
                                VideoRestriction.MEMBERS_ONLY -> R.string.message_video_members_only
                                VideoRestriction.PRIVATE -> R.string.message_video_private
                                VideoRestriction.REGION -> R.string.message_video_region_blocked
                                null -> R.string.message_video_unavailable
                            }
                        ),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            is VideoBundle.Playback -> {}
        }
    }
}
