package com.huellalive.app.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.huellalive.app.ui.location.GeoPoint
import com.huellalive.app.ui.theme.SurfaceRaised
import com.huellalive.app.ui.theme.TextSecondary

@Composable
actual fun ShelterLocationPicker(
    selectedPoint: GeoPoint?,
    modifier: Modifier,
    onPointChanged: (GeoPoint) -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize().background(SurfaceRaised),
        contentAlignment = Alignment.Center
    ) {
        Text("Selector de mapa listo para conectar con MapKit en iOS", color = TextSecondary)
    }
}
