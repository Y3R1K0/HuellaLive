package com.huellalive.app.ui.animal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.huellalive.app.data.local.SessionManager
import com.huellalive.app.data.model.AnimalCardDto
import com.huellalive.app.data.model.AnimalDto
import com.huellalive.app.data.model.ControlDto
import com.huellalive.app.data.model.SearchSpeciesDto
import com.huellalive.app.data.model.VaccineDto
import com.huellalive.app.data.model.VideoDto
import com.huellalive.app.data.repository.AnimalRepository
import com.huellalive.app.data.repository.EngagementRepository
import com.huellalive.app.data.repository.MediaRepository
import com.huellalive.app.data.repository.WalletRepository
import com.huellalive.app.ui.auth.AuthChoiceScreen
import com.huellalive.app.ui.components.AdjustableImagePreview
import com.huellalive.app.ui.components.ConfirmDeleteDialog
import com.huellalive.app.ui.components.SectionHeading
import com.huellalive.app.ui.components.TonalChip
import com.huellalive.app.ui.components.HuellaLoadingIndicator
import com.huellalive.app.ui.components.ProfileLoadingSkeleton
import com.huellalive.app.ui.components.HuellaMessageSnackbar
import com.huellalive.app.ui.components.StaggeredReveal
import com.huellalive.app.ui.components.VideoPlayer
import com.huellalive.app.ui.shelter.ShelterDetailScreen
import com.huellalive.app.ui.video.UploadVideoScreen
import com.huellalive.app.ui.theme.*
import com.huellalive.app.utils.PickedMedia
import com.huellalive.app.utils.ImageAdjustment
import com.huellalive.app.utils.adjustedPickedImage
import com.huellalive.app.utils.cloudinaryVideoPosterUrl
import com.huellalive.app.utils.rememberMediaPicker
import com.huellalive.app.utils.versionedMediaUrl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

data class AnimalDetailScreenData(val animalId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val animalRepo = koinInject<AnimalRepository>()
        val engagementRepo = koinInject<EngagementRepository>()
        val mediaRepo = koinInject<MediaRepository>()
        val walletRepo = koinInject<WalletRepository>()
        val sessionManager = koinInject<SessionManager>()
        val viewModel = rememberScreenModel(animalId) {
            AnimalDetailViewModel(animalId, animalRepo, engagementRepo, mediaRepo, walletRepo, sessionManager)
        }
        val state by viewModel.uiState.collectAsState()
        val sessionVersion by sessionManager.sessionVersion.collectAsState()
        val isLoggedIn = sessionManager.isLoggedIn()
        val isHuman = sessionManager.isHuman()
        val isShelter = sessionManager.isShelter()
        var showEditDialog by remember { mutableStateOf(false) }
        var showCardEditDialog by remember { mutableStateOf(false) }
        var showAdoptionConfirmation by remember { mutableStateOf(false) }
        var selectedVideoIndex by remember { mutableStateOf<Int?>(null) }
        var videoToDelete by remember { mutableStateOf<VideoDto?>(null) }

        LaunchedEffect(sessionVersion) {
            viewModel.refreshForSession()
        }

        if (showAdoptionConfirmation && state.animal != null) {
            AdoptionCommitmentDialog(
                animalName = state.animal!!.name,
                onCancel = { showAdoptionConfirmation = false },
                onConfirm = {
                    showAdoptionConfirmation = false
                    viewModel.requestAdoption()
                }
            )
        }

        videoToDelete?.let { video ->
            ConfirmDeleteDialog(
                title = "Eliminar video",
                message = "Se eliminará \"${video.description.ifBlank { "Video del animal" }}\" del perfil y del feed. Esta acción no se puede deshacer.",
                onDismiss = { videoToDelete = null },
                onConfirm = {
                    videoToDelete = null
                    viewModel.deleteVideo(video.id)
                }
            )
        }

        if (state.showCardDialog && state.animal?.card != null) {
            AnimalCardDialog(
                card = state.animal!!.card!!,
                animalName = state.animal!!.name,
                canEdit = state.animal?.let { animal ->
                    isLoggedIn && (
                        animal.shelter?.user?.id == sessionManager.getUserId() ||
                            animal.adoptedBy?.id == sessionManager.getUserId() ||
                            sessionManager.isAdmin()
                        )
                } == true,
                onEdit = {
                    viewModel.toggleCardDialog()
                    showCardEditDialog = true
                },
                onDismiss = { viewModel.toggleCardDialog() }
            )
        }

        if (showCardEditDialog && state.animal != null) {
            EditAnimalCardDialog(
                card = state.animal!!.card,
                isSaving = state.actionInProgress == "card",
                onDismiss = { showCardEditDialog = false },
                onSave = { sex, birthDate, sterilized, vaccines, controls, notes ->
                    viewModel.updateAnimalCard(
                        sex = sex,
                        birthDate = birthDate,
                        isSterilized = sterilized,
                        vaccines = vaccines,
                        controls = controls,
                        notes = notes,
                        onDone = { showCardEditDialog = false }
                    )
                }
            )
        }

        selectedVideoIndex?.let { index ->
            val videos = state.animal?.videos.orEmpty()
            if (videos.isNotEmpty() && index in videos.indices) {
                AnimalVideoViewer(
                    video = videos[index],
                    position = index + 1,
                    total = videos.size,
                    onPrevious = if (index > 0) ({ selectedVideoIndex = index - 1 }) else null,
                    onNext = if (index < videos.lastIndex) ({ selectedVideoIndex = index + 1 }) else null,
                    onDismiss = { selectedVideoIndex = null }
                )
            }
        }

        if (showEditDialog && state.animal != null) {
            val animal = state.animal!!
            val isHumanOwner = sessionManager.isHuman() &&
                animal.adoptedBy?.id == sessionManager.getUserId()
            EditAnimalDialog(
                animal = animal,
                speciesOptions = state.species,
                isLoadingSpecies = state.isLoadingSpecies,
                canEditStatus = !isHumanOwner || sessionManager.isAdmin(),
                currentPhotoUrl = animal.photoUrl,
                onDismiss = { showEditDialog = false },
                onSave = { name, species, breed, age, description, status, photo ->
                    viewModel.updateAnimal(
                        name = name,
                        species = species,
                        breed = breed,
                        age = age,
                        description = description,
                        status = status,
                        selectedPhoto = photo,
                        currentPhotoUrl = state.animal!!.photoUrl,
                        onDone = { showEditDialog = false }
                    )
                },
                onError = viewModel::setActionMessage
            )
        }

        Box(modifier = Modifier.fillMaxSize().background(Background)) {
            when {
                state.isLoading -> ProfileLoadingSkeleton()
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
                    val currentUserId = sessionManager.getUserId()
                    val isCurrentShelterOwner = isShelter &&
                        animal.status != "ADOPTED" &&
                        animal.adoptedBy?.id == null &&
                        animal.shelter?.user?.id == currentUserId
                    val isCurrentHumanOwner = isHuman &&
                        animal.adoptedBy?.id == currentUserId
                    val canUploadVideo = isLoggedIn && (isCurrentShelterOwner || isCurrentHumanOwner)
                    val canEditAnimal = isLoggedIn && (
                        animal.shelter?.user?.id == currentUserId ||
                            animal.adoptedBy?.id == currentUserId ||
                            sessionManager.isAdmin()
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding(),
                        contentPadding = PaddingValues(bottom = 136.dp)
                    ) {

                        // Hero foto
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(360.dp)
                                    .clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
                            ) {
                                if (animal.photoUrl != null) {
                                    AsyncImage(
                                        model = versionedMediaUrl(animal.photoUrl, state.mediaRevision),
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
                                    shape = RoundedCornerShape(14.dp)
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
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                                    .background(Background)
                                    .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 4.dp)
                            ) {
                                Text(animal.name, style = MaterialTheme.typography.displayLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                                if (animal.shelter != null) {
                                    Spacer(Modifier.height(5.dp))
                                    Row(
                                        modifier = Modifier.clickable { navigator.push(ShelterDetailScreen(animal.shelter.id)) },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Home, null, tint = DustyRose, modifier = Modifier.size(17.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(animal.shelter.user.name, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                                        Icon(Icons.Default.ChevronRight, null, tint = DustyRose, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Text(
                                    buildString {
                                        append(animal.species)
                                        if (animal.breed != null) append(" · ${animal.breed}")
                                        if (animal.age != null) append(" · ${animal.age} años")
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextSecondary
                                )
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TonalChip(animal.species, color = CareLilac, icon = Icons.Default.Pets)
                                    animal.age?.let { TonalChip("$it anos", color = InfoBlue, icon = Icons.Default.Event) }
                                    animal.breed?.let { TonalChip(it, color = BadgeCream) }
                                }
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
                                    shape = RoundedCornerShape(16.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DustyRose.copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Default.Description, null, modifier = Modifier.size(16.dp), tint = DustyRose)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Cartilla", color = DustyRose)
                                }

                                if (canEditAnimal) {
                                    OutlinedButton(
                                        onClick = { showEditDialog = true },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, DustyRose.copy(alpha = 0.5f))
                                    ) {
                                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp), tint = DustyRose)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Editar", color = DustyRose)
                                    }
                                }

                                if (animal.status == "AVAILABLE" && isHuman) {
                                    val requestState = state.adoptionRequestState
                                    when {
                                        requestState?.hasActiveRequest == true -> {
                                            Surface(
                                                modifier = Modifier.weight(1f),
                                                color = AdoptionGreen.copy(alpha = 0.16f),
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        Icons.Default.CheckCircle,
                                                        contentDescription = null,
                                                        tint = AdoptionGreen,
                                                        modifier = Modifier.size(17.dp)
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(
                                                        if (requestState.activeStatus == "ACCEPTED") "Solicitud aceptada"
                                                        else "Solicitud enviada",
                                                        color = AdoptionGreen,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                        requestState?.dailyLimitReached == true -> {
                                            Surface(
                                                modifier = Modifier.weight(1f),
                                                color = SurfaceRaised,
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Text(
                                                    "Cupo diario completo",
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                                    color = TextSecondary,
                                                    textAlign = TextAlign.Center,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                        else -> {
                                            Button(
                                                onClick = { showAdoptionConfirmation = true },
                                                enabled = state.actionInProgress == null,
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = DustyRose)
                                            ) {
                                                if (state.actionInProgress == "adoption") {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(18.dp),
                                                        color = TextOnAccent,
                                                        strokeWidth = 2.dp
                                                    )
                                                } else {
                                                    Icon(Icons.Default.Favorite, null, modifier = Modifier.size(16.dp), tint = TextOnAccent)
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("Solicitar adopción", color = TextOnAccent)
                                                }
                                            }
                                        }
                                    }
                                } else if (animal.status == "AVAILABLE" && !isLoggedIn) {
                            Button(
                                onClick = { navigator.push(AuthChoiceScreen()) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = DustyRose)
                                    ) {
                                        Text("Iniciar sesión para adoptar", color = TextOnAccent, fontSize = 12.sp)
                                    }
                                }
                        }
                        if (canUploadVideo) {
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = { navigator.push(UploadVideoScreen(animal.id)) },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DustyRose)
                            ) {
                                Icon(Icons.Default.Upload, null, tint = TextOnAccent)
                                Spacer(Modifier.width(8.dp))
                                Text("Subir video del animal", color = TextOnAccent)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        }

                        // Albergue de origen
                        if (animal.shelter != null) {
                            item {
                                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                                    SectionHeading("Albergue", modifier = Modifier.padding(bottom = 8.dp))
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)),
                                        color = Surface,
                                        onClick = { navigator.push(ShelterDetailScreen(animal.shelter.id)) }
                                    ) {
                                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(DustyRose),
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
                                SectionHeading("Videos de ${animal.name}", modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                            }
                            items(animal.videos.chunked(2)) { rowVideos ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowVideos.forEach { video ->
                                        val videoIndex = animal.videos.indexOfFirst { it.id == video.id }
                                        VideoGridCard(
                                            video = video,
                                            canDelete = isLoggedIn && (
                                                video.author.id == sessionManager.getUserId() ||
                                                    animal.shelter?.user?.id == sessionManager.getUserId() ||
                                                    sessionManager.isAdmin()
                                            ),
                                            modifier = Modifier.weight(1f),
                                            onClick = {
                                                if (videoIndex >= 0) selectedVideoIndex = videoIndex
                                            },
                                            onDelete = { videoToDelete = video }
                                        )
                                    }
                                    if (rowVideos.size == 1) {
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
private fun AdoptionCommitmentDialog(
    animalName: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val totalSeconds = 15
    var remainingSeconds by remember(animalName) { mutableStateOf(totalSeconds) }
    val canContinue = remainingSeconds == 0
    val progress = (totalSeconds - remainingSeconds) / totalSeconds.toFloat()

    LaunchedEffect(animalName) {
        while (remainingSeconds > 0) {
            delay(1_000)
            remainingSeconds -= 1
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (canContinue) onCancel()
        },
        properties = DialogProperties(
            dismissOnBackPress = canContinue,
            dismissOnClickOutside = canContinue
        ),
        icon = {
            Surface(
                color = DustyRose.copy(alpha = 0.16f),
                shape = CircleShape
            ) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    tint = DustyRose,
                    modifier = Modifier.padding(14.dp).size(28.dp)
                )
            }
        },
        title = {
            Text(
                "Adoptar es un compromiso",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Solicitar la adopción de $animalName no es un juego. Un animal necesita tiempo, cuidados, estabilidad y un hogar responsable durante toda su vida.",
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                    color = AdoptionGreen,
                    trackColor = SurfaceRaised
                )
                Text(
                    if (canContinue) "Ya puedes confirmar tu solicitud"
                    else "Lee con calma · $remainingSeconds s",
                    color = if (canContinue) AdoptionGreen else TextSecondary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = canContinue,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AdoptionGreen,
                    contentColor = TextOnAccent
                )
            ) {
                Icon(Icons.Default.Pets, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(
                    if (canContinue) "Confirmar solicitud" else "Espera $remainingSeconds s",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onCancel,
                enabled = canContinue,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Cancelar")
            }
        },
        containerColor = Surface
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EditAnimalDialog(
    animal: AnimalDto,
    speciesOptions: List<SearchSpeciesDto>,
    isLoadingSpecies: Boolean,
    canEditStatus: Boolean,
    currentPhotoUrl: String?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, PickedMedia?) -> Unit,
    onError: (String) -> Unit
) {
    var name by remember { mutableStateOf(animal.name) }
    var species by remember { mutableStateOf(animal.species) }
    var speciesExpanded by remember { mutableStateOf(false) }
    var breed by remember { mutableStateOf(animal.breed.orEmpty()) }
    var age by remember { mutableStateOf(animal.age?.toString().orEmpty()) }
    var description by remember { mutableStateOf(animal.description.orEmpty()) }
    var status by remember { mutableStateOf(animal.status) }
    var statusExpanded by remember { mutableStateOf(false) }
    var selectedPhoto by remember { mutableStateOf<PickedMedia?>(null) }
    var photoAdjustment by remember { mutableStateOf(ImageAdjustment()) }
    val scope = rememberCoroutineScope()
    val statusOptions = listOf(
        "AVAILABLE" to "Disponible",
        "RECOVERING" to "En recuperación",
        "PREGNANT" to "Preñada"
    )
    val visibleSpeciesOptions = remember(speciesOptions, animal.species) {
        buildList {
            addAll(speciesOptions.map { it.name })
            if (none { it.equals("Otro", ignoreCase = true) }) {
                add("Otro")
            }
        }.distinctBy { it.lowercase() }
    }
    val pickPhoto = rememberMediaPicker(
        mimeType = "image/*",
        onPicked = { selectedPhoto = it },
        onError = onError
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar animal", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = pickPhoto,
                    modifier = Modifier.fillMaxWidth(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DustyRose.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.PhotoCamera, null, tint = DustyRose)
                    Spacer(Modifier.width(8.dp))
                Text(selectedPhoto?.fileName ?: "Cambiar foto", color = DustyRose, maxLines = 1)
            }
            AdjustableImagePreview(
                title = "Foto del animal",
                selectedMedia = selectedPhoto,
                currentUrl = currentPhotoUrl,
                adjustment = photoAdjustment,
                onAdjustmentChange = { photoAdjustment = it },
                aspectRatio = 1f,
                shape = RoundedCornerShape(22.dp)
            )
                AnimalField("Nombre", name) { name = it }
                ExposedDropdownMenuBox(
                    expanded = speciesExpanded,
                    onExpandedChange = { if (!isLoadingSpecies && visibleSpeciesOptions.isNotEmpty()) speciesExpanded = it }
                ) {
                    OutlinedTextField(
                        value = if (isLoadingSpecies) "Cargando especies..." else species,
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isLoadingSpecies && visibleSpeciesOptions.isNotEmpty(),
                        label = { Text("Especie") },
                        leadingIcon = {
                            Icon(Icons.Default.Pets, contentDescription = null, tint = DustyRose)
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = speciesExpanded)
                        },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = DustyRose,
                            unfocusedBorderColor = Outline,
                            focusedLabelColor = DustyRose,
                            unfocusedLabelColor = TextSecondary,
                            disabledTextColor = TextSecondary,
                            disabledBorderColor = Outline,
                            disabledLabelColor = TextSecondary,
                            disabledLeadingIconColor = TextSecondary,
                            disabledTrailingIconColor = TextSecondary
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = speciesExpanded,
                        onDismissRequest = { speciesExpanded = false }
                    ) {
                        visibleSpeciesOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val selected = species.equals(option, ignoreCase = true)
                                        Icon(
                                            if (selected) Icons.Default.CheckCircle else Icons.Default.Circle,
                                            contentDescription = null,
                                            tint = if (selected) DustyRose else TextTertiary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(option, color = TextPrimary)
                                    }
                                },
                                onClick = {
                                    species = option
                                    speciesExpanded = false
                                }
                            )
                        }
                    }
                }
                AnimalField("Raza", breed) { breed = it }
                AnimalField("Edad", age) { age = it }
                AnimalField("Descripcion", description) { description = it }
                if (canEditStatus) {
                    ExposedDropdownMenuBox(
                        expanded = statusExpanded,
                        onExpandedChange = { statusExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = statusOptions.find { it.first == status }?.second
                                ?: if (status == "ADOPTED") "Adoptado" else "Seleccionar estado",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Estado") },
                            leadingIcon = {
                                Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = DustyRose)
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded)
                            },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = DustyRose,
                                unfocusedBorderColor = Outline,
                                focusedLabelColor = DustyRose,
                                unfocusedLabelColor = TextSecondary
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = statusExpanded,
                            onDismissRequest = { statusExpanded = false }
                        ) {
                            statusOptions.forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            val selected = status == value
                                            Icon(
                                                if (selected) Icons.Default.CheckCircle else Icons.Default.Circle,
                                                contentDescription = null,
                                                tint = if (selected) DustyRose else TextTertiary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Text(label, color = TextPrimary)
                                        }
                                    },
                                    onClick = {
                                        status = value
                                        statusExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = "Adoptado",
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Estado de adopción") },
                        leadingIcon = {
                            Icon(Icons.Default.Home, contentDescription = null, tint = AdoptionGreen)
                        },
                        supportingText = {
                            Text("Este estado es permanente después de la transferencia")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = AdoptionGreen,
                            disabledBorderColor = AdoptionGreen.copy(alpha = 0.45f),
                            disabledLabelColor = TextSecondary,
                            disabledLeadingIconColor = AdoptionGreen,
                            disabledSupportingTextColor = TextTertiary
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        val adjustedPhoto = selectedPhoto?.let {
                            adjustedPickedImage(it, photoAdjustment, aspectRatio = 1f, outputWidth = 1000)
                        }
                        onSave(name, species, breed, age, description, status, adjustedPhoto)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DustyRose)
            ) {
                Text("Guardar", color = TextOnAccent)
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancelar") } },
        containerColor = Surface
    )
}

@Composable
private fun AnimalField(label: String, value: String, onChange: (String) -> Unit) {
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

@Composable
private fun VideoThumbnailRow(video: VideoDto, canDelete: Boolean, onDelete: () -> Unit) {
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
        if (canDelete) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar video", tint = DustyRose)
            }
        }
    }
}

@Composable
private fun VideoGridCard(
    video: VideoDto,
    canDelete: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = modifier
            .height(210.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Surface)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(138.dp).background(SurfaceRaised),
            contentAlignment = Alignment.Center
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
                Icon(Icons.Default.PlayCircle, null, tint = DustyRose, modifier = Modifier.size(34.dp))
            }
            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                shape = CircleShape,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp).size(22.dp)
                )
            }
            if (canDelete) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.42f), CircleShape)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar video", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
        Column(modifier = Modifier.padding(10.dp)) {
            Text(video.description.ifBlank { "Video del animal" }, style = MaterialTheme.typography.bodySmall, color = TextPrimary, maxLines = 2)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Favorite, null, tint = DustyRose, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(video.likesCount.toString(), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun HealthMetric(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier, color = color.copy(alpha = 0.16f), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun EditAnimalCardDialog(
    card: AnimalCardDto?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String, String, Boolean, List<VaccineDto>, List<ControlDto>, String) -> Unit
) {
    var sex by remember(card) { mutableStateOf(card?.sex.orEmpty()) }
    var birthDate by remember(card) { mutableStateOf(card?.birthDate?.take(10).orEmpty()) }
    var isSterilized by remember(card) { mutableStateOf(card?.isSterilized == true) }
    var notes by remember(card) { mutableStateOf(card?.notes.orEmpty()) }
    var vaccines by remember(card) { mutableStateOf(card?.vaccines.orEmpty()) }
    var controls by remember(card) { mutableStateOf(card?.controls.orEmpty()) }
    var vaccineName by remember { mutableStateOf("") }
    var vaccineDate by remember { mutableStateOf("") }
    var controlDescription by remember { mutableStateOf("") }
    var controlDate by remember { mutableStateOf("") }
    var controlNextDate by remember { mutableStateOf("") }
    var pendingRemoval by remember { mutableStateOf<CardRecordRemoval?>(null) }

    pendingRemoval?.let { removal ->
        ConfirmDeleteDialog(
            title = "Quitar ${removal.kind}",
            message = "Se quitará \"${removal.title}\" de la cartilla. El cambio se aplicará cuando pulses Guardar cartilla.",
            confirmLabel = "Quitar",
            onDismiss = { pendingRemoval = null },
            onConfirm = {
                if (removal.kind == "vacuna") {
                    vaccines = vaccines.filterIndexed { index, _ -> index != removal.index }
                } else {
                    controls = controls.filterIndexed { index, _ -> index != removal.index }
                }
                pendingRemoval = null
            }
        )
    }

    Dialog(onDismissRequest = { if (!isSaving) onDismiss() }) {
        Surface(
            modifier = Modifier.fillMaxWidth().heightIn(max = 720.dp),
            shape = RoundedCornerShape(26.dp),
            color = Surface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = DustyRose.copy(alpha = 0.18f), shape = RoundedCornerShape(14.dp)) {
                        Icon(
                            Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = DustyRose,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Editar cartilla", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text("Mantén sus cuidados al día", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    IconButton(onClick = onDismiss, enabled = !isSaving) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecondary)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Column(
                    modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AnimalField("Sexo", sex) { sex = it }
                    AnimalField("Fecha de nacimiento (YYYY-MM-DD)", birthDate) { birthDate = it }
                    Surface(color = SurfaceRaised, shape = RoundedCornerShape(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.HealthAndSafety, null, tint = DustyRose)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Esterilizado", color = TextPrimary, fontWeight = FontWeight.Medium)
                                Text("Indica si el procedimiento ya se realizó", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(
                                checked = isSterilized,
                                onCheckedChange = { isSterilized = it },
                                colors = SwitchDefaults.colors(checkedTrackColor = DustyRose)
                            )
                        }
                    }

                    EditorSectionTitle("Vacunas", "${vaccines.size}")
                    vaccines.forEachIndexed { index, vaccine ->
                        EditableRecordRow(
                            title = vaccine.name,
                            subtitle = listOfNotNull(vaccine.date, vaccine.appliedBy).joinToString(" · "),
                            onDelete = {
                                pendingRemoval = CardRecordRemoval("vacuna", index, vaccine.name)
                            }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1.2f)) { AnimalField("Vacuna", vaccineName) { vaccineName = it } }
                        Box(Modifier.weight(1f)) { AnimalField("Fecha", vaccineDate) { vaccineDate = it } }
                        FilledTonalIconButton(
                            onClick = {
                                if (vaccineName.isNotBlank() && vaccineDate.isNotBlank()) {
                                    vaccines = vaccines + VaccineDto(vaccineName.trim(), vaccineDate.trim())
                                    vaccineName = ""
                                    vaccineDate = ""
                                }
                            },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = DustyRose.copy(alpha = 0.18f),
                                contentColor = DustyRose
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Agregar vacuna")
                        }
                    }

                    EditorSectionTitle("Controles", "${controls.size}")
                    controls.forEachIndexed { index, control ->
                        EditableRecordRow(
                            title = control.description,
                            subtitle = buildString {
                                append(control.date)
                                if (!control.nextDate.isNullOrBlank()) append(" · Próximo: ${control.nextDate}")
                            },
                            onDelete = {
                                pendingRemoval = CardRecordRemoval("control", index, control.description)
                            }
                        )
                    }
                    AnimalField("Descripción del control", controlDescription) { controlDescription = it }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f)) { AnimalField("Fecha", controlDate) { controlDate = it } }
                        Box(Modifier.weight(1f)) { AnimalField("Próximo", controlNextDate) { controlNextDate = it } }
                        FilledTonalIconButton(
                            onClick = {
                                if (controlDescription.isNotBlank() && controlDate.isNotBlank()) {
                                    controls = controls + ControlDto(
                                        description = controlDescription.trim(),
                                        date = controlDate.trim(),
                                        nextDate = controlNextDate.ifBlank { null }
                                    )
                                    controlDescription = ""
                                    controlDate = ""
                                    controlNextDate = ""
                                }
                            },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = DustyRose.copy(alpha = 0.18f),
                                contentColor = DustyRose
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Agregar control")
                        }
                    }
                    AnimalField("Notas y cuidados especiales", notes) { notes = it }
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onSave(sex, birthDate, isSterilized, vaccines, controls, notes) },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DustyRose)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = TextOnAccent)
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Guardar cartilla", color = TextOnAccent)
                    }
                }
            }
        }
    }
}

private data class CardRecordRemoval(
    val kind: String,
    val index: Int,
    val title: String
)

@Composable
private fun EditorSectionTitle(title: String, count: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Surface(color = DustyRose.copy(alpha = 0.16f), shape = CircleShape) {
            Text(count, modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp), color = DustyRose)
        }
    }
}

@Composable
private fun EditableRecordRow(title: String, subtitle: String, onDelete: () -> Unit) {
    Surface(color = SurfaceRaised, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusAvailable, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(subtitle, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Close, contentDescription = "Quitar", tint = TextSecondary)
            }
        }
    }
}

@Composable
private fun AnimalVideoViewer(
    video: VideoDto,
    position: Int,
    total: Int,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    var isPlaying by remember(video.id) { mutableStateOf(true) }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Black,
            shape = RoundedCornerShape(24.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(9f / 14f)
                        .clickable { isPlaying = !isPlaying }
                ) {
                    VideoPlayer(
                        videoUrl = video.videoUrl,
                        isPlaying = isPlaying,
                        modifier = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.55f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar video", tint = Color.White)
                    }
                    if (!isPlaying) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.62f),
                            shape = CircleShape,
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Reproducir",
                                tint = Color.White,
                                modifier = Modifier.padding(16.dp).size(34.dp)
                            )
                        }
                    }
                    Surface(
                        color = Color.Black.copy(alpha = 0.58f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                    ) {
                        Text(
                            "$position de $total",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onPrevious?.invoke() }, enabled = onPrevious != null) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Video anterior", tint = Color.White)
                    }
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                        Text(
                            video.description.ifBlank { "Video del animal" },
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2
                        )
                        Text(
                            "${video.likesCount} me gusta",
                            color = Color.White.copy(alpha = 0.65f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    IconButton(onClick = { onNext?.invoke() }, enabled = onNext != null) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Siguiente video", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimalCardDialog(
    card: AnimalCardDto,
    animalName: String,
    canEdit: Boolean,
    onEdit: () -> Unit,
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
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HealthMetric(
                            label = "Sexo",
                            value = card.sex ?: "Sin registrar",
                            icon = Icons.Default.Pets,
                            color = CareLilac,
                            modifier = Modifier.weight(1f)
                        )
                        HealthMetric(
                            label = "Nacimiento",
                            value = card.birthDate?.take(10) ?: "Sin registrar",
                            icon = Icons.Default.Cake,
                            color = InfoBlue,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        HealthMetric(
                            label = "Esterilización",
                            value = if (card.isSterilized) "Realizada" else "Pendiente",
                            icon = Icons.Default.HealthAndSafety,
                            color = if (card.isSterilized) StatusAvailable else StatusRecovering,
                            modifier = Modifier.weight(1f)
                        )
                        HealthMetric(
                            label = "Vacunas",
                            value = "${card.vaccines.size} registradas",
                            icon = Icons.Default.MedicalServices,
                            color = PeachWarm,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (card.notes != null) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceRaised, RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Default.Notes, contentDescription = null, tint = DustyRose, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Notas y cuidados", style = MaterialTheme.typography.labelLarge, color = DustyRose)
                                Text(card.notes, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
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
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cerrar")
                        }
                        if (canEdit) {
                            Button(
                                onClick = onEdit,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = DustyRose)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Editar", color = TextOnAccent)
                            }
                        }
                    }
                }
            }
        }
    }
}

