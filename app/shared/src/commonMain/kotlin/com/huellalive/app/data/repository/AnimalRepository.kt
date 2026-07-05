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
    suspend fun getSpecies(): Resource<List<SearchSpeciesDto>> = safeApiCall { api.getSearchSpecies() }
    suspend fun updateAnimal(id: String, request: UpdateAnimalRequest): Resource<AnimalDto> = safeApiCall { api.updateAnimal(id, request) }
    suspend fun updateAnimalStatus(id: String, status: String): Resource<AnimalDto> = safeApiCall { api.updateAnimalStatus(id, status) }
    suspend fun getAnimalCredentials(id: String): Resource<AnimalCredentialsDto> = safeApiCall { api.getAnimalCredentials(id) }
    suspend fun getShelterAdoptedAnimals(): Resource<List<AnimalDto>> = safeApiCall { api.getShelterAdoptedAnimals() }
    suspend fun updateAnimalCard(id: String, request: UpdateAnimalCardRequest): Resource<AnimalCardDto> =
        safeApiCall { api.updateAnimalCard(id, request) }
}
