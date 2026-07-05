package com.huellalive.app.ui.chat

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.huellalive.app.data.local.SessionManager
import com.huellalive.app.data.model.ChatDto
import com.huellalive.app.data.model.AdoptionRequestDto
import com.huellalive.app.data.model.MessageDto
import com.huellalive.app.data.repository.ChatRepository
import com.huellalive.app.data.repository.MediaRepository
import com.huellalive.app.ui.animal.AnimalUpdateSignal
import com.huellalive.app.ui.feed.FeedRefreshSignal
import com.huellalive.app.utils.PickedMedia
import com.huellalive.app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatListUiState(
    val chats: List<ChatDto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ChatListViewModel(private val repository: ChatRepository) : ScreenModel {
    private val _uiState = MutableStateFlow(ChatListUiState(isLoading = true))
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.getChats()) {
                is Resource.Success -> _uiState.value = ChatListUiState(chats = result.data)
                is Resource.Error -> _uiState.value = ChatListUiState(errorMessage = result.message)
                else -> Unit
            }
        }
    }

    fun deleteChat(chatId: String) {
        screenModelScope.launch {
            when (val result = repository.deleteChat(chatId)) {
                is Resource.Success -> _uiState.value = _uiState.value.copy(
                    chats = _uiState.value.chats.filterNot { it.id == chatId },
                    errorMessage = null
                )
                is Resource.Error -> _uiState.value = _uiState.value.copy(errorMessage = result.message)
                else -> Unit
            }
        }
    }
}

data class ChatUiState(
    val messages: List<MessageDto> = emptyList(),
    val text: String = "",
    val selectedImage: PickedMedia? = null,
    val transferableAdoptions: List<AdoptionRequestDto> = emptyList(),
    val isLoadingTransfers: Boolean = false,
    val isTransferring: Boolean = false,
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val errorMessage: String? = null
)

class ChatViewModel(
    private val chatId: String,
    private val repository: ChatRepository,
    private val mediaRepository: MediaRepository,
    val sessionManager: SessionManager
) : ScreenModel {
    private val _uiState = MutableStateFlow(ChatUiState(isLoading = true))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init { load() }

    fun updateText(value: String) { _uiState.value = _uiState.value.copy(text = value) }
    fun selectImage(media: PickedMedia) {
        _uiState.value = _uiState.value.copy(selectedImage = media, errorMessage = null)
    }
    fun clearSelectedImage() {
        _uiState.value = _uiState.value.copy(selectedImage = null)
    }
    fun setError(message: String) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }

    fun loadTransferableAdoptions() {
        if (!sessionManager.isShelter()) return
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingTransfers = true, errorMessage = null)
            when (val result = repository.getTransferableAdoptions(chatId)) {
                is Resource.Success -> _uiState.value = _uiState.value.copy(
                    transferableAdoptions = result.data,
                    isLoadingTransfers = false
                )
                is Resource.Error -> _uiState.value = _uiState.value.copy(
                    isLoadingTransfers = false,
                    errorMessage = result.message
                )
                else -> _uiState.value = _uiState.value.copy(isLoadingTransfers = false)
            }
        }
    }

    fun transferAnimal(animalId: String, onDone: () -> Unit) {
        if (_uiState.value.isTransferring) return
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(isTransferring = true, errorMessage = null)
            when (val result = repository.transferAnimal(chatId, animalId)) {
                is Resource.Success -> {
                    AnimalUpdateSignal.publish(result.data)
                    FeedRefreshSignal.invalidate()
                    _uiState.value = _uiState.value.copy(
                        transferableAdoptions = _uiState.value.transferableAdoptions.filterNot {
                            it.animalId == animalId
                        },
                        isTransferring = false
                    )
                    load()
                    onDone()
                }
                is Resource.Error -> _uiState.value = _uiState.value.copy(
                    isTransferring = false,
                    errorMessage = result.message
                )
                else -> _uiState.value = _uiState.value.copy(isTransferring = false)
            }
        }
    }

    fun load() {
        screenModelScope.launch {
            when (val result = repository.getMessages(chatId)) {
                is Resource.Success -> _uiState.value = _uiState.value.copy(messages = result.data, isLoading = false)
                is Resource.Error -> _uiState.value = _uiState.value.copy(errorMessage = result.message, isLoading = false)
                else -> Unit
            }
        }
    }

    fun send() {
        val state = _uiState.value
        if (state.isSending || state.text.isBlank() && state.selectedImage == null) return
        screenModelScope.launch {
            _uiState.value = state.copy(isSending = true, errorMessage = null)
            var mediaUrl: String? = null
            state.selectedImage?.let { image ->
                when (val upload = mediaRepository.uploadChatImage(
                    chatId = chatId,
                    bytes = image.bytes,
                    fileName = image.fileName,
                    contentType = image.contentType
                )) {
                    is Resource.Success -> mediaUrl = upload.data.url
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isSending = false,
                            errorMessage = upload.message
                        )
                        return@launch
                    }
                    else -> Unit
                }
            }

            when (val result = repository.sendMessage(chatId, state.text.ifBlank { null }, mediaUrl)) {
                is Resource.Success -> _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + result.data,
                    text = "",
                    selectedImage = null,
                    isSending = false,
                    errorMessage = null
                )
                is Resource.Error -> _uiState.value = _uiState.value.copy(
                    isSending = false,
                    errorMessage = result.message
                )
                else -> Unit
            }
        }
    }

    fun deleteChat(onDone: () -> Unit) {
        screenModelScope.launch {
            when (val result = repository.deleteChat(chatId)) {
                is Resource.Success -> onDone()
                is Resource.Error -> _uiState.value = _uiState.value.copy(errorMessage = result.message)
                else -> Unit
            }
        }
    }

    fun cleanupIfEmpty() {
        screenModelScope.launch {
            repository.deleteEmptyChat(chatId)
        }
    }
}
