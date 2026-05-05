package com.waytube.app.playback.ui

import androidx.media3.common.Player
import androidx.media3.session.MediaController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.guava.await

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackManager(private val controllerBuilder: MediaController.Builder) {
    private val activeId = MutableStateFlow<String?>(null)

    fun requestPlayer(id: String): Flow<Player?> =
        activeId.flatMapLatest { activeId ->
            if (activeId == id) {
                flow {
                    val future = controllerBuilder.buildAsync()
                    try {
                        val controller = future.await()

                        try {
                            emit(controller)
                            awaitCancellation()
                        } finally {
                            controller.apply {
                                stop()
                                clearMediaItems()
                            }
                        }
                    } finally {
                        MediaController.releaseFuture(future)
                    }
                }
            } else {
                flowOf(null)
            }
        }

    fun setActiveId(id: String?) {
        activeId.value = id
    }
}
