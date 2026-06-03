package com.huellalive.app.model

data class AnimalSearchFilters(
    val name: String? = null,
    val species: String? = null,
    val city: String? = null,
    val status: String? = null,
    val page: Int = 1,
    val limit: Int = 20,
)