package com.huellalive.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MediaUploadResponse(
    val url: String,
    val publicId: String,
    val resourceType: String,
    val format: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val duration: Double? = null,
    val bytes: Int? = null
)
