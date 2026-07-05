package com.huellalive.app.data.repository

import com.huellalive.app.data.model.ChatDto
import com.huellalive.app.data.model.AdoptionRequestDto
import com.huellalive.app.data.model.AnimalDto
import com.huellalive.app.data.model.MessageDto
import com.huellalive.app.data.model.SendMessageRequest
import com.huellalive.app.data.remote.ApiService
import com.huellalive.app.utils.Resource
import com.huellalive.app.utils.safeApiCall

class ChatRepository(private val api: ApiService) {
    suspend fun getChats(): Resource<List<ChatDto>> = safeApiCall { api.getChats() }
    suspend fun getMessages(chatId: String): Resource<List<MessageDto>> = safeApiCall { api.getMessages(chatId) }
    suspend fun sendMessage(chatId: String, content: String?, mediaUrl: String?): Resource<MessageDto> =
        safeApiCall { api.sendMessage(chatId, SendMessageRequest(content = content, mediaUrl = mediaUrl)) }
    suspend fun deleteChat(chatId: String): Resource<Unit> = safeApiCall { api.deleteChat(chatId) }
    suspend fun deleteEmptyChat(chatId: String): Resource<Unit> = safeApiCall { api.deleteEmptyChat(chatId) }
    suspend fun getTransferableAdoptions(chatId: String): Resource<List<AdoptionRequestDto>> =
        safeApiCall { api.getTransferableAdoptions(chatId) }
    suspend fun transferAnimal(chatId: String, animalId: String): Resource<AnimalDto> =
        safeApiCall { api.transferAnimalFromChat(chatId, animalId) }
}
