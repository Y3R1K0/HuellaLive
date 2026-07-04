package com.huellalive.app.utils

import io.ktor.client.plugins.ResponseException
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
    object Loading : Resource<Nothing>()
}

suspend fun <T> safeApiCall(call: suspend () -> T): Resource<T> {
    return try {
        Resource.Success(call())
    } catch (e: ResponseException) {
        Resource.Error(readHttpError(e))
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Resource.Error(e.message ?: "Error desconocido")
    }
}

private suspend fun readHttpError(e: ResponseException): String {
    val body = e.response.bodyAsText()
    return try {
        val json = Json.parseToJsonElement(body)
        val message = (json as? JsonObject)?.get("message")
        when (message) {
            is JsonArray -> message.joinToString(", ") { it.jsonPrimitive.content }
            else -> message?.jsonPrimitive?.content ?: body
        }
    } catch (_: Exception) {
        body.ifBlank { e.message ?: "Error ${e.response.status.value}" }
    }
}
