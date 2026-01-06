package com.waytube.app.video.ui

import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.google.common.util.concurrent.ListenableFuture
import com.waytube.app.common.ui.UiState
import com.waytube.app.common.ui.UiStateLoader
import com.waytube.app.video.domain.Video
import com.waytube.app.video.domain.VideoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class VideoViewModel(
    savedStateHandle: SavedStateHandle,
    private val controllerFuture: ListenableFuture<MediaController>,
    private val repository: VideoRepository
) : ViewModel() {
    private val videoId = savedStateHandle.getMutableStateFlow<String?>(
        key = "video_id",
        initialValue = null
    )

    private val positionMs = savedStateHandle.getMutableStateFlow<Long?>(
        key = "position_ms",
        initialValue = null
    )

    private val skippedSegmentIds = savedStateHandle.getMutableStateFlow(
        key = "skipped_segment_ids",
        initialValue = emptyList<String>()
    )

    private val videoLoader = UiStateLoader()

    private val skipSegmentsLoader = UiStateLoader()

    private var isAutoplayRequested = false

    val videoState = videoId
        .flatMapLatest { id ->
            if (id != null) videoLoader.bind { repository.getVideo(id) } else flowOf(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = null
        )

    val isActive = videoId
        .map { it != null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = false
        )

    val player = flow<Player> { emit(controllerFuture.await()) }
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            replay = 1
        )

    val isPlaying = player
        .flatMapLatest { player ->
            player.eventsFlow(Player.EVENT_IS_PLAYING_CHANGED) { it.isPlaying }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = false
        )

    val skipSegments = videoState
        .map { ((it as? UiState.Data)?.data as? Video.Content.Regular)?.id }
        .distinctUntilChanged()
        .flatMapLatest { id ->
            if (id != null) {
                skipSegmentsLoader.bind { repository.getSkipSegments(id) }
            } else flowOf(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = null
        )

    init {
        combine(
            videoState
                .map { ((it as? UiState.Data)?.data as? Video.Content) }
                .distinctUntilChanged(),
            player
        ) { video, player -> video to player }
            .onEach { (video, player) ->
                if (video != null) {
                    val (uri, mimeType) = when (video) {
                        is Video.Content.Regular ->
                            video.dashManifestUrl to MimeTypes.APPLICATION_MPD

                        is Video.Content.Live ->
                            video.hlsPlaylistUrl to MimeTypes.APPLICATION_M3U8
                    }

                    val mediaMetadata = MediaMetadata.Builder()
                        .setTitle(video.title)
                        .setArtist(video.channelName)
                        .setArtworkUri(video.thumbnailUrl.toUri())
                        .build()

                    val mediaItem = MediaItem.Builder()
                        .setUri(uri)
                        .setMimeType(mimeType)
                        .setMediaMetadata(mediaMetadata)
                        .build()

                    player.apply {
                        positionMs.value?.let { positionMs ->
                            setMediaItem(mediaItem, positionMs)
                        } ?: setMediaItem(mediaItem)
                        prepare()
                        playWhenReady = isAutoplayRequested
                    }
                } else {
                    player.apply {
                        stop()
                        clearMediaItems()
                    }
                }
            }
            .launchIn(viewModelScope)

        combine(
            player,
            callbackFlow {
                val lifecycle = ProcessLifecycleOwner.get().lifecycle

                send(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED).not())

                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_START -> trySend(false)
                        Lifecycle.Event.ON_STOP -> trySend(true)
                        else -> {}
                    }
                }

                lifecycle.addObserver(observer)

                awaitClose { lifecycle.removeObserver(observer) }
            }
        ) { player, isAppInBackground -> player to isAppInBackground }
            .onEach { (player, isAppInBackground) ->
                player.trackSelectionParameters = player.trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, isAppInBackground)
                    .build()
            }
            .launchIn(viewModelScope)

        combine(
            videoState
                .map { (it as? UiState.Data)?.data is Video.Content.Regular }
                .distinctUntilChanged(),
            player
        ) { isRegularVideo, player -> isRegularVideo to player }
            .flatMapLatest { (isRegularVideo, player) ->
                if (isRegularVideo) {
                    val eventsPosition = player.eventsFlow(
                        Player.EVENT_IS_PLAYING_CHANGED,
                        Player.EVENT_POSITION_DISCONTINUITY
                    ) { it.currentPosition }

                    val pollingPosition = isPlaying.transformLatest { isPlaying ->
                        while (isPlaying) {
                            delay(1.seconds)
                            emit(player.currentPosition)
                        }
                    }

                    merge(eventsPosition, pollingPosition)
                } else emptyFlow()
            }
            .onEach { positionMs.value = it }
            .launchIn(viewModelScope)

        combine(
            player,
            positionMs.map { it?.milliseconds },
            skipSegments.map { (it as? UiState.Data)?.data }.distinctUntilChanged()
        ) { player, position, skipSegments -> Triple(player, position, skipSegments) }
            .onEach { (player, position, skipSegments) ->
                if (position != null) {
                    val segment = skipSegments?.find { (id, start, end) ->
                        position in start..end && skippedSegmentIds.value.contains(id).not()
                    }

                    if (segment != null) {
                        player.seekTo(segment.end.inWholeMilliseconds)
                        skippedSegmentIds.value += segment.id
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun play(id: String) {
        if (videoId.value != id) {
            videoId.value = id
            positionMs.value = null
            isAutoplayRequested = true
            skippedSegmentIds.value = emptyList()
        }
    }

    fun retry() {
        viewModelScope.launch { videoLoader.retry() }
    }

    fun stop() {
        videoId.value = null
    }

    override fun onCleared() {
        super.onCleared()
        MediaController.releaseFuture(controllerFuture)
    }
}

private fun <T> Player.eventsFlow(
    @Player.Event vararg triggerEvents: Int,
    block: (Player) -> T
): Flow<T> = callbackFlow {
    send(block(this@eventsFlow))

    val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.containsAny(*triggerEvents)) {
                trySend(block(player))
            }
        }
    }

    addListener(listener)

    awaitClose { removeListener(listener) }
}
