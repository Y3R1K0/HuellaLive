package com.huellalive.app.utils

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberShareAction(
    onUnavailable: (String) -> Unit
): (String) -> Unit {
    val context = LocalContext.current
    return remember(context, onUnavailable) {
        { text ->
            runCatching {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(intent, "Compartir en"))
            }.onFailure {
                onUnavailable("No se pudo abrir el menu para compartir")
            }
        }
    }
}
