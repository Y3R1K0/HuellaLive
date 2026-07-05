package com.huellalive.app.utils

fun cloudinaryVideoPosterUrl(url: String): String? {
    if (!url.contains("res.cloudinary.com") || !url.contains("/video/upload/")) return null
    val parts = url.split("/video/upload/")
    if (parts.size != 2) return null

    val assetPath = parts[1]
        .split("/")
        .dropWhile { segment -> !segment.matches(Regex("v\\d+")) }
        .joinToString("/")
    if (assetPath.isBlank()) return null

    val posterPath = assetPath.replace(Regex("\\.[^.?#]+(?=\\?|#|$)"), ".jpg")
    return "${parts[0]}/video/upload/so_1,w_720,h_1280,c_fill,q_auto,f_jpg/$posterPath"
}
