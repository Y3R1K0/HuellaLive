package com.huellalive.app.utils

import androidx.compose.runtime.Composable

data class PickedMedia(
    val bytes: ByteArray,
    val fileName: String,
    val contentType: String
)

@Composable
expect fun rememberMediaPicker(
    mimeType: String,
    onPicked: (PickedMedia) -> Unit,
    onError: (String) -> Unit
): () -> Unit
