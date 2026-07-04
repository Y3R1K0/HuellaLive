package com.huellalive.app.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import com.huellalive.app.data.local.SessionManager
import com.huellalive.app.data.model.AnimalDto
import com.huellalive.app.data.model.SearchCityDto
import com.huellalive.app.data.model.SearchSpeciesDto
import com.huellalive.app.data.model.ShelterProfileDto
import com.huellalive.app.data.repository.AuthRepository
import com.huellalive.app.data.repository.ExploreRepository
import com.huellalive.app.data.repository.FeedRepository
import com.huellalive.app.ui.animal.AnimalDetailScreenData
import com.huellalive.app.ui.components.SectionHeading
import com.huellalive.app.ui.components.TonalChip
import com.huellalive.app.ui.components.HuellaLoadingIndicator
import com.huellalive.app.ui.components.AnimalGridSkeleton
import com.huellalive.app.ui.components.AnimalProfileCard
import com.huellalive.app.ui.components.ExploreMapSkeleton
import com.huellalive.app.ui.components.SearchShelterSkeletons
import com.huellalive.app.ui.components.ShelterProfileCard
import com.huellalive.app.ui.components.StaggeredReveal
import com.huellalive.app.ui.components.PreloadVideo
import com.huellalive.app.ui.explore.ExploreViewModel
import com.huellalive.app.ui.feed.FeedViewModel
import com.huellalive.app.ui.feed.FeedRefreshSignal
import com.huellalive.app.ui.feed.VideoFeedItem
import com.huellalive.app.ui.feed.stableCloudinaryVideoUrl
import com.huellalive.app.ui.map.ShelterMap
import com.huellalive.app.ui.search.SearchViewModel
import com.huellalive.app.ui.shelter.ShelterDetailScreen
import com.huellalive.app.ui.shelter.ShelterProfileScreen
import com.huellalive.app.ui.theme.*
import com.huellalive.app.utils.Resource
import kotlinx.coroutines.flow.StateFlow
import org.koin.compose.koinInject

class MainScreen(
    private val initialTab: String = "feed",
    private val instanceKey: Long = 0L,
    private val feedViewModel: FeedViewModel? = null,
    private val exploreViewModel: ExploreViewModel? = null,
    private val searchViewModel: SearchViewModel? = null,
    private val tabState: StateFlow<String>? = null
) : Screen {
    override val key: ScreenKey = "MainScreen:$initialTab:$instanceKey"
    private var retainedFeedViewModel: FeedViewModel? = null
    private var retainedExploreViewModel: ExploreViewModel? = null
    private var retainedSearchViewModel: SearchViewModel? = null

    @Composable
    override fun Content() {
        val feedRepository = koinInject<FeedRepository>()
        val exploreRepository = koinInject<ExploreRepository>()
        val authRepository = koinInject<AuthRepository>()
        val retainedOrCreatedFeedViewModel = remember(feedViewModel) {
            feedViewModel ?: retainedFeedViewModel ?: FeedViewModel(feedRepository, authRepository).also {
                retainedFeedViewModel = it
            }
        }
        val retainedOrCreatedExploreViewModel = remember(exploreViewModel) {
            exploreViewModel ?: retainedExploreViewModel ?: ExploreViewModel(exploreRepository).also {
                retainedExploreViewModel = it
            }
        }
        val retainedOrCreatedSearchViewModel = remember(searchViewModel) {
            searchViewModel ?: retainedSearchViewModel ?: SearchViewModel(exploreRepository).also {
                retainedSearchViewModel = it
            }
        }
        val selectedTab = tabState?.collectAsState()?.value ?: initialTab
        AnimatedContent(
            targetState = selectedTab,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                val movingForward = tabPosition(targetState) >= tabPosition(initialState)
                val enterOffset: (Int) -> Int = { width -> if (movingForward) width / 7 else -width / 7 }
                val exitOffset: (Int) -> Int = { width -> if (movingForward) -width / 10 else width / 10 }
                (
                    slideInHorizontally(
                        animationSpec = spring(dampingRatio = 0.86f, stiffness = 520f),
                        initialOffsetX = enterOffset
                    ) +
                        fadeIn(animationSpec = spring(stiffness = 700f)) +
                        scaleIn(initialScale = 0.985f, animationSpec = spring(stiffness = 600f))
                    ).togetherWith(
                    slideOutHorizontally(
                        animationSpec = spring(dampingRatio = 0.9f, stiffness = 560f),
                        targetOffsetX = exitOffset
                    ) +
                        fadeOut(animationSpec = spring(stiffness = 800f)) +
                        scaleOut(targetScale = 0.99f, animationSpec = spring(stiffness = 700f))
                )
            },
            contentKey = { it },
            label = "Main tab transition"
        ) { tab ->
            when (tab) {
                "explore" -> ExploreTab(retainedOrCreatedExploreViewModel)
                "search" -> SearchTab(retainedOrCreatedSearchViewModel)
                else -> FeedTab(
                    viewModel = retainedOrCreatedFeedViewModel,
                    isVisible = selectedTab == "feed"
                )
            }
        }
    }
}

@Composable
private fun FeedTab(viewModel: FeedViewModel, isVisible: Boolean) {
    val navigator = LocalNavigator.currentOrThrow
    val state by viewModel.uiState.collectAsState()
    val refreshVersion by FeedRefreshSignal.version.collectAsState()

    LaunchedEffect(refreshVersion) {
        viewModel.refreshIfNeeded(refreshVersion)
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when {
            state.isLoading -> HuellaLoadingIndicator(Modifier.align(Alignment.Center))
            state.errorMessage != null -> RetryState(state.errorMessage!!) { viewModel.loadFeed() }
            state.videos.isEmpty() -> EmptyCentered("No hay videos aun", "Los videos apareceran aqui")
            else -> {
                val initialPage = state.currentVideoIndex.coerceIn(0, (state.videos.size - 1).coerceAtLeast(0))
                val pagerState = androidx.compose.foundation.pager.rememberPagerState(initialPage = initialPage) { state.videos.size }
                LaunchedEffect(pagerState) {
                    snapshotFlow { pagerState.settledPage }
                        .collect { page -> viewModel.updateCurrentVideoIndex(page) }
                }
                LaunchedEffect(pagerState.settledPage, state.videos.size) {
                    if (state.videos.isNotEmpty() && pagerState.settledPage >= state.videos.size - 3) {
                        viewModel.loadMore()
                    }
                }
                LaunchedEffect(isVisible, pagerState.settledPage, state.videos) {
                    if (isVisible && state.videos.isNotEmpty()) {
                        state.videos.getOrNull(pagerState.settledPage)?.let(viewModel::markVideoViewed)
                    }
                }
                PreloadVideo(
                    state.videos.getOrNull(pagerState.settledPage + 1)
                        ?.videoUrl
                        ?.let(::stableCloudinaryVideoUrl)
                )
                state.actionMessage?.let { message ->
                    LaunchedEffect(message) {
                        kotlinx.coroutines.delay(2200)
                        viewModel.clearActionMessage()
                    }
                    Surface(
                        color = Color.Black.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 46.dp)
                    ) {
                        Text(
                            message,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                androidx.compose.foundation.pager.VerticalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                ) { page ->
                    VideoFeedItem(
                        video = state.videos[page],
                        isActive = isVisible && pagerState.settledPage == page,
                        animalMediaRevision = state.animalMediaRevision,
                        onLikeClick = viewModel::toggleLike,
                        onReportClick = { video, reason -> viewModel.reportVideo(video, reason) },
                        onReportProfileClick = { video, reason -> viewModel.reportAnimal(video, reason) },
                        onAnimalClick = { animalId -> navigator.push(AnimalDetailScreenData(animalId)) }
                    )
                }
            }
        }
    }
}

private fun tabPosition(tab: String): Int = when (tab) {
    "feed" -> 0
    "explore" -> 1
    "search" -> 2
    else -> 3
}

@Composable
private fun ExploreTab(viewModel: ExploreViewModel) {
    val navigator = LocalNavigator.currentOrThrow
    val sessionManager = koinInject<SessionManager>()
    val state by viewModel.uiState.collectAsState()
    val refreshRotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (state.isLoading) 360f else 0f,
        animationSpec = androidx.compose.animation.core.tween(650),
        label = "Explore refresh"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Background).statusBarsPadding(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.padding(start = 18.dp, end = 12.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Explorar", color = TextPrimary, style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
                    Text("Descubre ayuda cerca de ti", color = TextSecondary)
                }
                Surface(
                    onClick = { viewModel.load() },
                    shape = RoundedCornerShape(16.dp),
                    color = SurfaceRaised
                ) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Refresh,
                            "Actualizar",
                            tint = DustyRose,
                            modifier = Modifier.rotate(refreshRotation)
                        )
                    }
                }
            }
        }

        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { TonalChip("Cerca de ti", color = AdoptionGreen, icon = Icons.Default.MyLocation) }
                item { TonalChip("Eventos", color = CareLilac, icon = Icons.Default.Event) }
                item { TonalChip("Urgentes", color = CookieOrange, icon = Icons.Default.NotificationImportant) }
            }
        }

        item {
            if (state.isLoading && state.shelters.isEmpty()) {
                ExploreMapSkeleton(
                    modifier = Modifier
                        .padding(horizontal = 18.dp)
                        .fillMaxWidth()
                        .height(378.dp)
                )
            } else {
                ShelterMap(
                    shelters = state.shelters,
                    modifier = Modifier
                        .padding(horizontal = 18.dp)
                        .fillMaxWidth()
                        .height(378.dp)
                        .clip(RoundedCornerShape(24.dp))
                ) { shelter -> navigator.openShelter(shelter, sessionManager) }
            }
        }

        item {
            SectionHeading(
                title = "Albergues cerca de ti",
                modifier = Modifier.padding(horizontal = 18.dp)
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.shelters.isEmpty() && !state.isLoading) {
                    item { Text("No hay albergues para mostrar", color = TextSecondary) }
                } else {
                    itemsIndexed(
                        items = state.shelters.take(8),
                        key = { _, shelter -> shelter.id }
                    ) { index, shelter ->
                        StaggeredReveal(index = index, resetKey = state.shelters) {
                            ExploreShelterCard(shelter) { navigator.openShelter(shelter, sessionManager) }
                        }
                    }
                }
            }
        }

        if (state.errorMessage != null) {
            item { Text(state.errorMessage!!, color = Error, modifier = Modifier.padding(horizontal = 18.dp)) }
        }
    }
}

@Composable
private fun SearchTab(viewModel: SearchViewModel) {
    val navigator = LocalNavigator.currentOrThrow
    val sessionManager = koinInject<SessionManager>()
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.ensureLoaded()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
    ) {
        SearchTopBar(
            species = state.species,
            speciesOptions = state.speciesOptions,
            city = state.city,
            cities = state.cities,
            status = state.status,
            onSpecies = viewModel::updateSpecies,
            onCity = viewModel::updateCity,
            onStatus = viewModel::updateStatus,
            onSearch = { viewModel.search() }
        )
        ShelterCarousel(
            shelters = state.shelters,
            isLoading = state.isLoading,
            onShelterClick = { navigator.openShelter(it, sessionManager) }
        )
        if (state.isLoading && state.animals.isNotEmpty()) {
            LinearProgressIndicator(
                color = DustyRose,
                trackColor = SurfaceRaised,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text(
            "Animales que buscan hogar",
            color = TextPrimary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp)
        )
        Text(
            if (state.animals.isEmpty()) "Explora nuevas historias" else "${state.animals.size} perfiles para conocer",
            color = TextSecondary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)
        )
        state.errorMessage?.let { message ->
            Text(message, color = Error, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 112.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.isLoading && state.animals.isEmpty()) {
                items(6) {
                    AnimalGridSkeleton(Modifier.fillMaxWidth())
                }
            } else if (state.animals.isEmpty()) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(3) }) {
                    Text(
                        "No encontramos animales con estos filtros",
                        color = TextSecondary,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                itemsIndexed(
                    items = state.animals,
                    key = { _, animal -> animal.id }
                ) { index, animal ->
                    StaggeredReveal(index = index, resetKey = state.animals) {
                        AnimalGridCard(animal) { navigator.openAnimalFromSearch(animal, sessionManager) }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SearchTopBar(
    species: String,
    speciesOptions: List<SearchSpeciesDto>,
    city: String,
    cities: List<SearchCityDto>,
    status: String,
    onSpecies: (String) -> Unit,
    onCity: (String) -> Unit,
    onStatus: (String) -> Unit,
    onSearch: () -> Unit
) {
    var speciesMenuExpanded by remember { mutableStateOf(false) }
    var cityMenuExpanded by remember { mutableStateOf(false) }
    var filtersExpanded by remember { mutableStateOf(false) }
    val statusLabel = when (status) {
        "AVAILABLE" -> "Disponibles"
        "RECOVERING" -> "Recuperacion"
        else -> "Todos"
    }
    Surface(color = Background) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
        ) {
            Surface(
                color = Surface,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { filtersExpanded = !filtersExpanded }
                            .padding(horizontal = 13.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = DustyRose.copy(alpha = 0.16f),
                            contentColor = DustyRose,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Search, null, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                species.ifBlank { "Todas las especies" },
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${city.ifBlank { "Todas las ciudades" }} - $statusLabel",
                                color = TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            Icons.Default.ExpandMore,
                            contentDescription = if (filtersExpanded) "Cerrar filtros" else "Abrir filtros",
                            tint = TextSecondary,
                            modifier = Modifier.rotate(if (filtersExpanded) 180f else 0f)
                        )
                    }

                    AnimatedVisibility(
                        visible = filtersExpanded,
                        enter = expandVertically(
                            animationSpec = spring(dampingRatio = 0.82f, stiffness = 520f)
                        ) + fadeIn(),
                        exit = shrinkVertically(
                            animationSpec = spring(dampingRatio = 0.9f, stiffness = 620f)
                        ) + fadeOut()
                    ) {
                        Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                            HorizontalDivider(color = OutlineVariant)
                            Spacer(Modifier.height(10.dp))
                            ExposedDropdownMenuBox(
                                expanded = speciesMenuExpanded,
                                onExpandedChange = { speciesMenuExpanded = !speciesMenuExpanded }
                            ) {
                                OutlinedTextField(
                                    value = species.ifBlank { "Todas las especies" },
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    singleLine = true,
                                    label = { Text("Especie") },
                                    leadingIcon = { Icon(Icons.Default.Pets, null) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = speciesMenuExpanded) },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = searchTextColors()
                                )
                                ExposedDropdownMenu(
                                    expanded = speciesMenuExpanded,
                                    onDismissRequest = { speciesMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Todas las especies") },
                                        onClick = {
                                            onSpecies("")
                                            speciesMenuExpanded = false
                                        }
                                    )
                                    speciesOptions.forEach { item ->
                                        DropdownMenuItem(
                                            text = { Text(item.name) },
                                            onClick = {
                                                onSpecies(item.name)
                                                speciesMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    ExpressiveFilterChip(
                                        selected = status.isBlank(),
                                        label = "Todos",
                                        icon = Icons.Default.Pets,
                                        onClick = {
                                            onStatus("")
                                        }
                                    )
                                }
                                item {
                                    ExpressiveFilterChip(
                                        selected = status == "AVAILABLE",
                                        label = "Disponibles",
                                        icon = Icons.Default.Favorite,
                                        onClick = {
                                            onStatus("AVAILABLE")
                                        }
                                    )
                                }
                                item {
                                    ExpressiveFilterChip(
                                        selected = status == "RECOVERING",
                                        label = "Recuperacion",
                                        icon = Icons.Default.HealthAndSafety,
                                        onClick = {
                                            onStatus("RECOVERING")
                                        }
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            ExposedDropdownMenuBox(
                                expanded = cityMenuExpanded,
                                onExpandedChange = { cityMenuExpanded = !cityMenuExpanded }
                            ) {
                                OutlinedTextField(
                                    value = city.ifBlank { "Todas las ciudades" },
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                                    singleLine = true,
                                    label = { Text("Ciudad") },
                                    leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityMenuExpanded) },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = searchTextColors()
                                )
                                ExposedDropdownMenu(
                                    expanded = cityMenuExpanded,
                                    onDismissRequest = { cityMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Todas las ciudades") },
                                        onClick = {
                                            onCity("")
                                            cityMenuExpanded = false
                                        }
                                    )
                                    cities.forEach { item ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(item.name)
                                                    item.region?.takeIf { it != item.name }?.let {
                                                        Text(it, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                                    }
                                                }
                                            },
                                            onClick = {
                                                onCity(item.name)
                                                cityMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            TextButton(
                                onClick = {
                                    filtersExpanded = false
                                    speciesMenuExpanded = false
                                    cityMenuExpanded = false
                                    onSearch()
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(Icons.Default.Check, null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(5.dp))
                                Text("Listo")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShelterCarousel(
    shelters: List<ShelterProfileDto>,
    isLoading: Boolean,
    onShelterClick: (ShelterProfileDto) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Albergues registrados",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Conoce quienes cuidan de ellos",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Icon(Icons.Default.Home, null, tint = DustyRose, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isLoading && shelters.isEmpty()) {
                item {
                    SearchShelterSkeletons()
                }
            } else if (shelters.isEmpty()) {
                item {
                    Surface(
                        color = Surface,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Outline)
                    ) {
                        Text(
                            "No hay albergues para este filtro",
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = shelters,
                    key = { _, shelter -> shelter.id }
                ) { index, shelter ->
                    StaggeredReveal(index = index, resetKey = shelters) {
                        ShelterMiniCard(shelter) { onShelterClick(shelter) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpressiveFilterChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    val chipScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (selected) 1.035f else 1f,
        animationSpec = spring(dampingRatio = 0.68f, stiffness = 520f),
        label = "$label filter scale"
    )
    FilterChip(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.scale(chipScale),
        label = { Text(label, maxLines = 1) },
        leadingIcon = icon?.let { vector ->
            { Icon(vector, null, Modifier.size(16.dp)) }
        },
        shape = RoundedCornerShape(if (selected) 16.dp else 12.dp),
        colors = chipColors(),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = Outline,
            selectedBorderColor = DustyRose.copy(alpha = 0.55f)
        )
    )
}

private fun cafe.adriel.voyager.navigator.Navigator.openShelter(
    shelter: ShelterProfileDto,
    sessionManager: SessionManager
) {
    if (sessionManager.isShelter() && shelter.userId == sessionManager.getUserId()) {
        push(ShelterProfileScreen())
    } else {
        push(ShelterDetailScreen(shelter.id))
    }
}

private fun cafe.adriel.voyager.navigator.Navigator.openAnimalFromSearch(
    animal: AnimalDto,
    sessionManager: SessionManager
) {
    val isOwnShelterAnimal = sessionManager.isShelter() &&
        animal.shelter?.user?.id == sessionManager.getUserId()
    if (isOwnShelterAnimal) {
        push(ShelterProfileScreen())
        push(AnimalDetailScreenData(animal.id))
    } else {
        push(AnimalDetailScreenData(animal.id))
    }
}

@Composable
private fun ShelterMiniCard(shelter: ShelterProfileDto, onClick: () -> Unit) {
    ShelterProfileCard(
        shelter = shelter,
        modifier = Modifier.width(148.dp).height(136.dp),
        onClick = onClick
    )
}

@Composable
private fun AnimalGridCard(animal: AnimalDto, onClick: () -> Unit) {
    AnimalProfileCard(
        animal = animal,
        modifier = Modifier.fillMaxWidth().height(158.dp),
        onClick = onClick
    )
}

@Composable
private fun ExploreShelterCard(shelter: ShelterProfileDto, onClick: () -> Unit) {
    ShelterProfileCard(
        shelter = shelter,
        modifier = Modifier.width(136.dp).height(132.dp),
        onClick = onClick
    )
}

@Composable
private fun RetryState(message: String, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text(message, color = Color.White)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = DustyRose)) {
            Text("Reintentar", color = TextOnAccent)
        }
    }
}

@Composable
private fun EmptyCentered(title: String, body: String) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Pets, null, tint = DustyRose, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, color = Color.White)
        Text(body, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun searchTextColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = DustyRose,
    unfocusedBorderColor = Outline,
    focusedLeadingIconColor = DustyRose,
    unfocusedLeadingIconColor = TextSecondary
)

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = DustyRose,
    selectedLabelColor = TextOnAccent,
    labelColor = TextSecondary
)
