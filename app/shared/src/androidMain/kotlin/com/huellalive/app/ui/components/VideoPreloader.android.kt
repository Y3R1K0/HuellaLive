package com.huellalive.app.ui.components

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer

@Composable
actual fun PreloadVideo(videoUrl: String?) {
    val context = LocalContext.current
    DisposableEffect(videoUrl) {
        if (videoUrl.isNullOrBlank()) {
            onDispose {}
        } else {
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(1_500, 5_000, 750, 1_000)
                .build()
            val player = ExoPlayer.Builder(context)
                .setLoadControl(loadControl)
                .build()
                .apply {
                    playWhenReady = false
                    setMediaItem(MediaItem.fromUri(Uri.parse(videoUrl)))
                    prepare()
                }
            onDispose {
                player.stop()
                player.clearMediaItems()
                player.release()
            }
        }
    }
}
