package com.huellalive.app.utils

fun versionedMediaUrl(url: String?, revision: Int): String? {
    if (url.isNullOrBlank() || revision <= 0) return url
    val separator = if ('?' in url) '&' else '?'
    return "$url${separator}hlv=$revision"
}
