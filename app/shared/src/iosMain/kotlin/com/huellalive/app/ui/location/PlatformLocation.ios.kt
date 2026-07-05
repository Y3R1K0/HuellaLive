package com.huellalive.app.ui.location

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@Composable
actual fun rememberCurrentLocationRequester(
    onLocation: (GeoPoint) -> Unit,
    onError: (String) -> Unit
): () -> Unit {
    val currentOnError = rememberUpdatedState(onError)
    return remember {
        { currentOnError.value("Ubicacion iOS pendiente de conectar con CoreLocation") }
    }
}
