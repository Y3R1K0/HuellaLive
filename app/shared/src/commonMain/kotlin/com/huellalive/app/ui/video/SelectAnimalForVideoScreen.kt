package com.huellalive.app.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Storefront
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
import com.huellalive.app.data.model.AnimalDto
import com.huellalive.app.data.repository.AnimalRepository
import com.huellalive.app.ui.shelter.AnimalListItem
import com.huellalive.app.ui.theme.*
import com.huellalive.app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

data class SelectAnimalVideoUiState(
    val animals: List<AnimalDto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class SelectAnimalForVideoViewModel(private val repository: AnimalRepository) : ScreenModel {
    private val _uiState = MutableStateFlow(SelectAnimalVideoUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    init { load() }

    fun load() {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = repository.getMyAnimals()) {
                is Resource.Success -> _uiState.value = SelectAnimalVideoUiState(animals = result.data, isLoading = false)
                is Resource.Error -> _uiState.value = SelectAnimalVideoUiState(errorMessage = result.message, isLoading = false)
                else -> Unit
            }
        }
    }
}

class SelectAnimalForVideoScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository = koinInject<AnimalRepository>()
        val viewModel = rememberScreenModel { SelectAnimalForVideoViewModel(repository) }
        val state by viewModel.uiState.collectAsState()

        LazyColumn(
            modifier = Modifier.fillMaxSize().background(Background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Spacer(Modifier.height(24.dp))
                Column(Modifier.fillMaxWidth()) {
                    Text("Subir video", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Publica una historia del albergue o elige un animal", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }

            item {
                Surface(
                    color = DustyRose.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { navigator.push(UploadVideoScreen(target = UploadVideoTarget.ShelterStory)) }
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storefront, null, tint = DustyRose)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Historia de albergue", color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text("Video propio del albergue, separado de animales", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            if (state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = DustyRose)
                    }
                }
            }

            if (state.errorMessage != null) {
                item { Text(state.errorMessage!!, color = Error) }
            }

            if (!state.isLoading && state.animals.isEmpty()) {
                item {
                    Surface(color = Surface, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Movie, null, tint = DustyRose)
                            Spacer(Modifier.width(10.dp))
                            Text("Primero crea un animal desde tu perfil de albergue.", color = TextSecondary)
                        }
                    }
                }
            }

            items(state.animals) { animal ->
                AnimalListItem(animal = animal, onClick = {
                    navigator.push(UploadVideoScreen(animal.id, UploadVideoTarget.Animal))
                })
            }
        }
    }
}
