package com.huellalive.app.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.huellalive.app.data.local.SessionManager
import com.huellalive.app.data.model.AdoptionRequestDto
import com.huellalive.app.data.model.ChatDto
import com.huellalive.app.data.model.MessageDto
import com.huellalive.app.data.repository.ChatRepository
import com.huellalive.app.data.repository.MediaRepository
import com.huellalive.app.ui.components.HuellaLoadingIndicator
import com.huellalive.app.ui.components.ConfirmDeleteDialog
import com.huellalive.app.ui.theme.*
import com.huellalive.app.utils.rememberMediaPicker
import org.koin.compose.koinInject

class ChatListScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository = koinInject<ChatRepository>()
        val sessionManager = koinInject<SessionManager>()
        val viewModel = rememberScreenModel { ChatListViewModel(repository) }
        val state by viewModel.uiState.collectAsState()
        var chatToDelete by remember { mutableStateOf<ChatDto?>(null) }

        chatToDelete?.let { chat ->
            val counterpart = chatCounterpart(chat, sessionManager)
            ConfirmDeleteDialog(
                title = "Eliminar conversación",
                message = "Se borrará el chat con ${counterpart.first} para ambos usuarios. Esta acción no se puede deshacer.",
                onDismiss = { chatToDelete = null },
                onConfirm = {
                    chatToDelete = null
                    viewModel.deleteChat(chat.id)
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .statusBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                ChatListHeader(
                    onBack = { navigator.pop() },
                    onRefresh = viewModel::load
                )
                Spacer(Modifier.height(8.dp))
            }

            if (state.isLoading && state.chats.isEmpty()) {
                item {
                    Box(
                        Modifier.fillParentMaxSize().padding(bottom = 100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        HuellaLoadingIndicator()
                    }
                }
            }

            state.errorMessage?.let { message ->
                item {
                    Surface(
                        color = Error.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Error.copy(alpha = 0.35f))
                    ) {
                        Text(message, color = Error, modifier = Modifier.padding(14.dp))
                    }
                }
            }

            if (!state.isLoading && state.chats.isEmpty()) {
                item { EmptyChats() }
            }

            items(state.chats, key = { it.id }) { chat ->
                val counterpart = chatCounterpart(chat, sessionManager)
                ChatRow(
                    chat = chat,
                    title = counterpart.first,
                    avatarUrl = counterpart.second,
                    onClick = {
                        navigator.push(
                            ChatScreen(
                                chatId = chat.id,
                                title = counterpart.first,
                                avatarUrl = counterpart.second
                            )
                        )
                    },
                    onDelete = { chatToDelete = chat }
                )
            }
        }
    }
}

data class ChatScreen(
    val chatId: String,
    val title: String = "Chat",
    val avatarUrl: String? = null
) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val repository = koinInject<ChatRepository>()
        val mediaRepository = koinInject<MediaRepository>()
        val sessionManager = koinInject<SessionManager>()
        val viewModel = rememberScreenModel(chatId) {
            ChatViewModel(chatId, repository, mediaRepository, sessionManager)
        }
        val state by viewModel.uiState.collectAsState()
        val myId = sessionManager.getUserId()
        val listState = rememberLazyListState()
        var showDeleteDialog by remember { mutableStateOf(false) }
        var showTransferSheet by remember { mutableStateOf(false) }
        var transferToConfirm by remember { mutableStateOf<AdoptionRequestDto?>(null) }
        val pickImage = rememberMediaPicker(
            mimeType = "image/*",
            onPicked = viewModel::selectImage,
            onError = viewModel::setError
        )

        DisposableEffect(chatId) {
            onDispose { viewModel.cleanupIfEmpty() }
        }

        LaunchedEffect(state.messages.size) {
            if (state.messages.isNotEmpty()) {
                listState.animateScrollToItem(state.messages.lastIndex)
            }
        }

        if (showDeleteDialog) {
            ConfirmDeleteDialog(
                title = "Eliminar conversación",
                message = "Se borrará esta conversación para ambos usuarios. Esta acción no se puede deshacer.",
                onDismiss = { showDeleteDialog = false },
                onConfirm = {
                    showDeleteDialog = false
                    viewModel.deleteChat { navigator.pop() }
                }
            )
        }

        transferToConfirm?.let { request ->
            TransferConfirmDialog(
                animalName = request.animal?.name ?: "este animal",
                humanName = title,
                isLoading = state.isTransferring,
                onDismiss = { transferToConfirm = null },
                onConfirm = {
                    viewModel.transferAnimal(request.animalId) {
                        transferToConfirm = null
                        showTransferSheet = false
                    }
                }
            )
        }

        if (showTransferSheet) {
            TransferAnimalSheet(
                humanName = title,
                requests = state.transferableAdoptions,
                isLoading = state.isLoadingTransfers,
                isTransferring = state.isTransferring,
                onDismiss = { if (!state.isTransferring) showTransferSheet = false },
                onSelect = { transferToConfirm = it }
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .background(Background)
                .statusBarsPadding()
        ) {
            ConversationHeader(
                title = title,
                avatarUrl = avatarUrl,
                onBack = {
                    viewModel.cleanupIfEmpty()
                    navigator.pop()
                },
                onRefresh = viewModel::load,
                onDelete = { showDeleteDialog = true }
            )

            state.errorMessage?.let { message ->
                Text(
                    message,
                    color = Error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                )
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.isLoading && state.messages.isEmpty() -> {
                        HuellaLoadingIndicator(Modifier.align(Alignment.Center))
                    }
                    state.messages.isEmpty() -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center).padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                color = DustyRose.copy(alpha = 0.14f),
                                shape = CircleShape,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.WavingHand, null, tint = DustyRose)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Empieza la conversacion",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Coordinen con calma los siguientes pasos.",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            items(state.messages, key = { it.id }) { message ->
                                MessageBubble(message, message.senderId == myId)
                            }
                        }
                    }
                }
            }

            if (sessionManager.isShelter()) {
                Button(
                    onClick = {
                        showTransferSheet = true
                        viewModel.loadTransferableAdoptions()
                    },
                    enabled = !state.isTransferring,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AdoptionGreen,
                        contentColor = TextOnAccent
                    )
                ) {
                    Icon(Icons.Default.SyncAlt, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Transferir", fontWeight = FontWeight.Bold)
                }
            }

            MessageComposer(
                text = state.text,
                selectedImageBytes = state.selectedImage?.bytes,
                selectedImageName = state.selectedImage?.fileName,
                isSending = state.isSending,
                onTextChange = viewModel::updateText,
                onPickImage = pickImage,
                onClearImage = viewModel::clearSelectedImage,
                onSend = viewModel::send
            )
        }
    }
}

@Composable
private fun TransferConfirmDialog(
    animalName: String,
    humanName: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        icon = { Icon(Icons.Default.Pets, contentDescription = null, tint = AdoptionGreen) },
        title = {
            Text(
                "Transferir a $animalName",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                "El perfil pasará a la familia de $humanName y dejará de aparecer entre los animales del albergue. La cartilla y los videos se conservarán.",
                color = TextSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = AdoptionGreen)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = TextOnAccent,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.SyncAlt, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Transferir", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancelar")
            }
        },
        containerColor = Surface
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransferAnimalSheet(
    humanName: String,
    requests: List<AdoptionRequestDto>,
    isLoading: Boolean,
    isTransferring: Boolean,
    onDismiss: () -> Unit,
    onSelect: (AdoptionRequestDto) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val normalizedQuery = query.trim()
    val filtered = remember(requests, normalizedQuery) {
        if (normalizedQuery.isBlank()) {
            requests
        } else {
            requests.filter { request ->
                val animal = request.animal
                animal?.name?.contains(normalizedQuery, ignoreCase = true) == true ||
                    animal?.species?.contains(normalizedQuery, ignoreCase = true) == true
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextTertiary) }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 26.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = AdoptionGreen.copy(alpha = 0.18f), shape = RoundedCornerShape(16.dp)) {
                    Icon(
                        Icons.Default.Pets,
                        contentDescription = null,
                        tint = AdoptionGreen,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Transferir animal",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Nueva familia: $humanName",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Buscar por nombre o especie") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AdoptionGreen,
                    focusedLabelColor = AdoptionGreen
                )
            )
            Spacer(Modifier.height(14.dp))
            when {
                isLoading -> Box(
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    HuellaLoadingIndicator()
                }
                requests.isEmpty() -> TransferEmptyState(
                    "No hay adopciones listas",
                    "Acepta primero una solicitud de este humano para habilitar la transferencia."
                )
                filtered.isEmpty() -> TransferEmptyState(
                    "Sin resultados",
                    "No encontramos \"$normalizedQuery\" entre las adopciones aceptadas."
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 390.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    items(filtered, key = { it.id }) { request ->
                        val animal = request.animal ?: return@items
                        Surface(
                            onClick = { if (!isTransferring) onSelect(request) },
                            color = SurfaceRaised,
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, Outline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = animal.photoUrl,
                                    contentDescription = animal.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(58.dp).clip(RoundedCornerShape(15.dp))
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(animal.name, color = TextPrimary, fontWeight = FontWeight.Bold)
                                    Text(animal.species, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                    Text(
                                        "Solicitud aceptada",
                                        color = AdoptionGreen,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AdoptionGreen)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransferEmptyState(title: String, message: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 34.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Pets, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(10.dp))
        Text(title, color = TextPrimary, fontWeight = FontWeight.Bold)
        Text(
            message,
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 22.dp)
        )
    }
}

@Composable
private fun ChatListHeader(onBack: () -> Unit, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledIconButton(
            onClick = onBack,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = SurfaceRaised,
                contentColor = TextPrimary
            )
        ) {
            Icon(Icons.Default.ArrowBack, "Volver")
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Conversaciones",
                color = TextPrimary,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Coordina adopciones y cuidados",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
        IconButton(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, "Actualizar", tint = DustyRose)
        }
    }
}

@Composable
private fun ChatRow(
    chat: ChatDto,
    title: String,
    avatarUrl: String?,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember(chat.id) { mutableStateOf(false) }
    val lastMessage = chat.messages.firstOrNull()
    val preview = when {
        !lastMessage?.content.isNullOrBlank() -> lastMessage?.content.orEmpty()
        lastMessage?.mediaUrl != null -> "Foto"
        else -> "Nueva conversacion"
    }

    Surface(
        onClick = onClick,
        color = Surface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChatAvatar(title, avatarUrl, 54.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (lastMessage?.mediaUrl != null) {
                        Icon(
                            Icons.Default.Image,
                            null,
                            tint = DustyRose,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        preview,
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, "Opciones", tint = TextSecondary)
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Eliminar conversacion", color = Error) },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = Error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationHeader(
    title: String,
    avatarUrl: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        color = Surface,
        border = BorderStroke(1.dp, OutlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "Volver", tint = TextPrimary)
            }
            ChatAvatar(title, avatarUrl, 44.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("Conversacion privada", color = TextSecondary, fontSize = 12.sp)
            }
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, "Actualizar", tint = DustyRose)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, "Eliminar chat", tint = Error)
            }
        }
    }
}

@Composable
private fun MessageBubble(message: MessageDto, mine: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (mine) DustyRose else SurfaceRaised,
            contentColor = if (mine) TextOnAccent else TextPrimary,
            shape = if (mine) {
                RoundedCornerShape(18.dp, 18.dp, 5.dp, 18.dp)
            } else {
                RoundedCornerShape(18.dp, 18.dp, 18.dp, 5.dp)
            },
            modifier = Modifier.widthIn(max = 292.dp)
        ) {
            Column(Modifier.padding(5.dp)) {
                message.mediaUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = "Foto compartida",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .widthIn(min = 180.dp, max = 280.dp)
                            .heightIn(min = 140.dp, max = 260.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                }
                message.content?.takeIf { it.isNotBlank() }?.let { content ->
                    Text(
                        content,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    formatMessageTime(message.createdAt),
                    color = if (mine) TextOnAccent.copy(alpha = 0.62f) else TextSecondary,
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.End).padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
private fun MessageComposer(
    text: String,
    selectedImageBytes: ByteArray?,
    selectedImageName: String?,
    isSending: Boolean,
    onTextChange: (String) -> Unit,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit,
    onSend: () -> Unit
) {
    Surface(
        color = Surface,
        border = BorderStroke(1.dp, OutlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
            AnimatedVisibility(
                visible = selectedImageBytes != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .padding(start = 44.dp, bottom = 8.dp)
                        .size(104.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceRaised)
                ) {
                    if (selectedImageBytes != null) {
                        AsyncImage(
                            model = selectedImageBytes,
                            contentDescription = selectedImageName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    FilledIconButton(
                        onClick = onClearImage,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Color.Black.copy(alpha = 0.68f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(30.dp)
                    ) {
                        Icon(Icons.Default.Close, "Quitar foto", modifier = Modifier.size(17.dp))
                    }
                }
            }

            Row(verticalAlignment = Alignment.Bottom) {
                FilledIconButton(
                    onClick = onPickImage,
                    enabled = !isSending,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = SurfaceRaised,
                        contentColor = DustyRose
                    )
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, "Enviar foto")
                }
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Escribe un mensaje") },
                    minLines = 1,
                    maxLines = 4,
                    shape = RoundedCornerShape(20.dp),
                    colors = textFieldColors()
                )
                Spacer(Modifier.width(8.dp))
                FilledIconButton(
                    onClick = onSend,
                    enabled = !isSending && (text.isNotBlank() || selectedImageBytes != null),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = DustyRose,
                        contentColor = TextOnAccent,
                        disabledContainerColor = SurfaceRaised,
                        disabledContentColor = TextTertiary
                    )
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = TextOnAccent,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Send, "Enviar")
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatAvatar(name: String, avatarUrl: String?, size: androidx.compose.ui.unit.Dp) {
    Surface(
        shape = RoundedCornerShape(size / 3),
        color = DustyRose.copy(alpha = 0.18f),
        modifier = Modifier.size(size)
    ) {
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    name.firstOrNull()?.uppercase() ?: "H",
                    color = DustyRose,
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.38f).sp
                )
            }
        }
    }
}

@Composable
private fun EmptyChats() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 80.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = DustyRose.copy(alpha = 0.14f),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.size(76.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Forum, null, tint = DustyRose, modifier = Modifier.size(34.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Aun no hay conversaciones",
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Los chats de adopcion apareceran aqui.",
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun chatCounterpart(chat: ChatDto, sessionManager: SessionManager): Pair<String, String?> {
    return if (sessionManager.isShelter()) {
        (chat.human?.name ?: "Persona interesada") to chat.human?.avatarUrl
    } else {
        (chat.shelter?.user?.name ?: "Albergue") to chat.shelter?.user?.avatarUrl
    }
}

private fun formatMessageTime(value: String): String {
    val time = value.substringAfter('T', "").take(5)
    return time.ifBlank { "" }
}

@Composable
private fun textFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = DustyRose,
    unfocusedBorderColor = Outline,
    focusedLeadingIconColor = DustyRose,
    cursorColor = DustyRose
)
