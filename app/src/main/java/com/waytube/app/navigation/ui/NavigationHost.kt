package com.waytube.app.navigation.ui

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.waytube.app.channel.ui.ChannelScreen
import com.waytube.app.search.ui.SearchScreen
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
private data object SearchRoute : NavKey

@Serializable
private data class ChannelRoute(val id: String) : NavKey

@Composable
fun NavigationHost() {
    val backStack = rememberNavBackStack(SearchRoute)

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
                        onNavigateToChannel = { id ->
                            backStack += ChannelRoute(id)
                        }
                    )
                }

                entry<ChannelRoute> { (id) ->
                    ChannelScreen(
                        viewModel = koinViewModel { parametersOf(id) }
                    )
                }
            }
        )
    }
}
