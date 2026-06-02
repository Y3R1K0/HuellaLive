package com.huellalive.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
actual fun VideoPlayer(
    videoUrl: String,
    isPlaying: Boolean,
    modifier: Modifier
) {
    // iOS video player — implementar con AVPlayer en Fase 11
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surface), contentAlignment = Alignment.Center) {
        Text("▶", style = MaterialTheme.typography.displayLarge)
    }
}