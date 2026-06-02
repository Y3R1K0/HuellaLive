package com.huellalive.app.data.repository

import com.huellalive.app.data.model.*
import com.huellalive.app.data.remote.ApiService
import com.huellalive.app.utils.Resource
import com.huellalive.app.utils.safeApiCall

class AnimalRepository(private val api: ApiService) {
    suspend fun getAnimalById(id: String): Resource<AnimalDto> = safeApiCall { api.getAnimalById(id) }
    suspend fun getMyAnimals(): Resource<List<AnimalDto>> = safeApiCall { api.getMyAnimals() }
    suspend fun getAdoptedAnimals(): Resource<List<AnimalDto>> = safeApiCall { api.getAdoptedAnimals() }
    suspend fun createAnimal(request: CreateAnimalRequest): Resource<AnimalDto> = safeApiCall { api.createAnimal(request) }
    suspend fun updateAnimalStatus(id: String, status: String): Resource<AnimalDto> = safeApiCall { api.updateAnimalStatus(id, status) }
    suspend fun getAnimalCredentials(id: String): Resource<AnimalCredentialsDto> = safeApiCall { api.getAnimalCredentials(id) }
    suspend fun getShelterAdoptedAnimals(): Resource<List<AnimalDto>> = safeApiCall { api.getShelterAdoptedAnimals() }
    suspend fun updateAnimalCard(id: String, data: Map<String, Any>): Resource<AnimalCardDto> = safeApiCall { api.updateAnimalCard(id, data) }
}