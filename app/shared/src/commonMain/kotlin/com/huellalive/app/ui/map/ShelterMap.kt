package com.huellalive.app.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.huellalive.app.data.model.ShelterProfileDto

@Composable
expect fun ShelterMap(
    shelters: List<ShelterProfileDto>,
    modifier: Modifier = Modifier,
    onShelterClick: (ShelterProfileDto) -> Unit
)
