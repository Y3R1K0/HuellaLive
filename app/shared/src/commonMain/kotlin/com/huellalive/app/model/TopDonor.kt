package com.huellalive.app.model

import kotlinx.serialization.Serializable

@Serializable
data class TopDonor(
    val position: Int,
    val userId: String,
    val userName: String,
    val avatarUrl: String?,
    val totalAmount: Double,
    val donationCount: Int,
)