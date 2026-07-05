package com.huellalive.app.utils

import androidx.compose.runtime.Composable

@Composable
expect fun rememberShareAction(
    onUnavailable: (String) -> Unit = {}
): (String) -> Unit
