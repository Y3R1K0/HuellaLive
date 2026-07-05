package com.huellalive.app.ui.shelter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.huellalive.app.data.model.GeocodingResultDto
import com.huellalive.app.data.model.VideoDto
import com.huellalive.app.data.repository.AnimalRepository
import com.huellalive.app.data.repository.AuthRepository
import com.huellalive.app.data.repository.EngagementRepository
import com.huellalive.app.data.repository.MediaRepository
import com.huellalive.app.data.repository.ShelterRepository
import com.huellalive.app.data.repository.UserRepository
import com.huellalive.app.ui.adoption.AdoptionRequestsScreen
import com.huellalive.app.ui.animal.AnimalDetailScreenData
import com.huellalive.app.ui.animal.CreateAnimalScreen
import com.huellalive.app.ui.chat.ChatListScreen
import com.huellalive.app.ui.components.ExpressiveAction
import com.huellalive.app.ui.components.AdjustableImagePreview
import com.huellalive.app.ui.components.AnimalProfileCard
import com.huellalive.app.ui.components.ConfirmDeleteDialog
import com.huellalive.app.ui.components.ProfileLoadingSkeleton
import com.huellalive.app.ui.components.HuellaMessageSnackbar
import com.huellalive.app.ui.components.StaggeredReveal
import com.huellalive.app.ui.components.SectionHeading
import com.huellalive.app.ui.components.TonalChip
import com.huellalive.app.ui.components.VideoPlayer
import com.huellalive.app.ui.location.GeoPoint
import com.huellalive.app.ui.location.rememberCurrentLocationRequester
import com.huellalive.app.ui.map.ShelterLocationPicker
import com.huellalive.app.ui.notifications.NotificationsScreen
import com.huellalive.app.ui.feed.VideoHistoryScreen
import com.huellalive.app.ui.video.UploadVideoScreen
import com.huellalive.app.ui.video.UploadVideoTarget
import com.huellalive.app.ui.theme.*
import com.huellalive.app.utils.PickedMedia
import com.huellalive.app.utils.ImageAdjustment
import com.huellalive.app.utils.Resource
import com.huellalive.app.utils.adjustedPickedImage
import com.huellalive.app.utils.rememberMediaPicker
import com.huellalive.app.utils.versionedMediaUrl
import com.huellalive.app.utils.cloudinaryVideoPosterUrl
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import androidx.compose.ui.zIndex

class ShelterProfileScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val shelterRepo = koinInject<ShelterRepository>()
        val animalRepo = koinInject<AnimalRepository>()
        val mediaRepo = koinInject<MediaRepository>()
        val userRepo = koinInject<UserRepository>()
        val engagementRepo = koinInject<EngagementRepository>()
        val authRepo = koinInject<AuthRepository>()
        val viewModel = rememberScreenModel { ShelterProfileViewModel(shelterRepo, animalRepo, mediaRepo, userRepo, engagementRepo, authRepo) }
        val state by viewModel.uiState.collectAsState()
        var showEditDialog by remember { mutableStateOf(false) }
        var showSettingsDrawer by remember { mutableStateOf(false) }

        if (showEditDialog && state.shelter != null) {
            EditShelterProfileDialog(
                name = state.shelter!!.user.name,
                description = state.shelter!!.description.orEmpty(),
                location = state.shelter!!.location.orEmpty(),
                latitude = state.shelter!!.latitude?.toString().orEmpty(),
                longitude = state.shelter!!.longitude?.toString().orEmpty(),
                avatarUrl = state.shelter!!.user.avatarUrl,
                coverUrl = state.shelter!!.coverUrl,
                onDismiss = { showEditDialog = false },
                onSave = { name, description, location, latitude, longitude, avatar, cover ->
                    showEditDialog = false
                    viewModel.updateProfile(name, description, location, latitude, longitude, avatar, cover)
                },
                onError = viewModel::setActionMessage
            )
        }

        if (state.isLoading && state.shelter == null) {
            ProfileLoadingSkeleton()
            return
        }

        Box(modifier = Modifier.fillMaxSize().background(Background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 136.dp)
        ) {

            // Cover + Avatar

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(292.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(236.dp)
                            .clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
                            .background(SurfaceRaised)
                    )
                    IconButton(
                        onClick = { showSettingsDrawer = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 40.dp, end = 14.dp)
                            .background(Color.Black.copy(alpha = 0.48f), RoundedCornerShape(16.dp))
                            .zIndex(1f)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Configuracion", tint = Color.White)
                    }
                    if (state.shelter?.coverUrl != null) {
                        AsyncImage(
                            model = versionedMediaUrl(state.shelter!!.coverUrl, state.mediaRevision),
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
                        if (state.shelter?.user?.avatarUrl != null) {
                            AsyncImage(
                                model = versionedMediaUrl(state.shelter!!.user.avatarUrl, state.mediaRevision),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(state.shelter?.user?.name?.first()?.toString()?.uppercase() ?: "A", color = TextOnAccent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (state.shelter?.status == "PENDING") {
                        Surface(modifier = Modifier.align(Alignment.TopEnd).padding(top = 92.dp, end = 12.dp), color = Warning.copy(alpha = 0.9f), shape = RoundedCornerShape(8.dp)) {
                            Text("Pendiente de aprobación", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = TextOnAccent)
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
                            state.shelter?.user?.name ?: "",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        if (state.shelter?.status == "APPROVED") {
                            TonalChip("Verificado", color = AdoptionGreen, icon = Icons.Default.Verified)
                        }
                    }
                    if (state.shelter?.location != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                            Text(state.shelter!!.location!!, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                    if (state.shelter?.description != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(state.shelter!!.description!!, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, lineHeight = 20.sp)
                    }
                }
            }

            // Acciones
            item {
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExpressiveAction(
                        label = "Chats",
                        icon = Icons.Default.Chat,
                        onClick = { navigator.push(ChatListScreen()) },
                        modifier = Modifier.weight(1f)
                    )
                    ExpressiveAction(
                        label = "Avisos",
                        icon = Icons.Default.Notifications,
                        onClick = { navigator.push(NotificationsScreen()) },
                        modifier = Modifier.weight(1f),
                        accent = InfoBlue
                    )
                }
                Spacer(Modifier.height(20.dp))
            }

            val shelterStories = state.shelter?.stories.orEmpty()
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeading("Historias de albergue", modifier = Modifier.weight(1f))
                    OutlinedButton(
                        onClick = { navigator.push(UploadVideoScreen(target = UploadVideoTarget.ShelterStory)) },
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, PeachWarm.copy(alpha = 0.55f))
                    ) {
                        Icon(Icons.Default.Movie, null, modifier = Modifier.size(16.dp), tint = PeachWarm)
                        Spacer(Modifier.width(6.dp))
                        Text("Subir", color = PeachWarm)
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (shelterStories.isNotEmpty()) {
                    ShelterStoriesSection(
                        title = null,
                        videos = shelterStories,
                        onDeleteStory = viewModel::deleteShelterStory,
                        onSeeMore = {
                            navigator.push(
                                ShelterStoriesScreen(
                                    title = "Historias de ${state.shelter?.user?.name.orEmpty()}",
                                    videos = shelterStories,
                                    canDelete = true
                                )
                            )
                        }
                    )
                } else {
                    Surface(
                        color = Surface,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp)
                    ) {
                        Text(
                            "Sube videos del albergue para mostrar rescates, avances y momentos del refugio.",
                            color = TextSecondary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
            }

            // Header animales
            item {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    SectionHeading("Mis animales", modifier = Modifier.weight(1f))
                    Button(
                        onClick = { navigator.push(CreateAnimalScreen()) },
                        modifier = Modifier.height(42.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DustyRose)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp), tint = TextOnAccent)
                        Spacer(Modifier.width(4.dp))
                        Text("Crear", color = TextOnAccent, style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (state.animals.isEmpty()) {
                item { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("No tienes animales registrados", color = TextSecondary) } }
            } else {
                items(state.animals.chunked(3)) { rowAnimals ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowAnimals.forEach { animal ->
                            ShelterAnimalGridCard(
                                animal = animal,
                                mediaRevision = state.mediaRevision,
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
        SettingsSideSheet(
                visible = showSettingsDrawer,
                onDismiss = { showSettingsDrawer = false },
                onEdit = {
                    showSettingsDrawer = false
                    showEditDialog = true
                },
                onAdoptions = {
                    showSettingsDrawer = false
                    navigator.push(AdoptionRequestsScreen())
                },
                onWallet = {
                    showSettingsDrawer = false
                    navigator.push(ShelterWalletScreen())
                },
                onHistory = {
                    showSettingsDrawer = false
                    navigator.push(VideoHistoryScreen())
                },
                onLogout = {
                    showSettingsDrawer = false
                    viewModel.logout()
                    navigator.popUntilRoot()
                }
            )
        HuellaMessageSnackbar(
            message = state.actionMessage,
            onDismiss = viewModel::clearActionMessage,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
}

@Composable
private fun SettingsSideSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onAdoptions: () -> Unit,
    onWallet: () -> Unit,
    onHistory: () -> Unit,
    onLogout: () -> Unit
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = visible,
        modifier = Modifier.fillMaxSize().zIndex(4f),
        enter = androidx.compose.animation.fadeIn(),
        exit = androidx.compose.animation.fadeOut()
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
                .padding(bottom = 96.dp)
                .fillMaxHeight()
                .width(280.dp)
                .align(Alignment.CenterEnd)
                .animateEnterExit(
                    enter = androidx.compose.animation.slideInHorizontally { it },
                    exit = androidx.compose.animation.slideOutHorizontally { it }
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
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DustyRose.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp), tint = DustyRose)
                    Spacer(Modifier.width(8.dp))
                    Text("Editar perfil", color = DustyRose)
                }
                OutlinedButton(
                    onClick = onAdoptions,
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PeachWarm.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Favorite, null, modifier = Modifier.size(16.dp), tint = PeachWarm)
                    Spacer(Modifier.width(8.dp))
                    Text("Solicitudes de adopcion", color = PeachWarm)
                }
                OutlinedButton(
                    onClick = onWallet,
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AdoptionGreen.copy(alpha = 0.55f))
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, null, modifier = Modifier.size(16.dp), tint = AdoptionGreen)
                    Spacer(Modifier.width(8.dp))
                    Text("Donaciones y retiros", color = AdoptionGreen)
                }
                OutlinedButton(
                    onClick = onHistory,
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, InfoBlue.copy(alpha = 0.55f))
                ) {
                    Icon(Icons.Default.History, null, modifier = Modifier.size(16.dp), tint = InfoBlue)
                    Spacer(Modifier.width(8.dp))
                    Text("Historial de videos", color = InfoBlue)
                }
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Error.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Logout, null, modifier = Modifier.size(16.dp), tint = Error)
                    Spacer(Modifier.width(8.dp))
                    Text("Cerrar sesion", color = Error)
                }
            }
        }
    }
}
}

@Composable
private fun EditShelterProfileDialog(
    name: String,
    description: String,
    location: String,
    latitude: String,
    longitude: String,
    avatarUrl: String?,
    coverUrl: String?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, PickedMedia?, PickedMedia?) -> Unit,
    onError: (String) -> Unit
) {
    val shelterRepository = koinInject<ShelterRepository>()
    val scope = rememberCoroutineScope()
    var currentName by remember { mutableStateOf(name) }
    var currentDescription by remember { mutableStateOf(description) }
    var currentLocation by remember { mutableStateOf(location) }
    var addressQuery by remember { mutableStateOf(location) }
    var selectedPoint by remember {
        mutableStateOf(
            latitude.toDoubleOrNull()?.let { lat ->
                longitude.toDoubleOrNull()?.let { lng -> GeoPoint(lat, lng) }
            }
        )
    }
    var searchResults by remember { mutableStateOf<List<GeocodingResultDto>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var isResolvingPoint by remember { mutableStateOf(false) }
    var isPointConfirmed by remember { mutableStateOf(selectedPoint != null && location.isNotBlank()) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var currentAvatar by remember { mutableStateOf<PickedMedia?>(null) }
    var currentCover by remember { mutableStateOf<PickedMedia?>(null) }
    var avatarAdjustment by remember { mutableStateOf(ImageAdjustment()) }
    var coverAdjustment by remember { mutableStateOf(ImageAdjustment()) }
    val pickAvatar = rememberMediaPicker(
        mimeType = "image/*",
        onPicked = { currentAvatar = it },
        onError = onError
    )
    val pickCover = rememberMediaPicker(
        mimeType = "image/*",
        onPicked = { currentCover = it },
        onError = onError
    )
    val requestCurrentLocation = rememberCurrentLocationRequester(
        onLocation = { point ->
            selectedPoint = point
            isPointConfirmed = false
            isResolvingPoint = true
            locationError = null
            scope.launch {
                when (val result = shelterRepository.reverseLocation(point.latitude, point.longitude)) {
                    is Resource.Success -> {
                        currentLocation = result.data.displayName
                        addressQuery = result.data.displayName
                        isPointConfirmed = true
                    }
                    is Resource.Error -> locationError = result.message
                    else -> Unit
                }
                isResolvingPoint = false
            }
        },
        onError = onError
    )

    fun searchAddress() {
        if (addressQuery.trim().length < 3) {
            locationError = "Escribe al menos 3 caracteres"
            return
        }
        isSearching = true
        locationError = null
        searchResults = emptyList()
        scope.launch {
            when (val result = shelterRepository.searchLocations(addressQuery.trim())) {
                is Resource.Success -> {
                    searchResults = result.data
                    if (result.data.isEmpty()) locationError = "No encontramos esa direccion"
                }
                is Resource.Error -> locationError = result.message
                else -> Unit
            }
            isSearching = false
        }
    }

    fun confirmSelectedPoint() {
        val point = selectedPoint ?: return
        isResolvingPoint = true
        locationError = null
        scope.launch {
            when (val result = shelterRepository.reverseLocation(point.latitude, point.longitude)) {
                is Resource.Success -> {
                    currentLocation = result.data.displayName
                    addressQuery = result.data.displayName
                    isPointConfirmed = true
                }
                is Resource.Error -> locationError = result.message
                else -> Unit
            }
            isResolvingPoint = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Surface,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.94f)
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Editar albergue",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Cerrar", tint = TextSecondary)
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                OutlinedButton(
                    onClick = pickAvatar,
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DustyRose.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.AccountCircle, null, tint = DustyRose)
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
                    shape = RoundedCornerShape(26.dp)
                )
                OutlinedButton(
                    onClick = pickCover,
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DustyRose.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Image, null, tint = DustyRose)
                    Spacer(Modifier.width(8.dp))
                    Text(currentCover?.fileName ?: "Cambiar portada", color = DustyRose, maxLines = 1)
                }
                AdjustableImagePreview(
                    title = "Portada",
                    selectedMedia = currentCover,
                    currentUrl = coverUrl,
                    adjustment = coverAdjustment,
                    onAdjustmentChange = { coverAdjustment = it },
                    aspectRatio = 16f / 7f,
                    shape = RoundedCornerShape(22.dp)
                )
                ShelterField("Nombre del albergue", currentName) { currentName = it }
                ShelterField("Breve descripcion", currentDescription) { currentDescription = it }

                Text(
                    "Ubicacion del albergue",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = addressQuery,
                        onValueChange = {
                            addressQuery = it
                            searchResults = emptyList()
                        },
                        placeholder = { Text("Ej. Cercado, Arequipa") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, null) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = DustyRose,
                            unfocusedBorderColor = Outline
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = ::searchAddress,
                        enabled = !isSearching,
                        modifier = Modifier.background(DustyRose, RoundedCornerShape(8.dp))
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = TextOnAccent,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Search, "Buscar direccion", tint = TextOnAccent)
                        }
                    }
                }

                searchResults.forEach { result ->
                    Surface(
                        onClick = {
                            selectedPoint = GeoPoint(result.latitude, result.longitude)
                            currentLocation = result.displayName
                            addressQuery = result.displayName
                            searchResults = emptyList()
                            isPointConfirmed = true
                            locationError = null
                        },
                        color = SurfaceRaised,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = DustyRose)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                result.displayName,
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = requestCurrentLocation,
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DustyRose.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.MyLocation, null, tint = DustyRose)
                    Spacer(Modifier.width(8.dp))
                    Text("Usar mi ubicacion actual", color = DustyRose)
                }

                ShelterLocationPicker(
                    selectedPoint = selectedPoint,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) { point ->
                    selectedPoint = point
                    isPointConfirmed = false
                    locationError = null
                }
                Text(
                    "Toca el mapa para mover el marcador hasta la entrada del albergue.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )

                if (selectedPoint != null && !isPointConfirmed) {
                    Button(
                        onClick = ::confirmSelectedPoint,
                        enabled = !isResolvingPoint,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = DustyRose)
                    ) {
                        if (isResolvingPoint) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = TextOnAccent,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Confirmar punto del mapa", color = TextOnAccent)
                        }
                    }
                }

                if (isPointConfirmed && currentLocation.isNotBlank()) {
                    Surface(
                        color = DustyRose.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = Success)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                currentLocation,
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                locationError?.let { Text(it, color = Error, style = MaterialTheme.typography.bodySmall) }
                Spacer(Modifier.height(6.dp))
            }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            val point = selectedPoint
                            if (point == null || !isPointConfirmed) {
                                locationError = "Confirma la ubicacion antes de guardar"
                            } else {
                                scope.launch {
                                    val adjustedAvatar = currentAvatar?.let {
                                        adjustedPickedImage(it, avatarAdjustment, aspectRatio = 1f, outputWidth = 900)
                                    }
                                    val adjustedCover = currentCover?.let {
                                        adjustedPickedImage(it, coverAdjustment, aspectRatio = 16f / 7f, outputWidth = 1400)
                                    }
                                    onSave(
                                        currentName,
                                        currentDescription,
                                        currentLocation,
                                        point.latitude.toString(),
                                        point.longitude.toString(),
                                        adjustedAvatar,
                                        adjustedCover
                                    )
                                }
                            }
                        },
                        enabled = currentName.isNotBlank() &&
                            selectedPoint != null &&
                            isPointConfirmed &&
                            !isResolvingPoint,
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
private fun ShelterField(label: String, value: String, modifier: Modifier = Modifier.fillMaxWidth(), onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = modifier,
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

@Composable
internal fun ShelterAnimalGridCard(
    animal: AnimalDto,
    mediaRevision: Int = 0,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    AnimalProfileCard(
        animal = animal,
        mediaRevision = mediaRevision,
        modifier = modifier.height(158.dp),
        onClick = onClick
    )
}

internal fun List<AnimalDto>.shelterStoryVideos(): List<VideoDto> =
    flatMap { animal ->
        animal.videos.map { video ->
            video to video.createdAt
        }
    }
        .sortedByDescending { it.second }
        .map { it.first }

@Composable
internal fun ShelterStoriesSection(
    title: String?,
    videos: List<VideoDto>,
    onDeleteStory: ((String) -> Unit)? = null,
    onSeeMore: () -> Unit
) {
    var selectedVideo by remember { mutableStateOf<VideoDto?>(null) }
    var storyToDelete by remember { mutableStateOf<VideoDto?>(null) }

    selectedVideo?.let { video ->
        ShelterStoryViewer(
            video = video,
            canDelete = onDeleteStory != null,
            onDismiss = { selectedVideo = null },
            onDelete = { storyToDelete = video }
        )
    }

    storyToDelete?.let { video ->
        ConfirmDeleteDialog(
            title = "Eliminar historia",
            message = "Esta historia se borrara del perfil del albergue y del feed.",
            onDismiss = { storyToDelete = null },
            onConfirm = {
                storyToDelete = null
                selectedVideo = null
                onDeleteStory?.invoke(video.id)
            }
        )
    }

    Column(Modifier.fillMaxWidth()) {
        if (title != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeading(title, modifier = Modifier.weight(1f))
                TextButton(onClick = onSeeMore) {
                    Text("Ver mas", color = DustyRose, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ChevronRight, null, tint = DustyRose, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onSeeMore) {
                    Text("Ver mas", color = DustyRose, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.ChevronRight, null, tint = DustyRose, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            videos.take(3).forEach { video ->
                ShelterStoryCard(
                    video = video,
                    modifier = Modifier.weight(1f).height(146.dp),
                    canDelete = onDeleteStory != null,
                    onDelete = { storyToDelete = video },
                    onClick = { selectedVideo = video }
                )
            }
            repeat(3 - videos.take(3).size) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

data class ShelterStoriesScreen(
    private val title: String,
    private val videos: List<VideoDto>,
    private val canDelete: Boolean = false
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val engagementRepository = koinInject<com.huellalive.app.data.repository.EngagementRepository>()
        val scope = rememberCoroutineScope()
        var visibleVideos by remember { mutableStateOf(videos) }
        var selectedVideo by remember { mutableStateOf<VideoDto?>(null) }
        var storyToDelete by remember { mutableStateOf<VideoDto?>(null) }

        selectedVideo?.let { video ->
            ShelterStoryViewer(
                video = video,
                canDelete = canDelete,
                onDismiss = { selectedVideo = null },
                onDelete = { storyToDelete = video }
            )
        }

        storyToDelete?.let { video ->
            ConfirmDeleteDialog(
                title = "Eliminar historia",
                message = "Esta historia se borrara del perfil del albergue y del feed.",
                onDismiss = { storyToDelete = null },
                onConfirm = {
                    storyToDelete = null
                    selectedVideo = null
                    scope.launch {
                        when (engagementRepository.deleteShelterStory(video.id)) {
                            is Resource.Success -> {
                                visibleVideos = visibleVideos.filterNot { it.id == video.id }
                                ShelterStoryUpdateSignal.delete(video.id)
                            }
                            else -> Unit
                        }
                    }
                }
            )
        }

        Column(Modifier.fillMaxSize().background(Background).statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 18.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navigator.pop() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                }
                Text(
                    title,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }

            if (visibleVideos.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aun no hay historias", color = TextSecondary, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 132.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visibleVideos.chunked(2)) { rowVideos ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            rowVideos.forEach { video ->
                                ShelterStoryCard(
                                    video = video,
                                    modifier = Modifier.weight(1f).height(210.dp),
                                    canDelete = canDelete,
                                    onDelete = { storyToDelete = video },
                                    onClick = { selectedVideo = video }
                                )
                            }
                            if (rowVideos.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShelterStoryCard(
    video: VideoDto,
    modifier: Modifier = Modifier,
    canDelete: Boolean = false,
    onDelete: () -> Unit = {},
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceRaised)
            .clickable(onClick = onClick)
    ) {
        val posterUrl = video.thumbnailUrl ?: cloudinaryVideoPosterUrl(video.videoUrl)
        if (posterUrl != null) {
            AsyncImage(
                model = posterUrl,
                contentDescription = video.description,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(Icons.Default.PlayCircle, null, tint = DustyRose, modifier = Modifier.align(Alignment.Center).size(34.dp))
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))
                    )
                )
        )
        Surface(
            color = Color.Black.copy(alpha = 0.52f),
            shape = CircleShape,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.padding(8.dp).size(22.dp))
        }
        if (canDelete) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.58f), CircleShape)
                    .size(34.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar historia", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
        Text(
            video.description.ifBlank { "Historia del albergue" },
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
        )
    }
}

@Composable
private fun ShelterStoryViewer(
    video: VideoDto,
    canDelete: Boolean = false,
    onDismiss: () -> Unit,
    onDelete: () -> Unit = {}
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = Color.Black,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(Modifier.fillMaxWidth().height(520.dp)) {
                VideoPlayer(
                    videoUrl = video.videoUrl,
                    isPlaying = true,
                    modifier = Modifier.fillMaxSize()
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).background(Color.Black.copy(alpha = 0.48f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                }
                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.align(Alignment.TopStart).padding(10.dp).background(Color.Black.copy(alpha = 0.48f), CircleShape)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar historia", tint = Color.White)
                    }
                }
                Text(
                    video.description.ifBlank { "Historia del albergue" },
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun LegacyShelterAnimalGridCard(
    animal: AnimalDto,
    mediaRevision: Int = 0,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .height(214.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Surface)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(SurfaceRaised),
            contentAlignment = Alignment.Center
        ) {
            if (animal.photoUrl != null) {
                AsyncImage(
                    model = versionedMediaUrl(animal.photoUrl, mediaRevision),
                    contentDescription = animal.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    if (animal.species == "Perro") "ðŸ¶" else if (animal.species == "Gato") "ðŸ±" else "ðŸ¾",
                    fontSize = 34.sp
                )
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
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1
        )
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
