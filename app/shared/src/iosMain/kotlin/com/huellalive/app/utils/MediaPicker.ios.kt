package com.huellalive.app.utils

import androidx.compose.runtime.Composable

@Composable
actual fun rememberMediaPicker(
    mimeType: String,
    onPicked: (PickedMedia) -> Unit,
    onError: (String) -> Unit
): () -> Unit = {
    onError("Selector de archivos pendiente para iOS")
}
