package com.huellalive.app.ui.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.huellalive.app.data.model.VideoDto
import com.huellalive.app.ui.components.VideoPlayer
import com.huellalive.app.ui.components.VideoPlaybackState
import com.huellalive.app.ui.theme.DustyRose
import com.huellalive.app.ui.theme.TextOnAccent
import com.huellalive.app.utils.cloudinaryVideoPosterUrl
import com.huellalive.app.utils.rememberShareAction
import com.huellalive.app.utils.versionedMediaUrl
import kotlinx.coroutines.delay

@Composable
fun VideoFeedItem(
    video: VideoDto,
    isActive: Boolean,
    animalMediaRevision: Int = 0,
    onLikeClick: (VideoDto) -> Unit = {},
    onReportClick: (VideoDto, String) -> Unit = { _, _ -> },
    onReportProfileClick: (VideoDto, String) -> Unit = { _, _ -> },
    onAnimalClick: (String) -> Unit = {}
) {
    var shareMessage by remember { mutableStateOf<String?>(null) }
    var isManuallyPaused by remember(video.id) { mutableStateOf(false) }
    var playbackState by remember(video.id) { mutableStateOf(VideoPlaybackState.Idle) }
    var showReportDialog by remember(video.id) { mutableStateOf(false) }
    val author = video.author
    val animal = video.animal
    val isShelterStory = video.isShelterStory
    val storyShelter = video.shelter ?: animal?.shelter
    val displayName = if (isShelterStory) {
        storyShelter?.user?.name ?: author.name
    } else {
        animal?.name ?: "Animal"
    }
    val animalName = animal?.name ?: displayName
    val shelterName = storyShelter?.user?.name ?: author.name
    val avatarUrl = if (isShelterStory) storyShelter?.user?.avatarUrl ?: author.avatarUrl else animal?.photoUrl
    val share = rememberShareAction { shareMessage = it }
    val shouldPlayVideo = isActive && !isManuallyPaused
    val videoTapInteraction = remember { MutableInteractionSource() }
    val posterUrl = video.thumbnailUrl ?: cloudinaryVideoPosterUrl(video.videoUrl)

    LaunchedEffect(shareMessage) {
        if (shareMessage != null) {
            delay(2200)
            shareMessage = null
        }
    }

    LaunchedEffect(isActive, video.id) {
        if (!isActive) playbackState = VideoPlaybackState.Idle
    }

    if (showReportDialog) {
        ReportVideoDialog(
            animalName = displayName,
            onDismiss = { showReportDialog = false },
            onReportVideo = { reason ->
                showReportDialog = false
                onReportClick(video, reason)
            },
            onReportProfile = { reason ->
                showReportDialog = false
                onReportProfileClick(video, reason)
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            if (posterUrl != null) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (isActive) {
            VideoPlayer(
                videoUrl = stableCloudinaryVideoUrl(video.videoUrl),
                isPlaying = shouldPlayVideo,
                modifier = Modifier.fillMaxSize(),
                onPlaybackState = { playbackState = it }
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = videoTapInteraction,
                    indication = null
                ) {
                    if (isActive) {
                        isManuallyPaused = !isManuallyPaused
                    }
                }
        )

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

        AnimatedVisibility(
            visible = isActive && isManuallyPaused,
            enter = fadeIn() + scaleIn(initialScale = 0.72f),
            exit = fadeOut() + scaleOut(targetScale = 0.82f),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.58f),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.16f)
                ),
                modifier = Modifier.size(74.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Reanudar video",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isActive && !isManuallyPaused && playbackState == VideoPlaybackState.Buffering,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(color = Color.Black.copy(alpha = 0.45f), shape = CircleShape) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(18.dp).size(28.dp),
                    strokeWidth = 2.dp,
                    color = DustyRose
                )
            }
        }

        AnimatedVisibility(
            visible = isActive && playbackState == VideoPlaybackState.Error,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.70f),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
            ) {
                Text(
                    "No se pudo reproducir este video",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        AnimatedVisibility(
            visible = isActive,
            enter = fadeIn() + scaleIn(initialScale = 0.96f),
            exit = fadeOut() + scaleOut(targetScale = 0.98f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 118.dp, end = 72.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.clickable(enabled = animal != null && !isShelterStory) {
                        animal?.let { onAnimalClick(it.id) }
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DustyRose,
                        tonalElevation = 4.dp,
                        modifier = Modifier.size(48.dp)
                    ) {
                        if (avatarUrl != null) {
                            AsyncImage(
                                model = versionedMediaUrl(avatarUrl, animalMediaRevision),
                                contentDescription = displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    displayName.firstOrNull()?.toString()?.uppercase() ?: "H",
                                    color = TextOnAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            displayName,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (isShelterStory) "Historia de albergue" else "Albergue: $shelterName",
                            color = Color.White.copy(alpha = 0.78f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                if (video.description.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        video.description,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3
                    )
                }

                if (isShelterStory) {
                    Spacer(Modifier.height(10.dp))
                    FeedChip(
                        text = "Historia de albergue",
                        background = DustyRose.copy(alpha = 0.92f),
                        content = TextOnAccent
                    )
                } else if (animal != null) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FeedChip(
                            text = "${animal.species}${animal.breed?.let { " - $it" } ?: ""}",
                            background = Color.White.copy(alpha = 0.14f),
                            content = Color.White
                        )
                        val (statusText, statusColor) = adoptionStatus(animal.status)
                        FeedChip(
                            text = statusText,
                            background = statusColor.copy(alpha = 0.92f),
                            content = Color.White
                        )
                    }
                }

                AnimatedVisibility(
                    visible = shareMessage != null,
                    enter = slideInVertically { it / 2 } + fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        shareMessage.orEmpty(),
                        color = DustyRose,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val likeScale by animateFloatAsState(
                    targetValue = if (video.isLiked) 1.14f else 1f,
                    animationSpec = spring(dampingRatio = 0.48f, stiffness = 420f)
                )
                FeedActionButton(
                    onClick = { if (!isShelterStory) onLikeClick(video) },
                    containerColor = if (video.isLiked) DustyRose else Color.Black.copy(alpha = 0.44f)
                ) {
                    Icon(
                        if (video.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Me gusta",
                        tint = if (video.isLiked) TextOnAccent else Color.White,
                        modifier = Modifier.size(27.dp).scale(likeScale)
                    )
                }
                Text(formatCount(video.likesCount), color = Color.White, fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FeedActionButton(
                    onClick = {
                        share(
                            if (isShelterStory) {
                                "Mira esta historia de $displayName en HuellaLive. ${video.description.take(90)}"
                            } else {
                                "Conoce a $animalName en HuellaLive. ${video.description.take(90)}"
                            }
                        )
                    }
                ) {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = "Compartir",
                        tint = Color.White,
                        modifier = Modifier.size(25.dp)
                    )
                }
                Text("Compartir", color = Color.White, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FeedActionButton(
                    onClick = { showReportDialog = true }
                ) {
                    Icon(
                        Icons.Default.Flag,
                        contentDescription = "Reportar",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text("Reportar", color = Color.White, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ReportVideoDialog(
    animalName: String,
    onDismiss: () -> Unit,
    onReportVideo: (String) -> Unit,
    onReportProfile: (String) -> Unit
) {
    val reasons = listOf(
        "Contenido inapropiado",
        "Maltrato o riesgo",
        "Informacion falsa",
        "Spam",
        "Otro"
    )
    var selectedReason by remember { mutableStateOf(reasons.first()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reportar publicacion") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Ayudanos a revisar el video de $animalName.",
                    style = MaterialTheme.typography.bodyMedium
                )
                reasons.forEach { reason ->
                    FilterChip(
                        selected = selectedReason == reason,
                        onClick = { selectedReason = reason },
                        label = { Text(reason) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onReportVideo(selectedReason) },
                colors = ButtonDefaults.buttonColors(containerColor = DustyRose)
            ) {
                Text("Reportar video", color = TextOnAccent)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onReportProfile(selectedReason) }) {
                    Text("Reportar perfil")
                }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
}

@Composable
private fun FeedActionButton(
    onClick: () -> Unit,
    containerColor: Color = Color.Black.copy(alpha = 0.44f),
    content: @Composable () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 700f),
        finishedListener = { pressed = false }
    )
    Surface(
        onClick = {
            pressed = true
            onClick()
        },
        modifier = Modifier.size(50.dp).scale(scale),
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
        shadowElevation = 3.dp
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable
private fun FeedChip(text: String, background: Color, content: Color) {
    Surface(color = background, shape = RoundedCornerShape(8.dp)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            color = content,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

private fun adoptionStatus(status: String): Pair<String, Color> = when (status) {
    "AVAILABLE" -> "Disponible para adoptar" to Color(0xFF2EAD6B)
    "ADOPTED" -> "Adoptado" to Color(0xFF7C6CE5)
    "RECOVERING" -> "En recuperacion" to Color(0xFFE2A93B)
    "PREGNANT" -> "En cuidado" to Color(0xFFD86D9A)
    else -> "No disponible" to Color(0xFF8A8A8A)
}

private fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> "${count / 1_000_000}M"
    count >= 1_000 -> "${count / 1_000}K"
    else -> count.toString()
}

fun stableCloudinaryVideoUrl(url: String): String {
    if (!url.contains("res.cloudinary.com") || !url.contains("/upload/")) return url
    val parts = url.split("/upload/")
    if (parts.size != 2) return url
    val rest = parts[1]
    val firstSegment = rest.substringBefore("/")
    val hasTransform = !firstSegment.matches(Regex("v\\d+"))
    if (hasTransform) return url
    return "${parts[0]}/upload/f_mp4,vc_h264,ac_aac,q_auto/$rest"
}
