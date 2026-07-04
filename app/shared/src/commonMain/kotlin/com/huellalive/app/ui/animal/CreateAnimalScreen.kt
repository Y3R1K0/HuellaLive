package com.huellalive.app.ui.animal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.huellalive.app.data.repository.AnimalRepository
import com.huellalive.app.ui.theme.*
import org.koin.compose.koinInject

class CreateAnimalScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val animalRepo = koinInject<AnimalRepository>()
        val viewModel = rememberScreenModel { CreateAnimalViewModel(animalRepo) }
        val state by viewModel.uiState.collectAsState()

        var name by remember { mutableStateOf("") }
        var species by remember { mutableStateOf("") }
        var requestedSpeciesName by remember { mutableStateOf("") }
        var breed by remember { mutableStateOf("") }
        var age by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var status by remember { mutableStateOf("AVAILABLE") }
        var birthDate by remember { mutableStateOf("") }
        var speciesExpanded by remember { mutableStateOf(false) }
        var statusExpanded by remember { mutableStateOf(false) }

        val statusOptions = listOf(
            "AVAILABLE" to "Disponible",
            "RECOVERING" to "En recuperación",
            "PREGNANT" to "Preñada",
            "OTHER" to "Otro"
        )

        LaunchedEffect(state.isSuccess) {
            if (state.isSuccess) navigator.pop()
        }
        LaunchedEffect(state.species) {
            if (species.isBlank() && state.species.isNotEmpty()) {
                species = state.species.firstOrNull { it.name == "Perro" }?.name
                    ?: state.species.first().name
            }
        }

        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DustyRose,
            unfocusedBorderColor = Outline,
            focusedLabelColor = DustyRose,
            cursorColor = DustyRose
        )

        Box(modifier = Modifier.fillMaxSize().background(Background)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 80.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Nuevo Animal 🐾", style = MaterialTheme.typography.headlineLarge, color = TextPrimary)
                Text("El animal quedará registrado en tu albergue con credenciales automáticas", style = MaterialTheme.typography.bodySmall, color = TextSecondary)

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre *") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = fieldColors)

                ExposedDropdownMenuBox(expanded = speciesExpanded, onExpandedChange = { speciesExpanded = it }) {
                    OutlinedTextField(
                        value = species, onValueChange = {}, readOnly = true,
                        label = { Text("Especie *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = speciesExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = MaterialTheme.shapes.medium, colors = fieldColors
                    )
                    ExposedDropdownMenu(expanded = speciesExpanded, onDismissRequest = { speciesExpanded = false }) {
                        state.species.forEach {
                            DropdownMenuItem(
                                text = { Text(it.name, color = TextPrimary) },
                                onClick = {
                                    species = it.name
                                    requestedSpeciesName = ""
                                    speciesExpanded = false
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Otro", color = TextPrimary) },
                            onClick = {
                                species = "Otro"
                                speciesExpanded = false
                            }
                        )
                    }
                }

                if (species == "Otro") {
                    OutlinedTextField(
                        value = requestedSpeciesName,
                        onValueChange = { requestedSpeciesName = it },
                        label = { Text("Nombre de la nueva especie *") },
                        supportingText = { Text("Se enviara al administrador para su aprobacion") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = fieldColors
                    )
                }

                OutlinedTextField(value = breed, onValueChange = { breed = it }, label = { Text("Raza (opcional)") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = fieldColors)
                OutlinedTextField(value = age, onValueChange = { age = it.filter { c -> c.isDigit() } }, label = { Text("Edad en años (opcional)") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = fieldColors)
                OutlinedTextField(value = birthDate, onValueChange = { birthDate = it }, label = { Text("Fecha de nacimiento (YYYY-MM-DD)") }, placeholder = { Text("2022-01-15", color = TextTertiary) }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = fieldColors)
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descripción") }, minLines = 3, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = fieldColors)

                ExposedDropdownMenuBox(expanded = statusExpanded, onExpandedChange = { statusExpanded = it }) {
                    OutlinedTextField(
                        value = statusOptions.find { it.first == status }?.second ?: "",
                        onValueChange = {}, readOnly = true,
                        label = { Text("Estado *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = MaterialTheme.shapes.medium, colors = fieldColors
                    )
                    ExposedDropdownMenu(expanded = statusExpanded, onDismissRequest = { statusExpanded = false }) {
                        statusOptions.forEach { (value, label) ->
                            DropdownMenuItem(text = { Text(label, color = TextPrimary) }, onClick = { status = value; statusExpanded = false })
                        }
                    }
                }

                if (state.errorMessage != null) {
                    Text(state.errorMessage!!, color = Error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        viewModel.createAnimal(
                            name = name,
                            species = species,
                            requestedSpeciesName = requestedSpeciesName.ifBlank { null },
                            breed = breed.ifBlank { null },
                            age = age.toIntOrNull(),
                            description = description.ifBlank { null },
                            status = status,
                            birthDate = birthDate.ifBlank { null }
                        )
                    },
                    enabled = !state.isLoading &&
                        !state.isLoadingSpecies &&
                        name.isNotBlank() &&
                        species.isNotBlank() &&
                        (species != "Otro" || requestedSpeciesName.isNotBlank()),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = DustyRose)
                ) {
                    if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(22.dp), color = TextOnAccent, strokeWidth = 2.dp)
                    else Text("Crear Animal", style = MaterialTheme.typography.titleMedium, color = TextOnAccent)
                }
            }

            IconButton(
                onClick = { navigator.pop() },
                modifier = Modifier.align(Alignment.TopStart).padding(top = 40.dp, start = 8.dp).background(SurfaceRaised, androidx.compose.foundation.shape.CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, "Volver", tint = TextPrimary)
            }
        }
    }
}
