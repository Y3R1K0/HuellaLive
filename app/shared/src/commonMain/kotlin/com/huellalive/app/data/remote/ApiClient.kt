package com.huellalive.app.data.remote

import com.huellalive.app.data.local.SessionManager
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

fun createHttpClient(sessionManager: SessionManager): HttpClient {
    return HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) {
            level = LogLevel.BODY
        }
        install(DefaultRequest) {
            url("http://10.0.2.2:3000/")
            contentType(ContentType.Application.Json)
            val token = sessionManager.getAccessToken()
            if (token != null) {
                headers.append("Authorization", "Bearer $token")
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 30_000
        }
    }
}