package com.huellalive.app.data

import com.huellalive.app.model.TopDonor
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class DonationRepository(private val client: HttpClient) {

    private val BASE_URL = "http://10.0.2.2:3000" // ajusta si ya tienes una constante global

    suspend fun getTopDonors(limit: Int = 10): List<TopDonor> {
        return client.get("$BASE_URL/donations/top-donors") {
            parameter("limit", limit)
        }.body()
    }
}