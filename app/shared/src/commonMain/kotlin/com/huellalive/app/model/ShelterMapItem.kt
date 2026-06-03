package com.huellalive.app.model

import kotlinx.serialization.Serializable

@Serializable
data class ShelterMapItem(
    val id: String,
    val name: String,
    val city: String,
    val latitude: Double?,
    val longitude: Double?,
    val logoUrl: String?,
    val distanceKm: Double? = null,
    val animalCount: Int? = null,
)