package com.huellalive.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.huellalive.app.data.model.AnimalDto
import com.huellalive.app.data.model.ShelterProfileDto
import com.huellalive.app.ui.theme.DustyRose
import com.huellalive.app.ui.theme.Outline
import com.huellalive.app.ui.theme.SurfaceRaised
import com.huellalive.app.utils.versionedMediaUrl

@Composable
fun AnimalProfileCard(
    animal: AnimalDto,
    modifier: Modifier = Modifier,
    mediaRevision: Int = 0,
    onClick: () -> Unit
) {
    PhotoNameCard(
        name = animal.name,
        imageUrl = versionedMediaUrl(animal.photoUrl, mediaRevision),
        modifier = modifier,
        placeholderIcon = Icons.Default.Pets,
        onClick = onClick
    )
}

@Composable
fun ShelterProfileCard(
    shelter: ShelterProfileDto,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    PhotoNameCard(
        name = shelter.user.name,
        imageUrl = shelter.coverUrl ?: shelter.user.avatarUrl,
        modifier = modifier,
        placeholderIcon = Icons.Default.Home,
        avatarUrl = shelter.user.avatarUrl,
        onClick = onClick
    )
}

@Composable
private fun PhotoNameCard(
    name: String,
    imageUrl: String?,
    placeholderIcon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    avatarUrl: String? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceRaised)
            .clickable(onClick = onClick)
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    placeholderIcon,
                    contentDescription = null,
                    tint = DustyRose,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.52f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.9f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 9.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (avatarUrl != null) {
                Surface(
                    shape = CircleShape,
                    border = BorderStroke(2.dp, Color.White.copy(alpha = 0.85f)),
                    color = SurfaceRaised,
                    modifier = Modifier.size(28.dp)
                ) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = name,
                color = Color.White,
            fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Surface(
            color = Color.Transparent,
            border = BorderStroke(1.dp, Outline.copy(alpha = 0.9f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {}
    }
}
