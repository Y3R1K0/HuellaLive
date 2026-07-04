package com.huellalive.app.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberShareAction(
    onUnavailable: (String) -> Unit
): (String) -> Unit = remember(onUnavailable) {
    {
        onUnavailable("Compartir para iOS quedo preparado para conectar con la hoja nativa")
    }
}
