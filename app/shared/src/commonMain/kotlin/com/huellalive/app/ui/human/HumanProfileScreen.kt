package com.huellalive.app.ui.human

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.huellalive.app.data.model.AnimalDto
import com.huellalive.app.data.model.LinkAnimalRequest
import com.huellalive.app.data.repository.AnimalRepository
import com.huellalive.app.data.repository.AuthRepository
import com.huellalive.app.data.repository.UserRepository
import com.huellalive.app.ui.feed.FeedScreen
import com.huellalive.app.ui.theme.*
import org.koin.compose.koinInject
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.CircleShape

class HumanProfileScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val userRepo = koinInject<UserRepository>()
        val animalRepo = koinInject<AnimalRepository>()
        val authRepo = koinInject<AuthRepository>()
        val viewModel = rememberScreenModel { HumanProfileViewModel(userRepo, animalRepo, authRepo) }
        val state by viewModel.uiState.collectAsState()
        var showLinkDialog by remember { mutableStateOf(false) }

        if (showLinkDialog) {
            LinkAnimalDialog(
                onDismiss = { showLinkDialog = false },
                onConfirm = { _, _ -> showLinkDialog = false }
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize().background(Background)) {

            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Botón atrás flotante
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

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(modifier = Modifier.size(90.dp).clip(CircleShape).background(DustyRose), contentAlignment = Alignment.Center) {
                            if (state.user?.avatarUrl != null) {
                                AsyncImage(model = state.user!!.avatarUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            } else {
                                Text(state.user?.name?.first()?.toString()?.uppercase() ?: "U", color = TextOnAccent, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(state.user?.name ?: "", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                        Text(state.user?.email ?: "", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                            StatItem("Adoptados", "${state.adoptedAnimals.size}")
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { showLinkDialog = true },
                                border = androidx.compose.foundation.BorderStroke(1.dp, DustyRose.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.Link, null, modifier = Modifier.size(16.dp), tint = DustyRose)
                                Spacer(Modifier.width(6.dp))
                                Text("Vincular mascota", color = DustyRose)
                            }
                            IconButton(onClick = { viewModel.logout(); navigator.replaceAll(FeedScreen()) }) {
                                Icon(Icons.Default.Logout, null, tint = Error)
                            }
                        }
                    }
                }
            }

            // Insignias
            if (state.user?.badges?.isNotEmpty() == true) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text("Insignias", style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.padding(bottom = 8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.user!!.badges) { userBadge ->
                                BadgeChip(tier = userBadge.badge.tier, name = userBadge.badge.name)
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                }
            }

            item {
                Text("Mis Animales", style = MaterialTheme.typography.titleLarge, color = TextPrimary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }

            if (state.adoptedAnimals.isEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🐾", fontSize = 40.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Aún no has vinculado ningún animal", color = TextSecondary, textAlign = TextAlign.Center)
                    }
                }
            } else {
                items(state.adoptedAnimals) { animal ->
                    AdoptedAnimalCard(animal = animal, onClick = {})
                }
            }

            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
fun BadgeChip(tier: String, name: String) {
    val (color, emoji) = when (tier) {
        "BRONZE"   -> BadgeBronze to "🥉"
        "SILVER"   -> BadgeSilver to "🥈"
        "GOLD"     -> BadgeGold to "🥇"
        "PLATINUM" -> BadgePlatinum to "💎"
        "DIAMOND"  -> BadgeDiamond to "💠"
        else       -> TextTertiary to "🏅"
    }
    Surface(color = color.copy(alpha = 0.15f), shape = RoundedCornerShape(20.dp)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 14.sp)
            Spacer(Modifier.width(6.dp))
            Text(name, style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}

@Composable
private fun AdoptedAnimalCard(animal: AnimalDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp).clip(RoundedCornerShape(12.dp)).background(Surface).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceRaised), contentAlignment = Alignment.Center) {
            if (animal.photoUrl != null) {
                AsyncImage(model = animal.photoUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text(if (animal.species == "Perro") "🐶" else if (animal.species == "Gato") "🐱" else "🐾", fontSize = 22.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(animal.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text(animal.species, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Icon(Icons.Default.ChevronRight, null, tint = TextTertiary)
    }
}

@Composable
private fun LinkAnimalDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Surface, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("🐾", fontSize = 32.sp)
                Spacer(Modifier.height(8.dp))
                Text("Vincular mascota", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Text("Ingresa las credenciales del animal", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(20.dp))

                val fieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DustyRose,
                    unfocusedBorderColor = Outline,
                    focusedLabelColor = DustyRose,
                    cursorColor = DustyRose
                )

                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Usuario del animal") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = fieldColors)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Contraseña") }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, colors = fieldColors)
                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancelar", color = TextSecondary)
                    }
                    Button(
                        onClick = { onConfirm(username, password) },
                        enabled = username.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = DustyRose)
                    ) {
                        Text("Vincular", color = TextOnAccent)
                    }
                }
            }
        }
    }
}