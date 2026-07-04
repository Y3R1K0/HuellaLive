package com.huellalive.app.utils

data class ImageAdjustment(
    val zoom: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
)

expect suspend fun adjustedPickedImage(
    media: PickedMedia,
    adjustment: ImageAdjustment,
    aspectRatio: Float,
    outputWidth: Int = 1200,
): PickedMedia
