package com.huellalive.app.data.remote

import com.huellalive.app.data.model.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*

class ApiService(private val client: HttpClient) {

    // AUTH
    suspend fun login(request: LoginRequest): AuthResponse =
        client.post("auth/login") { setBody(request) }.body()

    suspend fun registerHuman(request: RegisterHumanRequest): AuthResponse =
        client.post("auth/register/human") { setBody(request) }.body()

    suspend fun registerShelter(request: RegisterShelterRequest): AuthResponse =
        client.post("auth/register/shelter") { setBody(request) }.body()

    suspend fun linkAnimal(request: LinkAnimalRequest): AnimalDto =
        client.post("auth/link-animal") { setBody(request) }.body()

    // FEED
    suspend fun getFeed(page: Int = 1): List<VideoDto> =
        client.get("feed") { parameter("page", page) }.body()

    // ANIMALS
    suspend fun getAnimalById(id: String): AnimalDto =
        client.get("animals/$id").body()

    suspend fun getMyAnimals(): List<AnimalDto> =
        client.get("animals/shelter/mine").body()

    suspend fun getAdoptedAnimals(): List<AnimalDto> =
        client.get("animals/human/adopted").body()

    suspend fun createAnimal(request: CreateAnimalRequest): AnimalDto =
        client.post("animals") { setBody(request) }.body()

    suspend fun updateAnimalStatus(id: String, status: String): AnimalDto =
        client.patch("animals/$id/status") { setBody(mapOf("status" to status)) }.body()

    suspend fun getAnimalCredentials(id: String): AnimalCredentialsDto =
        client.get("animals/$id/credentials").body()

    // SHELTERS
    suspend fun getShelterById(id: String): ShelterProfileDto =
        client.get("shelters/$id").body()

    suspend fun getMyShelterProfile(): ShelterProfileDto =
        client.get("shelters/me/profile").body()

    suspend fun updateShelterProfile(data: Map<String, String>): ShelterProfileDto =
        client.patch("shelters/me/profile") { setBody(data) }.body()

    suspend fun getNearbyShelters(): List<ShelterProfileDto> =
        client.get("shelters/nearby").body()

    // USERS
    suspend fun getMe(): UserProfileDto =
        client.get("users/me").body()

    suspend fun updateMe(data: Map<String, String>): UserProfileDto =
        client.patch("users/me") { setBody(data) }.body()

    suspend fun getShelterAdoptedAnimals(): List<AnimalDto> =
        client.get("animals/shelter/adopted").body()

    suspend fun updateAnimalCard(id: String, data: Map<String, Any>): AnimalCardDto =
        client.put("animals/$id/card") { setBody(data) }.body()
}