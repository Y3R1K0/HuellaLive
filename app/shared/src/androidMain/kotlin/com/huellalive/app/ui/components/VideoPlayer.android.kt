package com.huellalive.app.ui.components

import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

private object ActiveVideoPlayer {
    private var current: ExoPlayer? = null

    fun play(player: ExoPlayer) {
        if (current !== player) {
            current?.playWhenReady = false
            current?.pause()
        }
        current = player
        player.playbackParameters = PlaybackParameters(1f, 1f)
        player.playWhenReady = true
        player.play()
    }

    fun pause(player: ExoPlayer) {
        if (current === player) current = null
        player.playWhenReady = false
        player.pause()
    }

    fun release(player: ExoPlayer) {
        if (current === player) current = null
    }
}

@Composable
actual fun VideoPlayer(
    videoUrl: String,
    isPlaying: Boolean,
    modifier: Modifier,
    onPlaybackState: (VideoPlaybackState) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val exoPlayer = remember(videoUrl) {
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                2_500,
                15_000,
                1_000,
                1_500
            )
            .build()

        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .build()
            .apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
            repeatMode = Player.REPEAT_MODE_ONE
            playWhenReady = false
            skipSilenceEnabled = false
            playbackParameters = PlaybackParameters(1f, 1f)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                    .build(),
                true
            )
            prepare()
        }
    }

    DisposableEffect(exoPlayer, onPlaybackState) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                onPlaybackState(
                    when (playbackState) {
                        Player.STATE_BUFFERING -> VideoPlaybackState.Buffering
                        Player.STATE_READY -> VideoPlaybackState.Ready
                        Player.STATE_ENDED -> VideoPlaybackState.Ended
                        else -> VideoPlaybackState.Idle
                    }
                )
            }

            override fun onPlayerError(error: PlaybackException) {
                onPlaybackState(VideoPlaybackState.Error)
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    LaunchedEffect(exoPlayer, isPlaying) {
        exoPlayer.playbackParameters = PlaybackParameters(1f, 1f)
        exoPlayer.setPlaybackSpeed(1f)
        if (isPlaying) {
            ActiveVideoPlayer.play(exoPlayer)
        } else {
            ActiveVideoPlayer.pause(exoPlayer)
        }
    }

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    ActiveVideoPlayer.pause(exoPlayer)
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (isPlaying) {
                        ActiveVideoPlayer.play(exoPlayer)
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            ActiveVideoPlayer.pause(exoPlayer)
            ActiveVideoPlayer.release(exoPlayer)
            exoPlayer.clearVideoSurface()
            exoPlayer.clearMediaItems()
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
            }
        },
        update = { view -> view.player = exoPlayer },
        onRelease = { view -> view.player = null },
        modifier = modifier
    )
}
