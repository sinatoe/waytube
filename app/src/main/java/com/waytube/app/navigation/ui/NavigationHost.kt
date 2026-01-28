package com.waytube.app.navigation.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.waytube.app.channel.ui.ChannelScreen
import com.waytube.app.navigation.domain.DeepLinkResult
import com.waytube.app.playlist.ui.PlaylistScreen
import com.waytube.app.search.ui.SearchScreen
import com.waytube.app.video.ui.VideoScreen
import com.waytube.app.video.ui.VideoViewModel
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf


@Serializable
private data object SearchRoute : NavKey

private data object VideoRoute : NavKey

@Serializable
private data class ChannelRoute(val id: String) : NavKey

@Serializable
private data class PlaylistRoute(val id: String) : NavKey

@Composable
fun NavigationHost(
    viewModel: NavigationViewModel,
    videoViewModel: VideoViewModel,
    onSetVideoImmersiveMode: (Boolean) -> Unit,
    onKeepScreenAwake: (Boolean) -> Unit
) {
    val isVideoActive by videoViewModel.isActive.collectAsStateWithLifecycle()
    val isVideoPlaying by videoViewModel.isPlaying.collectAsStateWithLifecycle()

    val backStack = rememberNavBackStack(SearchRoute)

    LaunchedEffect(Unit) {
        viewModel.deepLinkResult.collect { result ->
            when (result) {
                is DeepLinkResult.Video -> {
                    videoViewModel.play(result.id)
                }

                is DeepLinkResult.Channel -> {
                    backStack += ChannelRoute(result.id)
                    videoViewModel.stop()
                }

                is DeepLinkResult.Playlist -> {
                    backStack += PlaylistRoute(result.id)
                    videoViewModel.stop()
                }
            }
        }
    }

    if (isVideoActive) {
        DisposableEffect(Unit) {
            onSetVideoImmersiveMode(true)

            onDispose { onSetVideoImmersiveMode(false) }
        }
    }

    if (isVideoActive && isVideoPlaying) {
        DisposableEffect(Unit) {
            onKeepScreenAwake(true)

            onDispose { onKeepScreenAwake(false) }
        }
    }

    Surface {
        NavDisplay(
            backStack = if (isVideoActive) backStack + VideoRoute else backStack,
            onBack = {
                if (isVideoActive) {
                    videoViewModel.stop()
                } else {
                    backStack.removeLastOrNull()
                }
            },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            transitionSpec = {
                slideInHorizontally { it } togetherWith slideOutHorizontally { -it / 2 }
            },
            popTransitionSpec = {
                slideInHorizontally { -it / 2 } togetherWith slideOutHorizontally { it }
            },
            predictivePopTransitionSpec = {
                slideInHorizontally { -it / 2 } togetherWith slideOutHorizontally { it }
            },
            entryProvider = entryProvider {
                entry<SearchRoute> {
                    SearchScreen(
                        viewModel = koinViewModel(),
                        onPlayVideo = videoViewModel::play,
                        onNavigateToChannel = { id ->
                            backStack += ChannelRoute(id)
                            videoViewModel.stop()
                        },
                        onNavigateToPlaylist = { id ->
                            backStack += PlaylistRoute(id)
                            videoViewModel.stop()
                        }
                    )
                }

                entry<VideoRoute>(
                    metadata = NavDisplay.transitionSpec {
                        slideInVertically { it } togetherWith
                                ExitTransition.KeepUntilTransitionsFinished
                    } + NavDisplay.popTransitionSpec {
                        EnterTransition.None togetherWith slideOutVertically { it }
                    } + NavDisplay.predictivePopTransitionSpec {
                        EnterTransition.None togetherWith slideOutVertically { it }
                    }
                ) {
                    VideoScreen(viewModel = videoViewModel)
                }

                entry<ChannelRoute> { (id) ->
                    ChannelScreen(
                        viewModel = koinViewModel { parametersOf(id) },
                        onPlayVideo = videoViewModel::play
                    )
                }

                entry<PlaylistRoute> { (id) ->
                    PlaylistScreen(
                        viewModel = koinViewModel { parametersOf(id) },
                        onPlayVideo = videoViewModel::play,
                        onNavigateToChannel = { id ->
                            backStack += ChannelRoute(id)
                            videoViewModel.stop()
                        }
                    )
                }
            }
        )
    }
}
