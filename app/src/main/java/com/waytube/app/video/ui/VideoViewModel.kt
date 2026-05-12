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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.transformWhile
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class VideoViewModel(
    private val id: String,
    savedStateHandle: SavedStateHandle,
    private val repository: VideoRepository,
    private val playbackManager: PlaybackManager
) : ViewModel() {
    private var savedPosition by savedStateHandle.saved<Duration?> { null }

    private val isPlaybackRequested = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)

    private val bundleRefreshSignal = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    val bundleState = asyncStateFlow(
        refreshSignal = bundleRefreshSignal,
        fetch = { repository.getVideo(id) }
    ) { response ->
        when (response) {
            is VideoResponse.Content -> {
                isPlaybackRequested
                    .onStart { emit(false) }
                    .distinctUntilChanged()
                    .flatMapLatest { isRequested ->
                        if (isRequested) {
                            requestPlaybackBundle(response.video)
                        } else {
                            flowOf(null)
                        }
                    }
                    .map { bundle ->
                        bundle ?: VideoBundle.Overview(response.video)
                    }
            }

            is VideoResponse.Unavailable -> {
                flowOf(VideoBundle.Unavailable(response.restriction))
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
            .transformLatest { player ->
                emit(player)
                while (player != null) {
                    delay(1.seconds)
                    savedPosition = player.currentPosition.milliseconds
                }
            }
            .onEach { player ->
                player?.apply {
                    val (uri, mimeType) = when (video) {
                        is Video.Regular -> video.dashManifestUrl to MimeTypes.APPLICATION_MPD
                        is Video.Live -> video.hlsPlaylistUrl to MimeTypes.APPLICATION_M3U8
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

                    setMediaItem(
                        mediaItem,
                        savedPosition?.inWholeMilliseconds ?: C.TIME_UNSET
                    )
                    prepare()
                    play()
                }
            }
            .flatMapLatest { player ->
                player?.videoPlaybackStateFlow()?.map { state ->
                    VideoBundle.Playback(
                        video = video,
                        player = player,
                        state = state
                    )
                } ?: flowOf(null)
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
