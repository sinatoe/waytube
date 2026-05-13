package com.waytube.app.playback.ui

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.guava.await

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackManager(private val controllerBuilder: MediaController.Builder) {
    private val activeId = MutableStateFlow<String?>(null)

    private val isAppInBackgroundFlow = callbackFlow {
        val lifecycle = ProcessLifecycleOwner.get().lifecycle

        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                trySend(false)
            }

            override fun onStop(owner: LifecycleOwner) {
                trySend(true)
            }
        }

        lifecycle.addObserver(observer)

        trySend(!lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))

        awaitClose {
            lifecycle.removeObserver(observer)
        }
    }

    private val playerFlow = callbackFlow {
        val future = controllerBuilder.buildAsync()
        val controller = future.await()

        trySend(controller)

        awaitClose {
            controller.apply {
                stop()
                clearMediaItems()
            }
            MediaController.releaseFuture(future)
        }
    }
        .transformLatest { player ->
            emit(player)

            isAppInBackgroundFlow.collect { isBackground ->
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, isBackground)
                    .build()
            }
        }

    fun requestPlayer(id: String): Flow<Player?> =
        activeId.flatMapLatest { activeId ->
            if (activeId == id) playerFlow else flowOf(null)
        }

    fun setActiveId(id: String?) {
        activeId.value = id
    }
}
