package com.huellalive.app.ui.location

import androidx.compose.runtime.Composable

data class GeoPoint(
    val latitude: Double,
    val longitude: Double
)

@Composable
expect fun rememberCurrentLocationRequester(
    onLocation: (GeoPoint) -> Unit,
    onError: (String) -> Unit
): () -> Unit
