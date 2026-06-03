package com.huellalive.app.ui.explore

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
import com.huellalive.app.data.model.RankingItemDto
import com.huellalive.app.data.model.ShelterProfileDto
import com.huellalive.app.data.repository.SearchRepository
import com.huellalive.app.data.repository.ShelterRepository
import com.huellalive.app.ui.human.BadgeChip
import com.huellalive.app.ui.shelter.ShelterDetailScreen
import com.huellalive.app.ui.theme.*
import org.koin.compose.koinInject

class ExploreScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val shelterRepo = koinInject<ShelterRepository>()
        val searchRepo = koinInject<SearchRepository>()
        val viewModel = rememberScreenModel { ExploreViewModel(shelterRepo, searchRepo) }
        val state by viewModel.uiState.collectAsState()

        // Popup ranking user
        if (state.selectedRankingUser != null) {
            RankingUserPopup(
                item = state.selectedRankingUser!!,
                onDismiss = { viewModel.selectRankingUser(null) }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
        ) {
            // Header
            item {
                Text(
                    "Explorar",
                    style = MaterialTheme.typography.headlineLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, top = 56.dp, bottom = 16.dp)
                )
            }

            // Top donadores
            item {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🏆 Top Donadores", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                        Text("Esta semana", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }

                    if (state.ranking.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Aún no hay donaciones esta semana", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.ranking) { item ->
                                RankingCard(
                                    item = item,
                                    onClick = { viewModel.selectRankingUser(item) }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }

            // Albergues cercanos header
            item {
                Text(
                    "📍 Albergues cercanos",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (state.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = DustyRose)
                    }
                }
            } else if (state.shelters.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay albergues registrados aún", color = TextSecondary)
                    }
                }
            } else {
                items(state.shelters) { shelter ->
                    ShelterExploreCard(
                        shelter = shelter,
                        onClick = { navigator.push(ShelterDetailScreen(shelter.id)) }
                    )
                }
            }

            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun RankingCard(item: RankingItemDto, onClick: () -> Unit) {
    val medal = when (item.rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> "#${item.rank}"
    }

    Column(
        modifier = Modifier
            .width(100.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(DustyRose),
            contentAlignment = Alignment.Center
        ) {
            if (item.user.avatarUrl != null) {
                AsyncImage(
                    model = item.user.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    item.user.name.first().toString().uppercase(),
                    color = TextOnAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(medal, fontSize = 16.sp)
        Text(
            item.user.name.split(" ").first(),
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        Text(
            "S/ ${String.format("%.0f", item.totalDonated)}",
            style = MaterialTheme.typography.labelSmall,
            color = PeachWarm
        )
    }
}

@Composable
private fun ShelterExploreCard(shelter: ShelterProfileDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(DustyRose),
            contentAlignment = Alignment.Center
        ) {
            if (shelter.user.avatarUrl != null) {
                AsyncImage(
                    model = shelter.user.avatarUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    shelter.user.name.first().toString().uppercase(),
                    color = TextOnAccent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(shelter.user.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            if (shelter.location != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = TextTertiary, modifier = Modifier.size(12.dp))
                    Text(shelter.location, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
            if (shelter.description != null) {
                Text(
                    shelter.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    maxLines = 1
                )
            }
        }
        Icon(Icons.Default.ChevronRight, null, tint = TextTertiary)
    }
}

@Composable
private fun RankingUserPopup(item: RankingItemDto, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar grande
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(DustyRose),
                    contentAlignment = Alignment.Center
                ) {
                    if (item.user.avatarUrl != null) {
                        AsyncImage(
                            model = item.user.avatarUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            item.user.name.first().toString().uppercase(),
                            color = TextOnAccent,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(item.user.name, style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(
                    when (item.rank) { 1 -> "🥇 #1 Donador" 2 -> "🥈 #2 Donador" 3 -> "🥉 #3 Donador" else -> "#${item.rank} Donador" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = PeachWarm
                )

                Spacer(Modifier.height(20.dp))

                // Stats grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PopupStat("Donado", "S/ ${String.format("%.0f", item.totalDonated)}", PeachWarm)
                    PopupStat("Albergues", "${item.sheltersHelped}", MintCream)
                    PopupStat("Adoptados", "${item.animalsAdopted}", SkyPowder)
                }

                // Badges
                if (item.user.badges.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(item.user.badges) { userBadge ->
                            BadgeChip(tier = userBadge.badge.tier, name = userBadge.badge.name)
                        }
                    }
                }

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

@Composable
private fun PopupStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}