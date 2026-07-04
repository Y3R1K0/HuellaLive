package com.huellalive.app.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import com.huellalive.app.data.model.ShelterProfileDto
import com.huellalive.app.ui.theme.DustyRose
import com.huellalive.app.ui.theme.SurfaceRaised

@Composable
actual fun ShelterMap(
    shelters: List<ShelterProfileDto>,
    modifier: Modifier,
    onShelterClick: (ShelterProfileDto) -> Unit
) {
    Box(modifier.background(SurfaceRaised), contentAlignment = Alignment.Center) {
        if (shelters.isEmpty()) {
            Text("No hay albergues con ubicación", color = Color.White)
        } else {
            Icon(Icons.Default.LocationOn, null, tint = DustyRose, modifier = Modifier.size(48.dp))
        }
    }
}
