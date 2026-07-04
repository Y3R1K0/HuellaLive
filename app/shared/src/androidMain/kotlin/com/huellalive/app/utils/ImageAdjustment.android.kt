package com.huellalive.app.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import java.io.ByteArrayOutputStream

actual suspend fun adjustedPickedImage(
    media: PickedMedia,
    adjustment: ImageAdjustment,
    aspectRatio: Float,
    outputWidth: Int,
): PickedMedia {
    val source = BitmapFactory.decodeByteArray(media.bytes, 0, media.bytes.size) ?: return media
    val safeAspect = aspectRatio.takeIf { it > 0f } ?: 1f
    val width = outputWidth.coerceAtLeast(320)
    val height = (width / safeAspect).toInt().coerceAtLeast(320)
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    val baseScale = maxOf(width / source.width.toFloat(), height / source.height.toFloat())
    val scale = baseScale * adjustment.zoom.coerceIn(1f, 2.4f)
    val drawWidth = source.width * scale
    val drawHeight = source.height * scale
    val maxShiftX = ((drawWidth - width) / 2f).coerceAtLeast(0f)
    val maxShiftY = ((drawHeight - height) / 2f).coerceAtLeast(0f)
    val left = (width - drawWidth) / 2f + adjustment.offsetX.coerceIn(-1f, 1f) * maxShiftX
    val top = (height - drawHeight) / 2f + adjustment.offsetY.coerceIn(-1f, 1f) * maxShiftY

    canvas.drawBitmap(source, null, RectF(left, top, left + drawWidth, top + drawHeight), paint)

    val bytes = ByteArrayOutputStream().use { stream ->
        output.compress(Bitmap.CompressFormat.JPEG, 92, stream)
        stream.toByteArray()
    }
    if (source != output) source.recycle()
    output.recycle()

    val baseName = media.fileName.substringBeforeLast('.', media.fileName)
    return PickedMedia(
        bytes = bytes,
        fileName = "$baseName-ajustada.jpg",
        contentType = "image/jpeg"
    )
}
