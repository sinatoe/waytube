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

    private val refreshSignal = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val playbackRequestSignal = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)

    private val responseState = asyncStateFlow(refreshSignal) { repository.getVideo(id) }
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            replay = 1
        )

    private val skipSegmentsState = asyncStateFlow(
        emptyFlow<Nothing>()
    ) { repository.getSkipSegments(id) }
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            replay = 1
        )

    private val playbackModel = responseState
        .filterIsInstance<AsyncState.Loaded<VideoResponse.Content>>()
        .distinctUntilChanged()
        .flatMapLatest { state ->
            if (!state.isRefreshing) {
                playbackRequestSignal
                    .onStart { emit(false) }
                    .flatMapLatest { isRequested ->
                        if (isRequested) {
                            requestPlaybackModel(state.data.video)
                        } else {
                            flowOf(null)
                        }
                    }
            } else {
                flowOf(null)
            }
        }

    val model = responseState
        .flatMapLatestData { response ->
            when (response) {
                is VideoResponse.Content -> {
                    playbackModel.map { it ?: VideoModel.Overview(response.video) }
                }

                is VideoResponse.Unavailable -> {
                    flowOf(VideoModel.Unavailable(response.restriction))
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = AsyncState.Loading
        )

    fun handleIntent(intent: VideoIntent) {
        when (intent) {
            VideoIntent.Refresh -> refreshSignal.tryEmit(Unit)
            VideoIntent.StartPlayback -> playbackRequestSignal.tryEmit(true)
            VideoIntent.StopPlayback -> playbackRequestSignal.tryEmit(false)
        }
    }

    private fun requestPlaybackModel(video: Video): Flow<VideoModel.Playback?> =
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
                        player.playbackSessionStatusFlow(),
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
                    ) { status, skipSegmentsState ->
                        VideoModel.Playback(
                            video = video,
                            session = VideoPlaybackSession(
                                player = player,
                                status = status
                            ),
                            skipSegmentsState = skipSegmentsState
                        )
                    }
                } else {
                    flowOf(null)
                }
            }
}

private fun Player.asPlaybackSessionStatus(): VideoPlaybackSession.Status =
    when {
        playerError != null -> VideoPlaybackSession.Status.ERROR
        playbackState == Player.STATE_BUFFERING -> VideoPlaybackSession.Status.BUFFERING
        isPlaying -> VideoPlaybackSession.Status.PLAYING
        else -> VideoPlaybackSession.Status.PAUSED
    }

private fun Player.playbackSessionStatusFlow(): Flow<VideoPlaybackSession.Status> =
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
                    trySend(player.asPlaybackSessionStatus())
                }
            }
        }

        addListener(listener)

        trySend(asPlaybackSessionStatus())

        awaitClose {
            removeListener(listener)
        }
    }
