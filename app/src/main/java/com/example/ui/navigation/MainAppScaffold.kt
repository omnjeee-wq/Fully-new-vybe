package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.VybeApp
import com.example.player.PlayerManager
import com.example.ui.components.FloatingBottomBar
import com.example.ui.components.NavDestination
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.library.LibraryScreen
import com.example.ui.library.LibraryViewModel
import com.example.ui.placeholder.PlaceholderScreen
import com.example.ui.player.PlayerScreen
import com.example.ui.search.SearchScreen
import com.example.ui.search.SearchViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.VybeBlack
import kotlinx.coroutines.launch

@Composable
fun MainAppScaffold(
    homeViewModel: HomeViewModel,
    searchViewModel: SearchViewModel,
    libraryViewModel: LibraryViewModel,
    settingsViewModel: SettingsViewModel,
    playerManager: PlayerManager,
    modifier: Modifier = Modifier,
    initialOpenPlayer: Boolean = false,
    onPlayerOpenedHandled: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var currentDestination by remember { mutableStateOf(NavDestination.HOME) }
    var isPlayerExpanded by remember { mutableStateOf(initialOpenPlayer) }

    val playerState by playerManager.playerState.collectAsStateWithLifecycle()

    LaunchedEffect(initialOpenPlayer) {
        if (initialOpenPlayer) {
            isPlayerExpanded = true
            onPlayerOpenedHandled()
        }
    }

    // Handle system back press
    BackHandler(enabled = isPlayerExpanded || currentDestination != NavDestination.HOME) {
        if (isPlayerExpanded) {
            isPlayerExpanded = false
        } else {
            currentDestination = NavDestination.HOME
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(VybeBlack)
    ) {
        // Active Destination Content with cinematic smooth cross-fade animation
        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = {
                fadeIn(
                    animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                ) togetherWith fadeOut(
                    animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
                )
            },
            label = "screen_navigation_crossfade",
            modifier = Modifier.fillMaxSize()
        ) { destination ->
            when (destination) {
                NavDestination.HOME -> {
                    HomeScreen(
                        viewModel = homeViewModel,
                        playerManager = playerManager,
                        onOpenFullPlayer = { isPlayerExpanded = true }
                    )
                }
                NavDestination.SEARCH -> {
                    SearchScreen(
                        viewModel = searchViewModel,
                        playerManager = playerManager
                    )
                }
                NavDestination.LIBRARY -> {
                    LibraryScreen(
                        viewModel = libraryViewModel,
                        playerManager = playerManager
                    )
                }
                NavDestination.SETTINGS -> {
                    SettingsScreen(
                        viewModel = settingsViewModel
                    )
                }
            }
        }

        // Floating Bottom Navigation + Mini-Player
        AnimatedVisibility(
            visible = !isPlayerExpanded,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            FloatingBottomBar(
                currentDestination = currentDestination,
                playerState = playerState,
                onNavigate = { currentDestination = it },
                onOpenFullPlayer = { isPlayerExpanded = true },
                onPlayPause = { playerManager.togglePlayPause() },
                onNext = { playerManager.next() }
            )
        }

        // Full Screen Player Modal with Slide-up Animation
        AnimatedVisibility(
            visible = isPlayerExpanded,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            PlayerScreen(
                playerManager = playerManager,
                onCollapse = { isPlayerExpanded = false },
                onToggleFavorite = { song ->
                    coroutineScope.launch {
                        VybeApp.instance.repository.toggleFavorite(song)
                    }
                }
            )
        }
    }
}
