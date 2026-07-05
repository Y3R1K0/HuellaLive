package com.huellalive.app.utils

import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable
actual fun rememberMediaPicker(
    mimeType: String,
    onPicked: (PickedMedia) -> Unit,
    onError: (String) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val resolver = context.contentResolver
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            try {
                val contentType = resolver.getType(uri) ?: mimeType.replace("*", "mp4")
                val fileName = resolver.query(uri, null, null, null, null)?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
                } ?: "huellalive-upload"
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("No se pudo leer el archivo")
                onPicked(PickedMedia(bytes, fileName, contentType))
            } catch (e: Exception) {
                onError(e.message ?: "No se pudo seleccionar el archivo")
            }
        }
    }
    return remember(launcher, mimeType) { { launcher.launch(mimeType) } }
}
