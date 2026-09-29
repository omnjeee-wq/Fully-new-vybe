package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.player.VybePlaybackService
import com.example.ui.home.HomeViewModel
import com.example.ui.library.LibraryViewModel
import com.example.ui.navigation.MainAppScaffold
import com.example.ui.search.SearchViewModel
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.VybeBlack
import com.example.ui.theme.VybeTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private val openPlayerTrigger = MutableStateFlow(false)

    private val homeViewModel: HomeViewModel by lazy {
        val app = VybeApp.instance
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(
                    repository = app.repository,
                    recommendationEngine = app.recommendationEngine,
                    playerManager = app.playerManager
                ) as T
            }
        })[HomeViewModel::class.java]
    }

    private val searchViewModel: SearchViewModel by lazy {
        val app = VybeApp.instance
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SearchViewModel(
                    repository = app.repository
                ) as T
            }
        })[SearchViewModel::class.java]
    }

    private val libraryViewModel: LibraryViewModel by lazy {
        val app = VybeApp.instance
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return LibraryViewModel(
                    repository = app.repository,
                    playerManager = app.playerManager
                ) as T
            }
        })[LibraryViewModel::class.java]
    }

    private val settingsViewModel: SettingsViewModel by lazy {
        val app = VybeApp.instance
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(
                    driveManager = app.driveManager,
                    songDao = app.database.songDao(),
                    appContext = applicationContext
                ) as T
            }
        })[SettingsViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        val app = VybeApp.instance

        setContent {
            VybeTheme {
                val shouldOpenPlayer by openPlayerTrigger.collectAsStateWithLifecycle()

                // Request notification permission on Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { _ -> }

                    LaunchedEffect(Unit) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(VybeBlack),
                    color = VybeBlack
                ) {
                    MainAppScaffold(
                        homeViewModel = homeViewModel,
                        searchViewModel = searchViewModel,
                        libraryViewModel = libraryViewModel,
                        settingsViewModel = settingsViewModel,
                        playerManager = app.playerManager,
                        initialOpenPlayer = shouldOpenPlayer,
                        onPlayerOpenedHandled = { openPlayerTrigger.value = false }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(VybePlaybackService.EXTRA_OPEN_PLAYER, false) == true) {
            openPlayerTrigger.value = true
        }
    }
}
