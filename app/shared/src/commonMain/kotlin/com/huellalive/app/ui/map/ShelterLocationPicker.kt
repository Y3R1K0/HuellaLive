package com.huellalive.app.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.huellalive.app.ui.location.GeoPoint

@Composable
expect fun ShelterLocationPicker(
    selectedPoint: GeoPoint?,
    modifier: Modifier = Modifier,
    onPointChanged: (GeoPoint) -> Unit
)
