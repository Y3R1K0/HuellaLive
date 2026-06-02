package com.huellalive.app.ui.shelter

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
import androidx.compose.ui.zIndex
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.huellalive.app.data.local.SessionManager
import com.huellalive.app.data.repository.ShelterRepository
import com.huellalive.app.ui.animal.AnimalDetailScreenData
import com.huellalive.app.ui.shelter.AnimalListItem
import com.huellalive.app.ui.theme.*
import org.koin.compose.koinInject

data class ShelterDetailScreen(val shelterId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val shelterRepo = koinInject<ShelterRepository>()
        val sessionManager = koinInject<SessionManager>()
        val viewModel = rememberScreenModel(shelterId) {
            ShelterDetailViewModel(shelterId, shelterRepo, sessionManager)
        }
        val state by viewModel.uiState.collectAsState()
        val isLoggedIn = sessionManager.isLoggedIn()
        val isHuman = sessionManager.isHuman()

        Box(modifier = Modifier.fillMaxSize().background(Background)) {
            when {
                state.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = DustyRose)
                state.shelter != null -> {
                    val shelter = state.shelter!!
                    LazyColumn(modifier = Modifier.fillMaxSize()) {

                        // Cover + Avatar + back
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                                if (shelter.coverUrl != null) {
                                    AsyncImage(model = shelter.coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                } else {
                                    Box(Modifier.fillMaxSize().background(SurfaceRaised))
                                }

                                IconButton(
                                    onClick = { navigator.pop() },
                                    modifier = Modifier.align(Alignment.TopStart).padding(top = 40.dp, start = 8.dp).background(Color.Black.copy(alpha = 0.3f), CircleShape).zIndex(1f)
                                ) {
                                    Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                                }

                                Box(
                                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp).offset(y = 40.dp).size(72.dp).clip(CircleShape).background(DustyRose),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (shelter.user.avatarUrl != null) {
                                        AsyncImage(model = shelter.user.avatarUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                                    } else {
                                        Text(shelter.user.name.first().toString().uppercase(), color = TextOnAccent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Info
                        item {
                            Spacer(Modifier.height(48.dp))
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                Text(shelter.user.name, style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                                if (shelter.location != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                                        Text(shelter.location, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                }
                                if (shelter.description != null) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(shelter.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }

                        // Botones
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {},
                                    modifier = Modifier.weight(1f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DustyRose.copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Default.FavoriteBorder, null, modifier = Modifier.size(16.dp), tint = DustyRose)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Seguir", color = DustyRose)
                                }
                                OutlinedButton(
                                    onClick = {},
                                    modifier = Modifier.weight(1f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PeachWarm.copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Default.VolunteerActivism, null, modifier = Modifier.size(16.dp), tint = PeachWarm)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Donar", color = PeachWarm)
                                }
                                Button(
                                    onClick = {},
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = DustyRose)
                                ) {
                                    Icon(Icons.Default.Chat, null, modifier = Modifier.size(16.dp), tint = TextOnAccent)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Contactar", color = TextOnAccent)
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                        }

                        // Animales disponibles
                        item {
                            Text(
                                "Animales disponibles (${shelter.animals.size})",
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        if (shelter.animals.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("No hay animales disponibles", color = TextSecondary)
                                }
                            }
                        } else {
                            items(shelter.animals) { animal ->
                                AnimalListItem(animal = animal, onClick = {
                                    navigator.push(AnimalDetailScreenData(animal.id))
                                })
                            }
                        }

                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}
