package com.waytube.app.navigation.ui

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
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

@Composable
fun NavigationHost(
    viewModel: NavigationViewModel,
    playbackManager: PlaybackManager = koinInject()
) {
    val backStack = rememberNavBackStack(SearchRoute)

    LaunchedEffect(Unit) {
        viewModel.deepLinkResult.collect { result ->
            backStack += when (result) {
                is DeepLinkResult.Video -> VideoRoute(result.id)
                is DeepLinkResult.Channel -> ChannelRoute(result.id)
                is DeepLinkResult.Playlist -> PlaylistRoute(result.id)
            }
        }
    }

    LaunchedEffect(backStack.last()) {
        playbackManager.setActiveId(
            (backStack.last() as? VideoRoute)?.id
        )
    }

    Surface {
        NavDisplay(
            backStack = backStack,
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
                        onNavigateToVideo = { id ->
                            backStack += VideoRoute(id)
                        },
                        onNavigateToChannel = { id ->
                            backStack += ChannelRoute(id)
                        },
                        onNavigateToPlaylist = { id ->
                            backStack += PlaylistRoute(id)
                        }
                    )
                }

                entry<VideoRoute> { (id) ->
                    VideoScreen(
                        viewModel = koinViewModel { parametersOf(id) }
                    )
                }

                entry<ChannelRoute> { (id) ->
                    ChannelScreen(
                        viewModel = koinViewModel { parametersOf(id) },
                        onNavigateToVideo = { id ->
                            backStack += VideoRoute(id)
                        }
                    )
                }

                entry<PlaylistRoute> { (id) ->
                    PlaylistScreen(
                        viewModel = koinViewModel { parametersOf(id) },
                        onNavigateToVideo = { id ->
                            backStack += VideoRoute(id)
                        },
                        onNavigateToChannel = { id ->
                            backStack += ChannelRoute(id)
                        }
                    )
                }
            }
        )
    }
}
