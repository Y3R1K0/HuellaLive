package com.huellalive.app.ui.explore

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.huellalive.app.data.local.SessionManager
import com.huellalive.app.data.model.AnimalDto
import com.huellalive.app.data.model.ShelterProfileDto
import com.huellalive.app.data.repository.ExploreRepository
import com.huellalive.app.ui.animal.AnimalDetailScreenData
import com.huellalive.app.ui.auth.AuthChoiceScreen
import com.huellalive.app.ui.components.BottomNavBar
import com.huellalive.app.ui.human.HumanProfileScreen
import com.huellalive.app.ui.main.MainScreen
import com.huellalive.app.ui.shelter.ShelterDetailScreen
import com.huellalive.app.ui.shelter.ShelterProfileScreen
import com.huellalive.app.ui.theme.*
import com.huellalive.app.ui.video.SelectAnimalForVideoScreen
import org.koin.compose.koinInject

class ExploreScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository = koinInject<ExploreRepository>()
        val sessionManager = koinInject<SessionManager>()
        val viewModel = rememberScreenModel { ExploreViewModel(repository) }
        val state by viewModel.uiState.collectAsState()
        val isLoggedIn = sessionManager.isLoggedIn()
        val userRole = sessionManager.getUserRole()

        Box(Modifier.fillMaxSize().background(Background)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Explorar", style = MaterialTheme.typography.headlineLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Text("Descubre albergues y animales", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        }
                        IconButton(onClick = { viewModel.load() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = DustyRose)
                        }
                    }
                }

                if (state.errorMessage != null) {
                    item { Text(state.errorMessage!!, color = Error, style = MaterialTheme.typography.bodySmall) }
                }

                item { SectionTitle("Albergues cercanos") }
                if (state.shelters.isEmpty()) {
                    item { EmptyCard("No hay albergues para mostrar") }
                } else {
                    items(state.shelters.take(8)) { shelter ->
                        ShelterRow(shelter) { navigator.push(ShelterDetailScreen(shelter.id)) }
                    }
                }

                item { SectionTitle("Animales disponibles") }
                if (state.animals.isEmpty()) {
                    item { EmptyCard("No hay animales disponibles") }
                } else {
                    items(state.animals.take(10)) { animal ->
                        AnimalExploreRow(animal) { navigator.push(AnimalDetailScreenData(animal.id)) }
                    }
                }
            }

            if (state.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center), color = DustyRose)
            }

            BottomNavBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                selectedItem = "explore",
                isLoggedIn = isLoggedIn,
                userRole = userRole,
                onFeedClick = { navigator.replaceAll(MainScreen("feed")) },
                onExploreClick = {},
                onSearchClick = { navigator.replaceAll(MainScreen("search")) },
                onProfileClick = {
                    when (userRole) {
                        "HUMAN" -> navigator.push(HumanProfileScreen())
                        "SHELTER" -> navigator.push(ShelterProfileScreen())
                        else -> navigator.push(AuthChoiceScreen())
                    }
                },
                onLoginClick = { navigator.push(AuthChoiceScreen()) },
                onUploadClick = { navigator.push(SelectAnimalForVideoScreen()) }
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun EmptyCard(text: String) {
    Surface(color = Surface, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Text(text, color = TextSecondary, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun ShelterRow(shelter: ShelterProfileDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Surface).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(DustyRose), contentAlignment = Alignment.Center) {
            if (shelter.user.avatarUrl != null) {
                AsyncImage(shelter.user.avatarUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text(shelter.user.name.take(1).uppercase(), color = TextOnAccent, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(shelter.user.name, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text(shelter.location ?: "Ubicacion pendiente", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.Default.ChevronRight, null, tint = TextTertiary)
    }
}

@Composable
private fun AnimalExploreRow(animal: AnimalDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Surface).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceRaised), contentAlignment = Alignment.Center) {
            if (animal.photoUrl != null) {
                AsyncImage(animal.photoUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Default.Pets, null, tint = DustyRose)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(animal.name, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text("${animal.species}${animal.breed?.let { " - $it" } ?: ""}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.Default.ChevronRight, null, tint = TextTertiary)
    }
}
