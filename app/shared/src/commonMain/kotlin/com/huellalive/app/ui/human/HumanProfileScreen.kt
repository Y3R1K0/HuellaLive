package com.huellalive.app.ui.human

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.huellalive.app.data.model.AnimalDto
import com.huellalive.app.data.model.BadgeDto
import com.huellalive.app.data.model.UserBadgeDto
import com.huellalive.app.data.repository.AnimalRepository
import com.huellalive.app.data.repository.AuthRepository
import com.huellalive.app.data.repository.MediaRepository
import com.huellalive.app.data.repository.UserRepository
import com.huellalive.app.ui.animal.AnimalDetailScreenData
import com.huellalive.app.ui.chat.ChatListScreen
import com.huellalive.app.ui.components.ExpressiveAction
import com.huellalive.app.ui.components.AdjustableImagePreview
import com.huellalive.app.ui.components.AnimalProfileCard
import com.huellalive.app.ui.components.ProfileLoadingSkeleton
import com.huellalive.app.ui.components.HuellaMessageSnackbar
import com.huellalive.app.ui.components.StaggeredReveal
import com.huellalive.app.ui.components.SectionHeading
import com.huellalive.app.ui.feed.VideoHistoryScreen
import com.huellalive.app.ui.notifications.NotificationsScreen
import com.huellalive.app.ui.theme.*
import com.huellalive.app.utils.PickedMedia
import com.huellalive.app.utils.ImageAdjustment
import com.huellalive.app.utils.adjustedPickedImage
import com.huellalive.app.utils.rememberMediaPicker
import com.huellalive.app.utils.versionedMediaUrl
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class HumanProfileScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val userRepo = koinInject<UserRepository>()
        val animalRepo = koinInject<AnimalRepository>()
        val mediaRepo = koinInject<MediaRepository>()
        val authRepo = koinInject<AuthRepository>()
        val viewModel = rememberScreenModel { HumanProfileViewModel(userRepo, animalRepo, mediaRepo, authRepo) }
        val state by viewModel.uiState.collectAsState()
        var showEditDialog by remember { mutableStateOf(false) }
        var showSettingsDrawer by remember { mutableStateOf(false) }
        var showCollectionDialog by remember { mutableStateOf(false) }

        if (showEditDialog && state.user != null) {
            EditHumanProfileDialog(
                name = state.user!!.name,
                avatarUrl = state.user!!.avatarUrl,
                onDismiss = { showEditDialog = false },
                onSave = { name, avatar ->
                    showEditDialog = false
                    viewModel.updateProfile(name, avatar, state.user!!.selectedBadgeId)
                },
                onError = viewModel::setActionMessage
            )
        }

        if (showCollectionDialog && state.user != null) {
            HumanCollectionDialog(
                badges = state.user!!.badges,
                selectedBadgeId = state.user!!.selectedBadgeId,
                onDismiss = { showCollectionDialog = false },
                onSelect = { badgeId ->
                    showCollectionDialog = false
                    viewModel.updateProfile(state.user!!.name, null, badgeId)
                }
            )
        }

        if (state.isLoading && state.user == null) {
            ProfileLoadingSkeleton()
            return
        }

        Box(Modifier.fillMaxSize().background(Background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 136.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(166.dp)) {
                    IconButton(
                        onClick = { showSettingsDrawer = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 22.dp, end = 16.dp)
                            .background(SurfaceHigh, RoundedCornerShape(16.dp))
                            .zIndex(1f)
                    ) {
                        Icon(Icons.Default.Settings, "Configuracion", tint = DustyRose)
                    }
                    val activeBadge = state.user?.badges?.firstOrNull {
                        it.badge.id == state.user?.selectedBadgeId
                    }?.badge
                    ProfileBadgeAvatar(
                        name = state.user?.name.orEmpty(),
                        avatarUrl = state.user?.avatarUrl,
                        badge = activeBadge,
                        mediaRevision = state.mediaRevision,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.user?.name ?: "", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                    Text(state.user?.email ?: "", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    state.user?.badges?.firstOrNull { it.badge.id == state.user?.selectedBadgeId }?.badge?.let { badge ->
                        Spacer(Modifier.height(6.dp))
                        Text(badge.name, color = badgeTierColor(badge.tier), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            item {
                SectionHeading("Mi familia", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
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
                items(state.adoptedAnimals.chunked(2)) { rowAnimals ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowAnimals.forEach { animal ->
                            AdoptedAnimalCard(
                                animal = animal,
                                modifier = Modifier.weight(1f),
                                onClick = { navigator.push(AnimalDetailScreenData(animal.id)) }
                            )
                        }
                        repeat(2 - rowAnimals.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(220.dp)) }
        }
        HuellaMessageSnackbar(
            message = state.actionMessage,
            onDismiss = viewModel::clearActionMessage,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        HumanSettingsSideSheet(
            visible = showSettingsDrawer,
            onDismiss = { showSettingsDrawer = false },
            onEdit = {
                showSettingsDrawer = false
                showEditDialog = true
            },
            onChats = {
                showSettingsDrawer = false
                navigator.push(ChatListScreen())
            },
            onNotifications = {
                showSettingsDrawer = false
                navigator.push(NotificationsScreen())
            },
            onHistory = {
                showSettingsDrawer = false
                navigator.push(VideoHistoryScreen())
            },
            onCollection = {
                showSettingsDrawer = false
                showCollectionDialog = true
            },
            onLogout = {
                showSettingsDrawer = false
                viewModel.logout()
                navigator.popUntilRoot()
            }
        )
        }
    }
}

@Composable
private fun HumanSettingsSideSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onChats: () -> Unit,
    onNotifications: () -> Unit,
    onHistory: () -> Unit,
    onCollection: () -> Unit,
    onLogout: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.fillMaxSize().zIndex(8f),
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(onClick = onDismiss)
            )
            Surface(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 128.dp)
                    .fillMaxHeight()
                    .width(280.dp)
                    .align(Alignment.CenterEnd)
                    .zIndex(9f)
                    .animateEnterExit(
                        enter = slideInHorizontally { it },
                        exit = slideOutHorizontally { it }
                    ),
                color = Surface,
                shape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp),
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Configuracion",
                            modifier = Modifier.weight(1f),
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecondary)
                        }
                    }
                    HumanSettingsButton("Editar perfil", Icons.Default.Edit, DustyRose, onEdit)
                    HumanSettingsButton("Chats", Icons.Default.Chat, DustyRose, onChats)
                    HumanSettingsButton("Avisos", Icons.Default.Notifications, InfoBlue, onNotifications)
                    HumanSettingsButton("Historial", Icons.Default.History, CareLilac, onHistory)
                    HumanSettingsButton("Mi coleccion", Icons.Default.AutoAwesome, BadgeGold, onCollection)
                    Spacer(Modifier.weight(1f))
                    HumanSettingsButton("Cerrar sesion", Icons.Default.Logout, Error, onLogout)
                }
            }
        }
    }
}

@Composable
private fun HumanSettingsButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = color)
        Spacer(Modifier.width(8.dp))
        Text(label, color = color)
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
fun BadgeChip(badge: BadgeDto, selected: Boolean = false, onClick: () -> Unit = {}) {
    val color = badgeAccentColor(badge)
    Surface(
        onClick = onClick,
        color = color.copy(alpha = if (selected) 0.24f else 0.12f),
        shape = RoundedCornerShape(20.dp),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, color) else null
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(badgeIcon(badge), null, tint = color, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(badge.name, style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}

@Composable
private fun HumanCollectionDialog(
    badges: List<UserBadgeDto>,
    selectedBadgeId: String?,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mi colección", color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "Elige la insignia que decorará tu foto de perfil.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Cerrar", tint = TextSecondary)
                    }
                }

                CollectionBadgeRow(
                    badge = null,
                    selected = selectedBadgeId == null,
                    onClick = { onSelect(null) }
                )

                if (badges.isEmpty()) {
                    Surface(color = SurfaceHigh, shape = RoundedCornerShape(18.dp)) {
                        Text(
                            "Aún no tienes insignias desbloqueadas.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(badges) { userBadge ->
                            CollectionBadgeRow(
                                badge = userBadge.badge,
                                selected = selectedBadgeId == userBadge.badge.id,
                                onClick = { onSelect(userBadge.badge.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionBadgeRow(
    badge: BadgeDto?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = badgeAccentColor(badge)
    Surface(
        onClick = onClick,
        color = if (selected) color.copy(alpha = 0.18f) else SurfaceHigh,
        shape = RoundedCornerShape(18.dp),
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, color) else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(42.dp).background(color.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (selected) Icons.Default.CheckCircle else badgeIcon(badge),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(badge?.name ?: "Sin insignia", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Text(
                    badge?.description?.takeIf { it.isNotBlank() } ?: "Mostrar la foto de perfil sin decoración.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun ProfileBadgeAvatar(
    name: String,
    avatarUrl: String?,
    badge: BadgeDto?,
    mediaRevision: Int,
    modifier: Modifier = Modifier
) {
    val color = badgeAccentColor(badge)
    val secondaryColor = badgeSecondaryColor(badge)
    val tierLevel = badgeTierLevel(badge?.tier)
    val frameSize = 124.dp + (tierLevel * 5).dp
    val glowSize = 136.dp + (tierLevel * 7).dp
    val orbitSize = 148.dp + (tierLevel * 6).dp
    val infinite = rememberInfiniteTransition(label = "Badge frame")
    val pulse by infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f + (tierLevel * 0.008f),
        animationSpec = infiniteRepeatable(tween(1350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "Badge pulse"
    )
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween((9800 - tierLevel * 1050).coerceAtLeast(5200), easing = LinearEasing)
        ),
        label = "Badge shine"
    )
    val reverseRotation by infinite.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            tween((12500 - tierLevel * 900).coerceAtLeast(7200), easing = LinearEasing)
        ),
        label = "Badge reverse shine"
    )

    Box(modifier = modifier.size(166.dp), contentAlignment = Alignment.Center) {
        if (badge != null) {
            Box(
                Modifier
                    .size(glowSize)
                    .scale(pulse)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                color.copy(alpha = 0.32f),
                                secondaryColor.copy(alpha = 0.18f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )
            Box(
                Modifier
                    .size(frameSize)
                    .rotate(rotation)
                    .border(
                        width = (3 + tierLevel).dp,
                        brush = Brush.sweepGradient(
                            listOf(
                                color.copy(alpha = 0.22f),
                                color,
                                Color.White.copy(alpha = 0.95f),
                                secondaryColor,
                                color.copy(alpha = 0.42f)
                            )
                        ),
                        shape = CircleShape
                    )
            )
            if (tierLevel >= 2) {
                Box(
                    Modifier
                        .size(orbitSize)
                        .rotate(reverseRotation)
                ) {
                    repeat(tierLevel.coerceAtMost(4)) { index ->
                        Surface(
                            color = if (index % 2 == 0) color else secondaryColor,
                            shape = CircleShape,
                            shadowElevation = (3 + tierLevel).dp,
                            modifier = Modifier
                                .align(
                                    when (index) {
                                        0 -> Alignment.TopCenter
                                        1 -> Alignment.CenterEnd
                                        2 -> Alignment.BottomCenter
                                        else -> Alignment.CenterStart
                                    }
                                )
                                .size((7 + tierLevel).dp)
                        ) {}
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(if (badge == null) DustyRose else color.copy(alpha = 0.28f))
                .border(3.dp, Background, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUrl != null) {
                AsyncImage(
                    model = versionedMediaUrl(avatarUrl, mediaRevision),
                    contentDescription = "Foto de perfil",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(name.firstOrNull()?.uppercase() ?: "U", color = TextOnAccent, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (badge != null) {
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-9).dp, y = (-8).dp),
                color = color,
                shape = CircleShape,
                shadowElevation = (6 + tierLevel).dp
            ) {
                Icon(badgeIcon(badge), badge.name, tint = Background, modifier = Modifier.padding(8.dp).size(21.dp))
            }
        }
    }
}

private fun badgeTierColor(tier: String?) = when (tier) {
    "BRONZE" -> BadgeBronze
    "SILVER" -> BadgeSilver
    "GOLD" -> BadgeGold
    "PLATINUM" -> BadgePlatinum
    "DIAMOND" -> BadgeDiamond
    else -> DustyRose
}

private fun badgeAccentColor(badge: BadgeDto?) = when (badge?.activity) {
    "FOLLOWS" -> InfoBlue
    "DONATIONS" -> DustyRose
    "ADOPTIONS" -> CareLilac
    else -> badgeTierColor(badge?.tier)
}

private fun badgeSecondaryColor(badge: BadgeDto?) = when (badge?.activity) {
    "FOLLOWS" -> Color(0xFF8FE7FF)
    "DONATIONS" -> Color(0xFFFFB1CF)
    "ADOPTIONS" -> Color(0xFFD7B7FF)
    else -> BadgeCream
}

private fun badgeTierLevel(tier: String?) = when (tier) {
    "BRONZE" -> 1
    "SILVER" -> 2
    "GOLD" -> 3
    "PLATINUM" -> 4
    "DIAMOND" -> 5
    else -> 0
}

private fun badgeIcon(badge: BadgeDto?) = when (badge?.activity) {
    "FOLLOWS" -> Icons.Default.Group
    "DONATIONS" -> Icons.Default.Favorite
    "ADOPTIONS" -> Icons.Default.Home
    else -> Icons.Default.AutoAwesome
}

@Composable
private fun AdoptedAnimalCard(animal: AnimalDto, modifier: Modifier, onClick: () -> Unit) {
    AnimalProfileCard(
        animal = animal,
        modifier = modifier.height(190.dp),
        onClick = onClick
    )
}

@Composable
private fun LegacyAdoptedAnimalCard(animal: AnimalDto, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .height(214.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Surface)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f).background(SurfaceRaised),
            contentAlignment = Alignment.Center
        ) {
            if (animal.photoUrl != null) {
                AsyncImage(model = animal.photoUrl, contentDescription = animal.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text(if (animal.species == "Perro") "P" else if (animal.species == "Gato") "G" else "A", fontSize = 28.sp, color = DustyRose)
            }
        }
        Text(
            animal.name,
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 10.dp),
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            animal.breed ?: animal.species,
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 1
        )
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

@Composable
private fun EditHumanProfileDialog(
    name: String,
    avatarUrl: String?,
    onDismiss: () -> Unit,
    onSave: (String, PickedMedia?) -> Unit,
    onError: (String) -> Unit
) {
    var currentName by remember { mutableStateOf(name) }
    var currentAvatar by remember { mutableStateOf<PickedMedia?>(null) }
    var avatarAdjustment by remember { mutableStateOf(ImageAdjustment()) }
    val scope = rememberCoroutineScope()
    val pickAvatar = rememberMediaPicker(
        mimeType = "image/*",
        onPicked = { currentAvatar = it },
        onError = onError
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(8.dp), color = Surface, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Editar perfil", color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                OutlinedButton(
                    onClick = pickAvatar,
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DustyRose.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.PhotoCamera, null, tint = DustyRose)
                    Spacer(Modifier.width(8.dp))
                    Text(currentAvatar?.fileName ?: "Cambiar foto de perfil", color = DustyRose, maxLines = 1)
                }
                AdjustableImagePreview(
                    title = "Foto de perfil",
                    selectedMedia = currentAvatar,
                    currentUrl = avatarUrl,
                    adjustment = avatarAdjustment,
                    onAdjustmentChange = { avatarAdjustment = it },
                    aspectRatio = 1f,
                    shape = CircleShape
                )
                ProfileField("Nombre", currentName) { currentName = it }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                    Button(
                        onClick = {
                            scope.launch {
                                val adjustedAvatar = currentAvatar?.let {
                                    adjustedPickedImage(it, avatarAdjustment, aspectRatio = 1f, outputWidth = 900)
                                }
                                onSave(currentName, adjustedAvatar)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = DustyRose)
                    ) {
                        Text("Guardar", color = TextOnAccent)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditHumanProfileDialog(
    name: String,
    avatarUrl: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var currentName by remember { mutableStateOf(name) }
    var currentAvatar by remember { mutableStateOf(avatarUrl) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(8.dp), color = Surface, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Editar perfil", color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                ProfileField("Nombre", currentName) { currentName = it }
                ProfileField("URL de foto", currentAvatar) { currentAvatar = it }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                    Button(onClick = { onSave(currentName, currentAvatar) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = DustyRose)) {
                        Text("Guardar", color = TextOnAccent)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
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
