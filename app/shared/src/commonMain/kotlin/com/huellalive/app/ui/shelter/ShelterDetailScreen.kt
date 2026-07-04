package com.huellalive.app.ui.shelter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
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
import com.huellalive.app.data.repository.EngagementRepository
import com.huellalive.app.data.repository.ShelterRepository
import com.huellalive.app.data.repository.WalletRepository
import com.huellalive.app.ui.animal.AnimalDetailScreenData
import com.huellalive.app.ui.auth.AuthChoiceScreen
import com.huellalive.app.ui.components.HuellaLoadingIndicator
import com.huellalive.app.ui.components.ProfileLoadingSkeleton
import com.huellalive.app.ui.components.HuellaMessageSnackbar
import com.huellalive.app.ui.components.StaggeredReveal
import com.huellalive.app.ui.components.SectionHeading
import com.huellalive.app.ui.components.TonalChip
import com.huellalive.app.ui.theme.*
import org.koin.compose.koinInject

data class ShelterDetailScreen(val shelterId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val shelterRepo = koinInject<ShelterRepository>()
        val engagementRepo = koinInject<EngagementRepository>()
        val walletRepo = koinInject<WalletRepository>()
        val sessionManager = koinInject<SessionManager>()
        val uriHandler = LocalUriHandler.current
        var showDonateDialog by remember { mutableStateOf(false) }
        val viewModel = rememberScreenModel(shelterId) {
            ShelterDetailViewModel(shelterId, shelterRepo, engagementRepo, walletRepo, sessionManager)
        }
        val state by viewModel.uiState.collectAsState()
        val isLoggedIn = sessionManager.isLoggedIn()
        val isHuman = sessionManager.isHuman()
        val isOwnShelter = sessionManager.isShelter() &&
            state.shelter?.userId == sessionManager.getUserId()

        LaunchedEffect(isOwnShelter) {
            if (isOwnShelter) {
                navigator.replace(ShelterProfileScreen())
            }
        }

        if (showDonateDialog) {
            DonateDialog(
                onDismiss = { showDonateDialog = false },
                onDonate = { amount ->
                    showDonateDialog = false
                    viewModel.donate(amount)
                }
            )
        }

        LaunchedEffect(state.checkoutUrl) {
            val checkoutUrl = state.checkoutUrl ?: return@LaunchedEffect
            val opened = runCatching {
                uriHandler.openUri(checkoutUrl)
                true
            }.getOrDefault(false)
            viewModel.clearCheckoutUrl()
            if (!opened) {
                viewModel.showActionMessage("No se pudo abrir Mercado Pago en este dispositivo")
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(Background)) {
            when {
                state.isLoading -> ProfileLoadingSkeleton()
                state.errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(horizontal = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = DustyRose,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            state.errorMessage ?: "No se pudo cargar el albergue",
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = viewModel::loadShelter,
                            border = androidx.compose.foundation.BorderStroke(1.dp, DustyRose.copy(alpha = 0.55f))
                        ) {
                            Text("Reintentar", color = DustyRose)
                        }
                    }
                }
                state.shelter != null -> {
                    val shelter = state.shelter!!
                    val listState = rememberLazyListState()
                    var stretchOffset by remember { mutableStateOf(0f) }
                    val animatedStretch by animateFloatAsState(
                        targetValue = stretchOffset,
                        animationSpec = spring(stiffness = 420f, dampingRatio = 0.72f),
                        label = "publicShelterStretch"
                    )
                    LaunchedEffect(listState.isScrollInProgress) {
                        if (!listState.isScrollInProgress) {
                            stretchOffset = 0f
                        }
                    }
                    val stretchConnection = remember(listState) {
                        object : NestedScrollConnection {
                            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                                val atTop = listState.firstVisibleItemIndex == 0 &&
                                    listState.firstVisibleItemScrollOffset == 0
                                if (source == NestedScrollSource.UserInput && atTop && available.y > 0f) {
                                    stretchOffset = (stretchOffset + available.y * 0.42f).coerceIn(0f, 170f)
                                } else if (source == NestedScrollSource.UserInput && available.y < 0f && stretchOffset > 0f) {
                                    stretchOffset = (stretchOffset + available.y * 0.8f).coerceAtLeast(0f)
                                }
                                return Offset.Zero
                            }
                        }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .nestedScroll(stretchConnection)
                            .graphicsLayer {
                                translationY = animatedStretch * 0.28f
                                scaleY = 1f + (animatedStretch / 3600f)
                            },
                        state = listState,
                        contentPadding = PaddingValues(bottom = 136.dp)
                    ) {

                        // Cover + Avatar + back
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(292.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(236.dp)
                                        .clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
                                        .background(SurfaceRaised)
                                )
                                if (shelter.coverUrl != null) {
                                    AsyncImage(
                                        model = shelter.coverUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxWidth().height(236.dp).clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
                                    )
                                } else {
                                    Box(Modifier.fillMaxWidth().height(236.dp), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Home, null, tint = DustyRose.copy(alpha = 0.45f), modifier = Modifier.size(64.dp))
                                    }
                                }


                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(start = 18.dp, bottom = 4.dp)
                                        .size(88.dp)
                                        .clip(RoundedCornerShape(26.dp))
                                        .background(DustyRose),
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
                            Spacer(Modifier.height(10.dp))
                            Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        shelter.user.name,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TonalChip("Verificado", color = AdoptionGreen, icon = Icons.Default.Verified)
                                }
                                if (shelter.location != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                                        Text(shelter.location, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                }
                                if (shelter.description != null) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(shelter.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, lineHeight = 20.sp)
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
                                    onClick = {
                                        when {
                                            isHuman && !shelter.isFollowing -> viewModel.follow()
                                            isHuman -> viewModel.showActionMessage("Ya sigues este albergue")
                                            !isLoggedIn -> navigator.push(AuthChoiceScreen())
                                            else -> viewModel.showActionMessage("Seguir albergues esta disponible para cuentas humanas")
                                        }
                                    },
                                    enabled = state.actionInProgress == null,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DustyRose.copy(alpha = 0.5f))
                                ) {
                                    if (state.actionInProgress == "follow") {
                                        CircularProgressIndicator(Modifier.size(17.dp), color = DustyRose, strokeWidth = 2.dp)
                                    } else {
                                        Icon(
                                            if (shelter.isFollowing) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            null,
                                            modifier = Modifier.size(16.dp),
                                            tint = DustyRose
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (shelter.isFollowing) "Siguiendo" else "Seguir", color = DustyRose)
                                    }
                                }
                                OutlinedButton(
                                    onClick = {
                                        when {
                                            isHuman -> showDonateDialog = true
                                            !isLoggedIn -> navigator.push(AuthChoiceScreen())
                                            else -> viewModel.showActionMessage("Las donaciones se realizan desde una cuenta humana")
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PeachWarm.copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Default.VolunteerActivism, null, modifier = Modifier.size(16.dp), tint = PeachWarm)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Donar", color = PeachWarm)
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                        }

                        val shelterStories = shelter.stories
                        if (shelterStories.isNotEmpty()) {
                            item {
                                ShelterStoriesSection(
                                    title = "Historias de albergue",
                                    videos = shelterStories,
                                    onSeeMore = {
                                        navigator.push(
                                            ShelterStoriesScreen(
                                                title = "Historias de ${shelter.user.name}",
                                                videos = shelterStories
                                            )
                                        )
                                    }
                                )
                                Spacer(Modifier.height(18.dp))
                            }
                        }

                        // Animales disponibles
                        item {
                            SectionHeading(
                                "Animales que buscan hogar",
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                            )
                        }

                        if (shelter.animals.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("No hay animales disponibles", color = TextSecondary)
                                }
                            }
                        } else {
                            items(shelter.animals.chunked(3)) { rowAnimals ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowAnimals.forEach { animal ->
                                        ShelterAnimalGridCard(
                                            animal = animal,
                                            modifier = Modifier.weight(1f),
                                            onClick = { navigator.push(AnimalDetailScreenData(animal.id)) }
                                        )
                                    }
                                    repeat(3 - rowAnimals.size) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        item { Spacer(Modifier.height(220.dp)) }
                    }
                }
            }
            HuellaMessageSnackbar(
                message = state.actionMessage,
                onDismiss = viewModel::clearActionMessage,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun DonateDialog(onDismiss: () -> Unit, onDonate: (Double) -> Unit) {
    var amount by remember { mutableStateOf("10") }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = { onDonate(amount.toDoubleOrNull() ?: 0.0) },
                colors = ButtonDefaults.buttonColors(containerColor = DustyRose)
            ) {
                Text("Donar", color = TextOnAccent)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancelar") }
        },
        title = { Text("Donar al albergue", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "El pago se completara en Mercado Pago. HuellaLive registra la donacion cuando el pago sea aprobado.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("10", "20", "50").forEach { value ->
                        FilterChip(
                            selected = amount == value,
                            onClick = { amount = value },
                            label = { Text("S/ $value") }
                        )
                    }
                }
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Aporte voluntario S/") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = DustyRose,
                        unfocusedBorderColor = Outline,
                        focusedLabelColor = DustyRose,
                        unfocusedLabelColor = TextSecondary
                    )
                )
            }
        },
        containerColor = Surface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary
    )
}
