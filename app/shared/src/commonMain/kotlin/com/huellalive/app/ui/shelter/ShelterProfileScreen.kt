package com.huellalive.app.ui.shelter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.huellalive.app.data.repository.AnimalRepository
import com.huellalive.app.data.repository.AuthRepository
import com.huellalive.app.data.repository.ShelterRepository
import com.huellalive.app.ui.animal.CreateAnimalScreen
import com.huellalive.app.ui.feed.FeedScreen
import com.huellalive.app.ui.theme.*
import org.koin.compose.koinInject
import androidx.compose.ui.zIndex
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import com.huellalive.app.ui.animal.AnimalDetailScreenData


class ShelterProfileScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val shelterRepo = koinInject<ShelterRepository>()
        val animalRepo = koinInject<AnimalRepository>()
        val authRepo = koinInject<AuthRepository>()
        val viewModel = rememberScreenModel { ShelterProfileViewModel(shelterRepo, animalRepo, authRepo) }
        val state by viewModel.uiState.collectAsState()

        LazyColumn(modifier = Modifier.fillMaxSize().background(Background)) {

            // Cover + Avatar

            item {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                    IconButton(
                        onClick = { navigator.pop() },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 40.dp, start = 8.dp)
                            .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                            .zIndex(1f)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Volver",
                            tint = Color.White
                        )
                    }
                    if (state.shelter?.coverUrl != null) {
                        AsyncImage(model = state.shelter!!.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    } else {
                        Box(Modifier.fillMaxSize().background(SurfaceRaised))
                    }
                    Box(
                        modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp).offset(y = 40.dp).size(72.dp).clip(CircleShape).background(DustyRose),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.shelter?.user?.avatarUrl != null) {
                            AsyncImage(model = state.shelter!!.user.avatarUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Text(state.shelter?.user?.name?.first()?.toString()?.uppercase() ?: "A", color = TextOnAccent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (state.shelter?.status == "PENDING") {
                        Surface(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).padding(top = 8.dp), color = Warning.copy(alpha = 0.9f), shape = RoundedCornerShape(8.dp)) {
                            Text("Pendiente de aprobación", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = TextOnAccent)
                        }
                    }
                }
            }

            // Info
            item {
                Spacer(Modifier.height(48.dp))
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(state.shelter?.user?.name ?: "", style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    if (state.shelter?.location != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                            Text(state.shelter!!.location!!, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                    if (state.shelter?.description != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(state.shelter!!.description!!, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    }
                }
            }

            // Acciones
            item {
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {},
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DustyRose.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Chat, null, modifier = Modifier.size(16.dp), tint = DustyRose)
                        Spacer(Modifier.width(4.dp))
                        Text("Chats", color = DustyRose)
                    }
                    IconButton(onClick = { viewModel.logout(); navigator.replaceAll(FeedScreen()) }) {
                        Icon(Icons.Default.Logout, null, tint = Error)
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // Header animales
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Mis Animales (${state.animals.size})", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    if (state.shelter?.status == "APPROVED") {
                        Button(onClick = { navigator.push(CreateAnimalScreen()) }, modifier = Modifier.height(36.dp), colors = ButtonDefaults.buttonColors(containerColor = DustyRose)) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = TextOnAccent)
                            Spacer(Modifier.width(4.dp))
                            Text("Agregar", color = TextOnAccent, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            item {
                if (state.isLoading) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = DustyRose)
                    }
                } else if (state.animals.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No tienes animales registrados", color = TextSecondary)
                    }
                } else {
                    androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 2000.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        userScrollEnabled = false
                    ) {
                        items(state.animals) { animal ->
                            AnimalGridCard(animal = animal, onClick = {
                                navigator.push(AnimalDetailScreenData(animal.id))
                            })
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

@Composable
fun AnimalListItem(animal: AnimalDto, onClick: () -> Unit) {
    val (statusColor, statusLabel) = when (animal.status) {
        "AVAILABLE"  -> StatusAvailable to "Disponible"
        "RECOVERING" -> StatusRecovering to "En recuperación"
        "PREGNANT"   -> StatusPregnant to "Preñada"
        "ADOPTED"    -> StatusAdopted to "Adoptado"
        else         -> StatusOther to "Otro"
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp).clip(RoundedCornerShape(12.dp)).background(Surface).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceRaised), contentAlignment = Alignment.Center) {
            if (animal.photoUrl != null) {
                AsyncImage(model = animal.photoUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text(if (animal.species == "Perro") "🐶" else if (animal.species == "Gato") "🐱" else "🐾", fontSize = 24.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(animal.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text("${animal.species}${if (animal.breed != null) " · ${animal.breed}" else ""}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(4.dp))
            Surface(color = statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                Text(statusLabel, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = statusColor)
            }
        }
        Icon(Icons.Default.ChevronRight, null, tint = TextTertiary)
    }

}

@Composable
fun AnimalGridCard(animal: AnimalDto, onClick: () -> Unit) {
    val (statusColor, statusLabel) = when (animal.status) {
        "AVAILABLE"  -> StatusAvailable to "Disponible"
        "RECOVERING" -> StatusRecovering to "En recuperación"
        "PREGNANT"   -> StatusPregnant to "Preñada"
        "ADOPTED"    -> StatusAdopted to "Adoptado"
        else         -> StatusOther to "Otro"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .clickable(onClick = onClick)
    ) {
        // Foto grande
        if (animal.photoUrl != null) {
            AsyncImage(
                model = animal.photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                Modifier.fillMaxSize().background(SurfaceRaised),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (animal.species == "Perro") "🐶"
                    else if (animal.species == "Gato") "🐱" else "🐾",
                    fontSize = 40.sp
                )
            }
        }

        // Gradiente bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.BottomCenter)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            androidx.compose.ui.graphics.Color.Transparent,
                            androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        // Info bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        ) {
            Text(animal.name, style = MaterialTheme.typography.titleSmall, color = White, fontWeight = FontWeight.Bold)
            Text(animal.species, style = MaterialTheme.typography.bodySmall, color = White.copy(alpha = 0.8f))
        }

        // Status badge top right
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            color = statusColor,
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                statusLabel,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                style = MaterialTheme.typography.labelSmall,
                color = TextOnAccent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}