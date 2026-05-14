package com.waytube.app.navigation.ui

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.unveilIn
import androidx.compose.animation.veilOut
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent
import com.waytube.app.channel.ui.ChannelScreen
import com.waytube.app.navigation.domain.DeepLinkResult
import com.waytube.app.playback.ui.PlaybackManager
import com.waytube.app.playlist.ui.PlaylistScreen
import com.waytube.app.search.ui.SearchScreen
import com.waytube.app.video.ui.VideoScreen
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Serializable
private data object SearchRoute : NavKey

@Serializable
private data class VideoRoute(val id: String) : NavKey

@Serializable
private data class ChannelRoute(val id: String) : NavKey

@Serializable
private data class PlaylistRoute(val id: String) : NavKey

private val <T : NavKey> NavBackStack<T>.activeVideoId: String?
    get() = (last() as? VideoRoute)?.id

private fun <T : NavKey> NavBackStack<T>.push(element: T) {
    if (element != last()) {
        add(element)
    }
}

private fun <T : NavKey> NavBackStack<T>.pop(element: T) {
    if (element == last()) {
        removeLastOrNull()
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun NavigationHost(
    viewModel: NavigationViewModel,
    playbackManager: PlaybackManager = koinInject()
) {
    val backStack = rememberNavBackStack(SearchRoute)

    LaunchedEffect(Unit) {
        viewModel.deepLinkResult.collect { result ->
            backStack.push(
                when (result) {
                    is DeepLinkResult.Video -> VideoRoute(result.id)
                    is DeepLinkResult.Channel -> ChannelRoute(result.id)
                    is DeepLinkResult.Playlist -> PlaylistRoute(result.id)
                }
            )
        }
    }

    LaunchedEffect(backStack.activeVideoId) {
        playbackManager.setActiveId(backStack.activeVideoId)
    }

    Surface {
        NavDisplay(
            backStack = backStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            ),
            transitionSpec = {
                slideInHorizontally { it } togetherWith slideOutHorizontally { -it / 4 } + veilOut()
            },
            popTransitionSpec = {
                slideInHorizontally { -it / 4 } + unveilIn() togetherWith slideOutHorizontally { it }
            },
            predictivePopTransitionSpec = { edge ->
                unveilIn() togetherWith scaleOut(
                    targetScale = 0.5f,
                    transformOrigin = TransformOrigin(
                        pivotFractionX = if (edge == NavigationEvent.EDGE_RIGHT) 0.25f else 0.75f,
                        pivotFractionY = 0.5f
                    )
                ) + fadeOut()
            },
            entryProvider = entryProvider {
                entry<SearchRoute> {
                    SearchScreen(
                        viewModel = koinViewModel(),
                        onNavigateToVideo = { id ->
                            backStack.push(VideoRoute(id))
                        },
                        onNavigateToChannel = { id ->
                            backStack.push(ChannelRoute(id))
                        },
                        onNavigateToPlaylist = { id ->
                            backStack.push(PlaylistRoute(id))
                        }
                    )
                }

                entry<VideoRoute> { route ->
                    VideoScreen(
                        viewModel = koinViewModel { parametersOf(route.id) },
                        onNavigateBack = { backStack.pop(route) },
                        onNavigateToChannel = { id ->
                            backStack.push(ChannelRoute(id))
                        }
                    )
                }

                entry<ChannelRoute> { route ->
                    ChannelScreen(
                        viewModel = koinViewModel { parametersOf(route.id) },
                        onNavigateBack = { backStack.pop(route) },
                        onNavigateToVideo = { id ->
                            backStack.push(VideoRoute(id))
                        }
                    )
                }

                entry<PlaylistRoute> { route ->
                    PlaylistScreen(
                        viewModel = koinViewModel { parametersOf(route.id) },
                        onNavigateBack = { backStack.pop(route) },
                        onNavigateToVideo = { id ->
                            backStack.push(VideoRoute(id))
                        },
                        onNavigateToChannel = { id ->
                            backStack.push(ChannelRoute(id))
                        }
                    )
                }
            }
        )
    }
}
