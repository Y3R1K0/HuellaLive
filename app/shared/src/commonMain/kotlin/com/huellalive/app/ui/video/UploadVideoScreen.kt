package com.huellalive.app.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.huellalive.app.data.model.UploadVideoRequest
import com.huellalive.app.data.repository.EngagementRepository
import com.huellalive.app.data.repository.MediaRepository
import com.huellalive.app.utils.Resource
import com.huellalive.app.ui.theme.*
import com.huellalive.app.ui.feed.FeedRefreshSignal
import com.huellalive.app.ui.shelter.ShelterStoryUpdateSignal
import com.huellalive.app.utils.PickedMedia
import com.huellalive.app.utils.rememberMediaPicker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

data class UploadVideoUiState(
    val selectedVideo: PickedMedia? = null,
    val description: String = "",
    val isSaving: Boolean = false,
    val message: String? = null
)

class UploadVideoViewModel(
    private val animalId: String?,
    private val target: UploadVideoTarget,
    private val engagementRepository: EngagementRepository,
    private val mediaRepository: MediaRepository
) : ScreenModel {
    private val _uiState = MutableStateFlow(UploadVideoUiState())
    val uiState = _uiState.asStateFlow()
    fun updateSelectedVideo(value: PickedMedia) { _uiState.value = _uiState.value.copy(selectedVideo = value, message = null) }
    fun updateDescription(value: String) { _uiState.value = _uiState.value.copy(description = value) }
    fun setMessage(value: String) { _uiState.value = _uiState.value.copy(message = value) }

    fun save(onDone: () -> Unit) {
        val state = _uiState.value
        val video = state.selectedVideo
        if (video == null) {
            _uiState.value = state.copy(message = "Selecciona un video")
            return
        }
        screenModelScope.launch {
            _uiState.value = state.copy(isSaving = true, message = null)
            val uploadResult = when (target) {
                UploadVideoTarget.Animal -> mediaRepository.uploadAnimalVideo(
                    animalId = animalId ?: "",
                    bytes = video.bytes,
                    fileName = video.fileName,
                    contentType = video.contentType
                )
                UploadVideoTarget.ShelterStory -> mediaRepository.uploadShelterStoryVideo(
                    bytes = video.bytes,
                    fileName = video.fileName,
                    contentType = video.contentType
                )
            }
            if (uploadResult is Resource.Error) {
                _uiState.value = _uiState.value.copy(isSaving = false, message = uploadResult.message)
                return@launch
            }

            val videoUrl = (uploadResult as Resource.Success).data.url
            val request = UploadVideoRequest(
                videoUrl = videoUrl,
                description = state.description
            )
            val result = when (target) {
                UploadVideoTarget.Animal -> engagementRepository.uploadAnimalVideo(
                    animalId ?: "",
                    request
                )
                UploadVideoTarget.ShelterStory -> engagementRepository.uploadShelterStory(request)
            }
            when (result) {
                is Resource.Success -> {
                    when (target) {
                        UploadVideoTarget.Animal -> FeedRefreshSignal.invalidate()
                        UploadVideoTarget.ShelterStory -> {
                            ShelterStoryUpdateSignal.publish(result.data)
                            FeedRefreshSignal.invalidate()
                        }
                    }
                    onDone()
                }
                is Resource.Error -> _uiState.value = _uiState.value.copy(isSaving = false, message = result.message)
                else -> Unit
            }
        }
    }
}

enum class UploadVideoTarget { Animal, ShelterStory }

data class UploadVideoScreen(
    val animalId: String? = null,
    val target: UploadVideoTarget = UploadVideoTarget.Animal
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val engagementRepository = koinInject<EngagementRepository>()
        val mediaRepository = koinInject<MediaRepository>()
        val viewModel = rememberScreenModel("${target.name}:${animalId.orEmpty()}") {
            UploadVideoViewModel(animalId, target, engagementRepository, mediaRepository)
        }
        val state by viewModel.uiState.collectAsState()
        val pickVideo = rememberMediaPicker(
            mimeType = "video/*",
            onPicked = { viewModel.updateSelectedVideo(it) },
            onError = { viewModel.setMessage(it) }
        )

        Column(Modifier.fillMaxSize().background(Background).padding(16.dp)) {
            Spacer(Modifier.height(24.dp))
            Text(
                if (target == UploadVideoTarget.ShelterStory) "Subir historia" else "Subir video",
                color = TextPrimary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (target == UploadVideoTarget.ShelterStory) "Este video aparecera en Historias de albergue" else "Este video aparecera en el perfil del animal y el feed",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = pickVideo,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, DustyRose.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Default.Movie, null, tint = DustyRose)
                Spacer(Modifier.width(8.dp))
                Text(
                    state.selectedVideo?.fileName ?: "Seleccionar video",
                    color = DustyRose,
                    maxLines = 1
                )
            }
            Spacer(Modifier.height(10.dp))
            DescriptionField(state.description, viewModel::updateDescription)
            Spacer(Modifier.height(16.dp))
            if (state.message != null) Text(state.message!!, color = Error)
            Button(
                onClick = { viewModel.save { navigator.pop() } },
                enabled = !state.isSaving && state.selectedVideo != null,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = DustyRose)
            ) {
                Icon(Icons.Default.CloudUpload, null, tint = TextOnAccent)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (state.isSaving) "Subiendo..." else if (target == UploadVideoTarget.ShelterStory) "Subir historia" else "Subir y publicar",
                    color = TextOnAccent
                )
            }
        }
    }
}

@Composable
private fun DescriptionField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text("Descripcion del video") },
        placeholder = { Text("Cuenta que esta pasando") },
        modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp),
        minLines = 3,
        maxLines = 5,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedBorderColor = DustyRose,
            unfocusedBorderColor = Outline,
            focusedLabelColor = DustyRose,
            unfocusedLabelColor = TextSecondary
        )
    )
}

