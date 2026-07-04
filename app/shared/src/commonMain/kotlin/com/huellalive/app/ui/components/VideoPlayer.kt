package com.huellalive.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun VideoPlayer(
    videoUrl: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    onPlaybackState: (VideoPlaybackState) -> Unit = {}
)

enum class VideoPlaybackState {
    Idle,
    Buffering,
    Ready,
    Ended,
    Error
}
