package com.huellalive.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.huellalive.app.ui.theme.*
import com.huellalive.app.utils.ImageAdjustment
import com.huellalive.app.utils.PickedMedia
import kotlin.math.abs
import kotlin.math.max

@Composable
fun AdjustableImagePreview(
    title: String,
    selectedMedia: PickedMedia?,
    currentUrl: String?,
    adjustment: ImageAdjustment,
    onAdjustmentChange: (ImageAdjustment) -> Unit,
    aspectRatio: Float,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(18.dp),
) {
    val imageModel: Any? = selectedMedia?.bytes ?: currentUrl
    if (imageModel == null) return

    val controlsEnabled = selectedMedia != null
    var previewSize by remember { mutableStateOf(IntSize.Zero) }
    val latestAdjustment by rememberUpdatedState(adjustment)
    LaunchedEffect(selectedMedia?.fileName) {
        onAdjustmentChange(ImageAdjustment())
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = SurfaceRaised,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        if (controlsEnabled) "Encuadre de guardado" else "Vista actual",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (controlsEnabled) {
                    IconButton(
                        onClick = {
                            onAdjustmentChange(ImageAdjustment())
                        }
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Restablecer encuadre", tint = DustyRose)
                    }
                }
            }

            val maxPreviewShiftX = max(90f, previewSize.width * 0.34f)
            val maxPreviewShiftY = max(120f, previewSize.height * 0.34f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .onSizeChanged { previewSize = it }
                    .clip(shape)
                    .background(Surface)
                    .then(
                        if (controlsEnabled) {
                            Modifier
                                .pointerInput(selectedMedia?.fileName, adjustment) {
                                    detectTapGestures(
                                        onDoubleTap = {
                                            onAdjustmentChange(
                                                if (latestAdjustment.zoom > 1.05f) {
                                                    ImageAdjustment()
                                                } else {
                                                    latestAdjustment.copy(zoom = 1.8f)
                                                }
                                            )
                                        }
                                    )
                                }
                                .pointerInput(selectedMedia?.fileName, previewSize) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        var previousCentroid = down.position
                                        var previousDistance = 0f
                                        var isAdjustingImage = false

                                        do {
                                            val event = awaitPointerEvent()
                                            val pressed = event.changes.filter { it.pressed }
                                            val centroid = if (pressed.isNotEmpty()) {
                                                pressed.map { it.position }.reduce { acc, offset -> acc + offset } / pressed.size.toFloat()
                                            } else {
                                                previousCentroid
                                            }
                                            val distance = if (pressed.size > 1) {
                                                val center = centroid
                                                pressed.map { (it.position - center).getDistance() }.average().toFloat()
                                            } else {
                                                0f
                                            }
                                            val zoomChange = if (previousDistance > 0f && distance > 0f) {
                                                distance / previousDistance
                                            } else {
                                                1f
                                            }
                                            val pan = centroid - previousCentroid
                                            val hasPinch = pressed.size > 1 && abs(zoomChange - 1f) > 0.01f
                                            val hasHorizontalPan = abs(pan.x) > abs(pan.y) * 1.2f
                                            val imageIsZoomed = latestAdjustment.zoom > 1.03f
                                            if (hasPinch || imageIsZoomed || hasHorizontalPan) {
                                                isAdjustingImage = true
                                            }
                                            val base = latestAdjustment
                                            if (isAdjustingImage && (zoomChange != 1f || pan != Offset.Zero)) {
                                                val nextZoom = (base.zoom * zoomChange).coerceIn(1f, 2.4f)
                                                val nextOffsetX = (base.offsetX + pan.x / maxPreviewShiftX).coerceIn(-1f, 1f)
                                                val nextOffsetY = (base.offsetY + pan.y / maxPreviewShiftY).coerceIn(-1f, 1f)
                                                onAdjustmentChange(
                                                    ImageAdjustment(
                                                        zoom = nextZoom,
                                                        offsetX = nextOffsetX,
                                                        offsetY = nextOffsetY,
                                                    )
                                                )
                                                event.changes.forEach { it.consume() }
                                            }
                                            previousCentroid = centroid
                                            previousDistance = distance
                                        } while (event.changes.any { it.pressed })
                                    }
                                }
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = imageModel,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = adjustment.zoom
                            scaleY = adjustment.zoom
                            translationX = adjustment.offsetX * maxPreviewShiftX
                            translationY = adjustment.offsetY * maxPreviewShiftY
                        }
                )
            }
        }
    }
}
