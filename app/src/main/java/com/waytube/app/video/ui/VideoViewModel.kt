package com.waytube.app.video.ui

import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.serialization.saved
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import com.waytube.app.common.ui.async.AsyncState
import com.waytube.app.common.ui.async.asyncStateFlow
import com.waytube.app.common.ui.async.flatMapLatestData
import com.waytube.app.playback.ui.PlaybackManager
import com.waytube.app.video.domain.Video
import com.waytube.app.video.domain.VideoRepository
import com.waytube.app.video.domain.VideoResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.transformWhile
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class VideoViewModel(
    private val id: String,
    savedStateHandle: SavedStateHandle,
    private val repository: VideoRepository,
    private val playbackManager: PlaybackManager
) : ViewModel() {
    private var savedPosition by savedStateHandle.saved<Duration?> { null }
    private var skippedSegmentIds by savedStateHandle.saved { emptySet<String>() }

    private val isPlaybackRequested = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)

    private val bundleRefreshSignal = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val responseState = asyncStateFlow(bundleRefreshSignal) { repository.getVideo(id) }
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            replay = 1
        )

    private val skipSegmentsState = asyncStateFlow(emptyFlow()) { repository.getSkipSegments(id) }
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            replay = 1
        )

    private val playbackBundle = responseState
        .filterIsInstance<AsyncState.Loaded<VideoResponse.Content>>()
        .distinctUntilChanged()
        .flatMapLatest { state ->
            if (!state.isRefreshing) {
                isPlaybackRequested
                    .onStart { emit(false) }
                    .flatMapLatest { isRequested ->
                        if (isRequested) {
                            requestPlaybackBundle(state.data.video)
                        } else {
                            flowOf(null)
                        }
                    }
            } else {
                flowOf(null)
            }
        }

    val bundleState = responseState
        .flatMapLatestData { state ->
            when (state) {
                is VideoResponse.Content -> {
                    playbackBundle.map { it ?: VideoBundle.Overview(state.video) }
                }

                is VideoResponse.Unavailable -> {
                    flowOf(VideoBundle.Unavailable(state.restriction))
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = AsyncState.Loading
        )

    fun refreshBundle() {
        bundleRefreshSignal.tryEmit(Unit)
    }

    fun play() {
        isPlaybackRequested.tryEmit(true)
    }

    fun stop() {
        isPlaybackRequested.tryEmit(false)
    }

    private fun requestPlaybackBundle(video: Video): Flow<VideoBundle.Playback?> =
        playbackManager.requestPlayer(id)
            .transformWhile { player ->
                emit(player)
                player != null
            }
            .onEach { player ->
                player?.apply {
                    val mediaMetadata = MediaMetadata.Builder()
                        .setTitle(video.title)
                        .setArtist(video.channelName)
                        .setArtworkUri(video.thumbnailUrl.toUri())
                        .build()

                    val mediaItem = MediaItem.Builder()
                        .setUri(video.streamUrl)
                        .setMimeType(MimeTypes.APPLICATION_MPD)
                        .setMediaMetadata(mediaMetadata)
                        .build()

                    setMediaItem(
                        mediaItem,
                        savedPosition?.inWholeMilliseconds ?: C.TIME_UNSET
                    )
                    prepare()
                    play()
                }
            }
            .flatMapLatest { player ->
                if (player != null) {
                    combine(
                        player.videoPlaybackStateFlow(),
                        (if (video is Video.Regular) skipSegmentsState else flowOf(null))
                            .transformLatest { state ->
                                emit(state)

                                while (video is Video.Regular) {
                                    val position = player.currentPosition.milliseconds.also {
                                        savedPosition = it
                                    }

                                    val segments = (state as? AsyncState.Loaded)?.data

                                    segments
                                        ?.find { segment ->
                                            position in segment.start..segment.end
                                                    && !skippedSegmentIds.contains(segment.id)
                                        }
                                        ?.let { segment ->
                                            player.seekTo(segment.end.inWholeMilliseconds)
                                            skippedSegmentIds = skippedSegmentIds + segment.id
                                        }

                                    delay(500.milliseconds)
                                }
                            }
                    ) { state, skipSegmentsState ->
                        VideoBundle.Playback(
                            video = video,
                            player = player,
                            state = state,
                            skipSegmentsState = skipSegmentsState
                        )
                    }
                } else {
                    flowOf(null)
                }
            }
}

private fun Player.asVideoPlaybackState(): VideoPlaybackState =
    when {
        playerError != null -> VideoPlaybackState.ERROR
        playbackState == Player.STATE_BUFFERING -> VideoPlaybackState.BUFFERING
        isPlaying -> VideoPlaybackState.PLAYING
        else -> VideoPlaybackState.PAUSED
    }

private fun Player.videoPlaybackStateFlow(): Flow<VideoPlaybackState> =
    callbackFlow {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (
                    events.containsAny(
                        Player.EVENT_PLAYBACK_STATE_CHANGED,
                        Player.EVENT_PLAYER_ERROR,
                        Player.EVENT_IS_PLAYING_CHANGED
                    )
                ) {
                    trySend(player.asVideoPlaybackState())
                }
            }
        }

        addListener(listener)

        trySend(asVideoPlaybackState())

        awaitClose {
            removeListener(listener)
        }
    }
