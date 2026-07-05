package com.huellalive.app.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.huellalive.app.data.local.SessionManager
import com.huellalive.app.data.model.SearchSpeciesDto
import com.huellalive.app.data.model.SpeciesRequestDto
import com.huellalive.app.data.repository.AdminRepository
import com.huellalive.app.ui.components.ConfirmDeleteDialog
import com.huellalive.app.ui.theme.Background
import com.huellalive.app.ui.theme.DustyRose
import com.huellalive.app.ui.theme.Error
import com.huellalive.app.ui.theme.Outline
import com.huellalive.app.ui.theme.SurfaceRaised
import com.huellalive.app.ui.theme.TextOnAccent
import com.huellalive.app.ui.theme.TextPrimary
import com.huellalive.app.ui.theme.TextSecondary
import com.huellalive.app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

data class AdminSpeciesUiState(
    val species: List<SearchSpeciesDto> = emptyList(),
    val requests: List<SpeciesRequestDto> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class AdminSpeciesViewModel(private val repository: AdminRepository) : ScreenModel {
    private val _state = MutableStateFlow(AdminSpeciesUiState(isLoading = true))
    val state = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        screenModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            val species = repository.getSpecies()
            val requests = repository.getSpeciesRequests()
            _state.value = AdminSpeciesUiState(
                species = (species as? Resource.Success)?.data ?: _state.value.species,
                requests = (requests as? Resource.Success)?.data ?: _state.value.requests,
                errorMessage = listOf(species, requests)
                    .filterIsInstance<Resource.Error>()
                    .firstOrNull()
                    ?.message
            )
        }
    }

    fun addSpecies(name: String, onDone: () -> Unit) = act {
        repository.createSpecies(name).also {
            if (it is Resource.Success) onDone()
        }
    }

    fun setActive(species: SearchSpeciesDto, active: Boolean) = act {
        repository.setSpeciesActive(species.id, active)
    }

    fun delete(species: SearchSpeciesDto) = act {
        repository.deleteSpecies(species.id)
    }

    fun approve(request: SpeciesRequestDto) = act {
        repository.approveSpeciesRequest(request.id)
    }

    fun reject(request: SpeciesRequestDto) = act {
        repository.rejectSpeciesRequest(request.id, null)
    }

    private fun act(block: suspend () -> Resource<*>) {
        screenModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errorMessage = null)
            when (val result = block()) {
                is Resource.Success -> load()
                is Resource.Error -> _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = result.message
                )
                else -> _state.value = _state.value.copy(isLoading = false)
            }
        }
    }
}

class AdminSpeciesScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository = koinInject<AdminRepository>()
        val sessionManager = koinInject<SessionManager>()
        val viewModel = rememberScreenModel { AdminSpeciesViewModel(repository) }
        val state by viewModel.state.collectAsState()
        var newSpecies by remember { mutableStateOf("") }
        var speciesToDelete by remember { mutableStateOf<SearchSpeciesDto?>(null) }

        speciesToDelete?.let { species ->
            ConfirmDeleteDialog(
                title = "Eliminar especie",
                message = "Se eliminará \"${species.name}\" del catálogo. Los formularios dejarán de mostrarla y esta acción no se puede deshacer.",
                onDismiss = { speciesToDelete = null },
                onConfirm = {
                    speciesToDelete = null
                    viewModel.delete(species)
                }
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().background(Background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(22.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Administracion",
                            color = TextPrimary,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Catalogo y solicitudes de especies", color = TextSecondary)
                    }
                    IconButton(onClick = viewModel::load) {
                        Icon(Icons.Default.Refresh, "Actualizar", tint = DustyRose)
                    }
                    IconButton(onClick = {
                        sessionManager.clearSession()
                        navigator.popUntilRoot()
                    }) {
                        Icon(Icons.Default.Logout, "Cerrar sesion", tint = TextPrimary)
                    }
                }
            }

            state.errorMessage?.let { message ->
                item { Text(message, color = Error) }
            }

            item {
                Text(
                    "Solicitudes pendientes",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (!state.isLoading && state.requests.isEmpty()) {
                item { Text("No hay solicitudes pendientes", color = TextSecondary) }
            }

            items(state.requests) { request ->
                Surface(
                    color = SurfaceRaised,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Pets, null, tint = DustyRose)
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(request.requestedName, color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text(
                                    "${request.animal?.name ?: "Animal"} - ${request.shelter?.user?.name ?: "Albergue"}",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.approve(request) },
                                colors = ButtonDefaults.buttonColors(containerColor = DustyRose)
                            ) {
                                Icon(Icons.Default.Check, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Aprobar", color = TextOnAccent)
                            }
                            OutlinedButton(onClick = { viewModel.reject(request) }) {
                                Icon(Icons.Default.Close, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Rechazar")
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Especies disponibles",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newSpecies,
                        onValueChange = { newSpecies = it },
                        placeholder = { Text("Nueva especie") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        enabled = newSpecies.isNotBlank() && !state.isLoading,
                        onClick = {
                            viewModel.addSpecies(newSpecies) { newSpecies = "" }
                        }
                    ) {
                        Icon(Icons.Default.Add, "Agregar", tint = DustyRose)
                    }
                }
            }

            items(state.species) { species ->
                Surface(
                    color = SurfaceRaised,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(species.name, color = TextPrimary, modifier = Modifier.weight(1f))
                        Switch(
                            checked = species.isActive,
                            onCheckedChange = { viewModel.setActive(species, it) }
                        )
                        IconButton(onClick = { speciesToDelete = species }) {
                            Icon(Icons.Default.Delete, "Eliminar", tint = Error)
                        }
                    }
                }
            }

            if (state.isLoading) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(color = DustyRose)
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}
