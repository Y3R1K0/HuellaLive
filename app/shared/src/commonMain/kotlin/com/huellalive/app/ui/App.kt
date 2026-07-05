package com.huellalive.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.CurrentScreen
import com.huellalive.app.data.local.SessionManager
import com.huellalive.app.data.repository.AuthRepository
import com.huellalive.app.data.repository.ExploreRepository
import com.huellalive.app.data.repository.FeedRepository
import com.huellalive.app.ui.animal.AnimalDetailScreenData
import com.huellalive.app.ui.admin.AdminSpeciesScreen
import com.huellalive.app.ui.auth.AuthChoiceScreen
import com.huellalive.app.ui.components.BottomNavBar
import com.huellalive.app.ui.explore.ExploreViewModel
import com.huellalive.app.ui.feed.FeedViewModel
import com.huellalive.app.ui.human.HumanProfileScreen
import com.huellalive.app.ui.main.MainScreen
import com.huellalive.app.ui.navigation.PlatformBackHandler
import com.huellalive.app.ui.search.SearchViewModel
import com.huellalive.app.ui.shelter.ShelterDetailScreen
import com.huellalive.app.ui.shelter.ShelterProfileScreen
import com.huellalive.app.ui.theme.Background
import com.huellalive.app.ui.theme.HuellaLiveTheme
import com.huellalive.app.ui.video.SelectAnimalForVideoScreen
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.compose.koinInject

@Composable
fun App() {
    HuellaLiveTheme {
        val feedRepository = koinInject<FeedRepository>()
        val exploreRepository = koinInject<ExploreRepository>()
        val authRepository = koinInject<AuthRepository>()
        val feedViewModel = remember { FeedViewModel(feedRepository, authRepository) }
        val exploreViewModel = remember { ExploreViewModel(exploreRepository) }
        val searchViewModel = remember { SearchViewModel(exploreRepository) }
        val mainTabState = remember { MutableStateFlow("feed") }

        Navigator(
            MainScreen(
                feedViewModel = feedViewModel,
                exploreViewModel = exploreViewModel,
                searchViewModel = searchViewModel,
                tabState = mainTabState
            ),
            onBackPressed = null
        ) { navigator ->
            val sessionManager = koinInject<SessionManager>()
            var selectedItem by remember { mutableStateOf("feed") }
            val sessionVersion by sessionManager.sessionVersion.collectAsState()
            val isLoggedIn = sessionManager.isLoggedIn()
            val userRole = sessionManager.getUserRole()

            fun resetSearchIfLeaving() {
                if (selectedItem == "search") searchViewModel.reset()
            }

            fun goToFeed() {
                resetSearchIfLeaving()
                selectedItem = "feed"
                mainTabState.value = "feed"
                if (navigator.lastItem is MainScreen) return
                navigator.popUntilRoot()
            }

            LaunchedEffect(sessionVersion) {
                if (!isLoggedIn && selectedItem == "profile") {
                    goToFeed()
                }
            }

            val currentScreen = navigator.lastItem
            val previousScreen = navigator.items.dropLast(1).lastOrNull()
            val shouldHandleBack = navigator.canPop || selectedItem != "feed"

            LaunchedEffect(currentScreen, sessionVersion) {
                when {
                    currentScreen is ShelterProfileScreen && selectedItem != "profile" -> {
                        resetSearchIfLeaving()
                        selectedItem = "profile"
                    }
                    currentScreen is AnimalDetailScreenData && previousScreen is ShelterProfileScreen && selectedItem != "profile" -> {
                        resetSearchIfLeaving()
                        selectedItem = "profile"
                    }
                    isLoggedIn && currentScreen is MainScreen && selectedItem == "profile" -> {
                        selectedItem = "feed"
                    }
                }
            }

            PlatformBackHandler(enabled = shouldHandleBack) {
                when {
                    currentScreen is AnimalDetailScreenData && previousScreen is ShelterDetailScreen -> {
                        navigator.pop()
                    }
                    currentScreen is ShelterDetailScreen && previousScreen is MainScreen -> {
                        navigator.pop()
                    }
                    currentScreen is ShelterDetailScreen -> {
                        goToFeed()
                    }
                    currentScreen is HumanProfileScreen ||
                        currentScreen is ShelterProfileScreen ||
                        currentScreen is AdminSpeciesScreen ||
                        currentScreen is AuthChoiceScreen ||
                        currentScreen is SelectAnimalForVideoScreen -> {
                        goToFeed()
                    }
                    currentScreen is MainScreen && selectedItem != "feed" -> {
                        goToFeed()
                    }
                    navigator.canPop -> {
                        navigator.pop()
                    }
                }
            }

            Scaffold(
                containerColor = Background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { padding ->
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(top = padding.calculateTopPadding())
                ) {
                    CurrentScreen()
                    BottomNavBar(
                        modifier = Modifier.align(Alignment.BottomCenter),
                        selectedItem = selectedItem,
                        isLoggedIn = isLoggedIn,
                        userRole = userRole,
                        onFeedClick = {
                            if (currentScreen is MainScreen) {
                                resetSearchIfLeaving()
                                selectedItem = "feed"
                                mainTabState.value = "feed"
                            } else if (selectedItem != "feed") {
                                goToFeed()
                            }
                        },
                        onExploreClick = {
                            if (currentScreen is MainScreen) {
                                resetSearchIfLeaving()
                                selectedItem = "explore"
                                mainTabState.value = "explore"
                            } else if (selectedItem != "explore") {
                                resetSearchIfLeaving()
                                mainTabState.value = "explore"
                                selectedItem = "explore"
                                navigator.popUntilRoot()
                            }
                        },
                        onSearchClick = {
                            if (currentScreen is MainScreen) {
                                selectedItem = "search"
                                mainTabState.value = "search"
                            } else if (selectedItem != "search") {
                                mainTabState.value = "search"
                                selectedItem = "search"
                                navigator.popUntilRoot()
                            }
                        },
                        onProfileClick = {
                            val isOnProfileRoot = currentScreen is HumanProfileScreen ||
                                currentScreen is ShelterProfileScreen ||
                                currentScreen is AdminSpeciesScreen
                            if (selectedItem != "profile" || !isOnProfileRoot) {
                                resetSearchIfLeaving()
                                selectedItem = "profile"
                                when (userRole) {
                                    "HUMAN" -> navigator.push(HumanProfileScreen())
                                    "SHELTER" -> navigator.push(ShelterProfileScreen())
                                    "ADMIN" -> navigator.push(AdminSpeciesScreen())
                                    else -> navigator.push(AuthChoiceScreen())
                                }
                            }
                        },
                        onLoginClick = {
                            if (selectedItem != "profile") {
                                resetSearchIfLeaving()
                                selectedItem = "profile"
                                navigator.push(AuthChoiceScreen())
                            }
                        },
                        onUploadClick = {
                            if (selectedItem != "upload") {
                                resetSearchIfLeaving()
                                selectedItem = "upload"
                                navigator.push(SelectAnimalForVideoScreen())
                            }
                        }
                    )
                }
            }
        }
    }
}
