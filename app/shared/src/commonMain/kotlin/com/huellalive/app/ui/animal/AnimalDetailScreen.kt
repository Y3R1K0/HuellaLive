package com.huellalive.app.ui.animal

import androidx.compose.foundation.background
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.huellalive.app.data.local.SessionManager
import com.huellalive.app.data.model.AnimalCardDto
import com.huellalive.app.data.model.AnimalDto
import com.huellalive.app.data.model.VideoDto
import com.huellalive.app.data.repository.AnimalRepository
import com.huellalive.app.ui.theme.*
import org.koin.compose.koinInject

data class AnimalDetailScreenData(val animalId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val animalRepo = koinInject<AnimalRepository>()
        val sessionManager = koinInject<SessionManager>()
        val viewModel = rememberScreenModel(animalId) {
            AnimalDetailViewModel(animalId, animalRepo, sessionManager)
        }
        val state by viewModel.uiState.collectAsState()
        val isLoggedIn = sessionManager.isLoggedIn()
        val isHuman = sessionManager.isHuman()

        if (state.showCardDialog && state.animal?.card != null) {
            AnimalCardDialog(
                card = state.animal!!.card!!,
                animalName = state.animal!!.name,
                onDismiss = { viewModel.toggleCardDialog() }
            )
        }

        Box(modifier = Modifier.fillMaxSize().background(Background)) {
            when {
                state.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = DustyRose
                )
                state.errorMessage != null -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.errorMessage!!, color = TextSecondary)
                    Button(onClick = { viewModel.loadAnimal() },
                        colors = ButtonDefaults.buttonColors(containerColor = DustyRose)) {
                        Text("Reintentar", color = TextOnAccent)
                    }
                }
                state.animal != null -> {
                    val animal = state.animal!!
                    LazyColumn(modifier = Modifier.fillMaxSize()) {

                        // Hero foto + back button
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(320.dp)) {
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
                                            fontSize = 80.sp
                                        )
                                    }
                                }

                                // Back button
                                IconButton(
                                    onClick = { navigator.pop() },
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(top = 40.dp, start = 8.dp)
                                        .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                        .zIndex(1f)
                                ) {
                                    Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                                }

                                // Status badge
                                val (statusColor, statusLabel) = when (animal.status) {
                                    "AVAILABLE"  -> StatusAvailable to "Disponible"
                                    "RECOVERING" -> StatusRecovering to "En recuperación"
                                    "PREGNANT"   -> StatusPregnant to "Preñada"
                                    "ADOPTED"    -> StatusAdopted to "Adoptado"
                                    else         -> StatusOther to "Otro"
                                }
                                Surface(
                                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = 12.dp),
                                    color = statusColor,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        statusLabel,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        color = TextOnAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        // Info básica
                        item {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(animal.name, style = MaterialTheme.typography.displayLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text(
                                    buildString {
                                        append(animal.species)
                                        if (animal.breed != null) append(" · ${animal.breed}")
                                        if (animal.age != null) append(" · ${animal.age} años")
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextSecondary
                                )
                                if (animal.description != null) {
                                    Spacer(Modifier.height(12.dp))
                                    Text(animal.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }
                            }
                        }

                        // Botones de acción
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.toggleCardDialog() },
                                    modifier = Modifier.weight(1f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DustyRose.copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Default.Description, null, modifier = Modifier.size(16.dp), tint = DustyRose)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Cartilla", color = DustyRose)
                                }

                                if (animal.status == "AVAILABLE" && isHuman) {
                                    Button(
                                        onClick = {},
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = DustyRose)
                                    ) {
                                        Icon(Icons.Default.Favorite, null, modifier = Modifier.size(16.dp), tint = TextOnAccent)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Adoptar", color = TextOnAccent)
                                    }
                                } else if (animal.status == "AVAILABLE" && !isLoggedIn) {
                                    Button(
                                        onClick = {},
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = DustyRose)
                                    ) {
                                        Text("Iniciar sesión para adoptar", color = TextOnAccent, fontSize = 12.sp)
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }

                        // Albergue de origen
                        if (animal.shelter != null) {
                            item {
                                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                                    Text("Albergue", style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.padding(bottom = 8.dp))
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                                        color = Surface,
                                        onClick = {}
                                    ) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(DustyRose),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (animal.shelter.user.avatarUrl != null) {
                                                    AsyncImage(model = animal.shelter.user.avatarUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                                } else {
                                                    Text(animal.shelter.user.name.first().toString().uppercase(), color = TextOnAccent, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(animal.shelter.user.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                                if (animal.shelter.location != null) {
                                                    Text(animal.shelter.location, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                                }
                                            }
                                            Icon(Icons.Default.ChevronRight, null, tint = TextTertiary)
                                        }
                                    }
                                    Spacer(Modifier.height(20.dp))
                                }
                            }
                        }

                        // Videos
                        if (animal.videos.isNotEmpty()) {
                            item {
                                Text("Videos", style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                            }
                            items(animal.videos) { video ->
                                VideoThumbnailRow(video = video)
                            }
                        }

                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoThumbnailRow(video: VideoDto) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp).clip(RoundedCornerShape(10.dp)).background(Surface).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(6.dp)).background(SurfaceRaised),
            contentAlignment = Alignment.Center
        ) {
            if (video.thumbnailUrl != null) {
                AsyncImage(model = video.thumbnailUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Default.PlayCircle, null, tint = DustyRose, modifier = Modifier.size(28.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(video.description, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, maxLines = 2)
            Text("❤️ ${video.likesCount}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
private fun AnimalCardDialog(
    card: AnimalCardDto,
    animalName: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyColumn(modifier = Modifier.padding(24.dp)) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                        Text("📋", fontSize = 28.sp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Cartilla de $animalName", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text("Historial veterinario", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }

                item {
                    CardInfoRow("Sexo", card.sex ?: "No registrado")
                    CardInfoRow("Esterilizado", if (card.isSterilized) "Sí ✅" else "No ❌")
                    if (card.birthDate != null) CardInfoRow("Fecha nac.", card.birthDate)
                    if (card.notes != null) {
                        Spacer(Modifier.height(8.dp))
                        Text("Notas", style = MaterialTheme.typography.labelLarge, color = DustyRose)
                        Text(card.notes, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Spacer(Modifier.height(16.dp))
                }

                if (card.vaccines.isNotEmpty()) {
                    item {
                        Text("Vacunas", style = MaterialTheme.typography.titleMedium, color = DustyRose, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    items(card.vaccines) { v ->
                        Surface(color = SurfaceRaised, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(v.name, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
                                Text(v.date, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }
                }

                if (card.controls.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        Text("Controles", style = MaterialTheme.typography.titleMedium, color = DustyRose, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    items(card.controls) { c ->
                        Surface(color = SurfaceRaised, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(c.description, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Fecha: ${c.date}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    if (c.nextDate != null) Text("Próximo: ${c.nextDate}", style = MaterialTheme.typography.bodySmall, color = DustyRose)
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = DustyRose)
                    ) {
                        Text("Cerrar", color = TextOnAccent)
                    }
                }
            }
        }
    }
}

@Composable
private fun CardInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}