package com.huellalive.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.huellalive.app.data.model.AnimalDto
import com.huellalive.app.data.model.ShelterProfileDto
import com.huellalive.app.data.repository.SearchRepository
import com.huellalive.app.ui.animal.AnimalDetailScreenData
import com.huellalive.app.ui.shelter.AnimalListItem
import com.huellalive.app.ui.shelter.ShelterDetailScreen
import com.huellalive.app.ui.theme.*
import org.koin.compose.koinInject

class SearchScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val searchRepo = koinInject<SearchRepository>()
        val viewModel = rememberScreenModel { SearchViewModel(searchRepo) }
        val state by viewModel.uiState.collectAsState()
        var showFilters by remember { mutableStateOf(false) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
        ) {
            Spacer(Modifier.height(52.dp))

            // Search bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = { viewModel.onQueryChange(it) },
                    placeholder = { Text("Buscar animales o albergues...", color = TextTertiary) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary) },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChange("") }) {
                                Icon(Icons.Default.Close, null, tint = TextSecondary)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DustyRose,
                        unfocusedBorderColor = Outline,
                        focusedContainerColor = Surface,
                        unfocusedContainerColor = Surface,
                        cursorColor = DustyRose
                    ),
                    singleLine = true
                )

                IconButton(
                    onClick = { showFilters = !showFilters },
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            if (showFilters) DustyRose else Surface,
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    Icon(
                        Icons.Default.FilterList,
                        null,
                        tint = if (showFilters) TextOnAccent else TextSecondary
                    )
                }
            }

            // Filtros
            if (showFilters) {
                FilterPanel(
                    currentSpecies = state.filterSpecies,
                    currentStatus = state.filterStatus,
                    onSpeciesSelected = { viewModel.setFilter(species = it) },
                    onStatusSelected = { viewModel.setFilter(status = it) },
                    onClear = { viewModel.clearFilters() }
                )
            }

            // Filtros activos chips
            val hasFilters = state.filterSpecies != null || state.filterCity != null || state.filterStatus != null
            if (hasFilters) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (state.filterSpecies != null) {
                        FilterChip(label = state.filterSpecies!!, onRemove = { viewModel.setFilter(species = null) })
                    }
                    if (state.filterStatus != null) {
                        val statusLabel = when (state.filterStatus) {
                            "AVAILABLE"  -> "Disponible"
                            "RECOVERING" -> "En recuperación"
                            "PREGNANT"   -> "Preñada"
                            else -> state.filterStatus!!
                        }
                        FilterChip(label = statusLabel, onRemove = { viewModel.setFilter(status = null) })
                    }
                    if (state.filterCity != null) {
                        FilterChip(label = state.filterCity!!, onRemove = { viewModel.setFilter(city = null) })
                    }
                }
            }

            // Resultados
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DustyRose)
                }
                !state.hasSearched -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("Busca animales y albergues", color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
                        Text("Usa filtros para afinar tu búsqueda", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
                    }
                }
                state.animals.isEmpty() && state.shelters.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("😕", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("Sin resultados", color = TextSecondary, style = MaterialTheme.typography.bodyLarge)
                        Text("Intenta con otros filtros", color = TextTertiary, style = MaterialTheme.typography.bodySmall)
                    }
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    if (state.animals.isNotEmpty()) {
                        item {
                            Text(
                                "Animales (${state.animals.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(state.animals) { animal ->
                            AnimalListItem(
                                animal = animal,
                                onClick = { navigator.push(AnimalDetailScreenData(animal.id)) }
                            )
                        }
                    }

                    if (state.shelters.isNotEmpty()) {
                        item {
                            Text(
                                "Albergues (${state.shelters.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(state.shelters) { shelter ->
                            ShelterSearchCard(
                                shelter = shelter,
                                onClick = { navigator.push(ShelterDetailScreen(shelter.id)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPanel(
    currentSpecies: String?,
    currentStatus: String?,
    onSpeciesSelected: (String?) -> Unit,
    onStatusSelected: (String?) -> Unit,
    onClear: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
            .padding(16.dp)
    ) {
        Text("Especie", style = MaterialTheme.typography.labelLarge, color = DustyRose)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Perro", "Gato", "Conejo", "Ave", "Otro").forEach { s ->
                FilterChipSelectable(
                    label = s,
                    selected = currentSpecies == s,
                    onClick = { onSpeciesSelected(if (currentSpecies == s) null else s) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("Estado", style = MaterialTheme.typography.labelLarge, color = DustyRose)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            mapOf("AVAILABLE" to "Disponible", "RECOVERING" to "Recuperación", "PREGNANT" to "Preñada").forEach { (value, label) ->
                FilterChipSelectable(
                    label = label,
                    selected = currentStatus == value,
                    onClick = { onStatusSelected(if (currentStatus == value) null else value) }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onClear) {
            Text("Limpiar filtros", color = TextSecondary)
        }
    }
}

@Composable
private fun FilterChipSelectable(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (selected) DustyRose else SurfaceRaised,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) TextOnAccent else TextSecondary
        )
    }
}

@Composable
private fun FilterChip(label: String, onRemove: () -> Unit) {
    Surface(color = DustyRose.copy(alpha = 0.2f), shape = RoundedCornerShape(20.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = DustyRose)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Default.Close, null, tint = DustyRose, modifier = Modifier.size(14.dp).clickable(onClick = onRemove))
        }
    }
}

@Composable
private fun ShelterSearchCard(shelter: ShelterProfileDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(DustyRose),
            contentAlignment = Alignment.Center
        ) {
            if (shelter.user.avatarUrl != null) {
                AsyncImage(model = shelter.user.avatarUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text(shelter.user.name.first().toString().uppercase(), color = TextOnAccent, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(shelter.user.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            if (shelter.location != null) {
                Text(shelter.location, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
        Surface(color = MintCream.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp)) {
            Text("Albergue", modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = MintCream)
        }
    }
}