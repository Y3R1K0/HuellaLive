package com.huellalive.app.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.huellalive.app.data.model.VideoDto
import com.huellalive.app.ui.components.VideoPlayer
import com.huellalive.app.ui.theme.*

@Composable
fun VideoFeedItem(
    video: VideoDto,
    isActive: Boolean,
    onAnimalClick: (String) -> Unit = {}
) {
    var isLiked by remember { mutableStateOf(false) }
    var likeCount by remember { mutableIntStateOf(video.likesCount) }

    Box(modifier = Modifier.fillMaxSize()) {

        // Video player
        VideoPlayer(
            videoUrl = video.videoUrl,
            isPlaying = isActive,
            modifier = Modifier.fillMaxSize()
        )

        // Gradient overlay bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(350.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
        )

        // Info izquierda
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 100.dp, end = 72.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(DustyRose),
                    contentAlignment = Alignment.Center
                ) {
                    if (video.user.avatarUrl != null) {
                        AsyncImage(model = video.user.avatarUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
                    } else {
                        Text(video.user.name.first().toString().uppercase(), color = TextOnAccent, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text("@${video.user.name}", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }

            Spacer(Modifier.height(8.dp))
            Text(video.description, color = Color.White, fontSize = 13.sp, maxLines = 2)

            if (video.animal != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .clickable { onAnimalClick(video.animal.id) }
                        .background(DustyRose.copy(alpha = 0.85f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Pets, null, tint = Color.White, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${video.animal.name} · ${video.animal.species}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Acciones derecha
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 110.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = {
                    isLiked = !isLiked
                    likeCount += if (isLiked) 1 else -1
                }) {
                    Icon(
                        if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        null,
                        tint = if (isLiked) Color(0xFFFF8FA3) else Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Text(formatCount(likeCount), color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

private fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> "${count / 1_000_000}M"
    count >= 1_000 -> "${count / 1_000}K"
    else -> count.toString()
}
