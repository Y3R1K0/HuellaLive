package com.huellalive.app.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.huellalive.app.data.local.SessionManager
import com.huellalive.app.data.repository.AuthRepository
import com.huellalive.app.data.repository.FeedRepository
import com.huellalive.app.ui.animal.AnimalDetailScreenData
import com.huellalive.app.ui.auth.AuthChoiceScreen
import com.huellalive.app.ui.components.BottomNavBar
import com.huellalive.app.ui.theme.*
import org.koin.compose.koinInject
import com.huellalive.app.ui.human.HumanProfileScreen
import com.huellalive.app.ui.shelter.ShelterProfileScreen
import com.huellalive.app.ui.explore.ExploreScreen
import com.huellalive.app.ui.search.SearchScreen

class FeedScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val feedRepository = koinInject<FeedRepository>()
        val authRepository = koinInject<AuthRepository>()
        val sessionManager = koinInject<SessionManager>()
        val viewModel = rememberScreenModel { FeedViewModel(feedRepository, authRepository) }
        val state by viewModel.uiState.collectAsState()
        val isLoggedIn = sessionManager.isLoggedIn()
        val userRole = sessionManager.getUserRole()

        val pagerState = rememberPagerState { state.videos.size }

        LaunchedEffect(pagerState.currentPage) {
            if (state.videos.isNotEmpty() && pagerState.currentPage >= state.videos.size - 3) {
                viewModel.loadMore()
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            when {
                state.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = DustyRose
                )
                state.errorMessage != null -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.errorMessage!!, color = Color.White)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadFeed() },
                        colors = ButtonDefaults.buttonColors(containerColor = DustyRose)) {
                        Text("Reintentar", color = TextOnAccent)
                    }
                }
                state.videos.isEmpty() -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🐾", fontSize = 64.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("No hay videos aún", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    Text("Los videos aparecerán aquí", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                }
                else -> VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    VideoFeedItem(
                        video = state.videos[page],
                        isActive = pagerState.currentPage == page,
                        onAnimalClick = { animalId -> navigator.push(AnimalDetailScreenData(animalId)) }
                    )
                }
            }

            BottomNavBar(
                modifier = Modifier.align(Alignment.BottomCenter),
                isLoggedIn = isLoggedIn,
                userRole = userRole,
                onFeedClick = {},
                onProfileClick = {
                    when (userRole) {
                        "HUMAN"   -> navigator.push(HumanProfileScreen())
                        "SHELTER" -> navigator.push(ShelterProfileScreen())
                        else      -> navigator.push(AuthChoiceScreen())
                    }
                },
                onLoginClick = { navigator.push(AuthChoiceScreen()) },
                onUploadClick = {}
            )
        }
    }
}
