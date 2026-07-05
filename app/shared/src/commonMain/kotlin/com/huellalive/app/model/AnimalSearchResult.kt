package com.huellalive.app.model

import com.huellalive.app.data.model.AnimalDto
import kotlinx.serialization.Serializable

@Serializable
data class AnimalSearchResult(
    val data: List<AnimalDto>,
    val total: Int,
    val page: Int,
    val limit: Int,
    val totalPages: Int,
)