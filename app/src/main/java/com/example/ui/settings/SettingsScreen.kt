package com.example.ui.settings

import android.app.Activity
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.drive.DriveAccountInfo
import com.example.data.drive.DriveBackupSummary
import com.example.data.drive.SyncStatus
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.VybeBlack
import com.example.ui.theme.VybeBorder
import com.example.ui.theme.VybeCoral
import com.example.ui.theme.VybeElectricCyan
import com.example.ui.theme.VybeOrange
import com.example.ui.theme.VybePink
import com.example.ui.theme.VybePurple
import com.example.ui.theme.VybeSurface
import com.example.ui.theme.VybeSurfaceVariant
import com.example.ui.theme.VybeTextMuted
import com.example.ui.theme.VybeTextPrimary
import com.example.ui.theme.VybeTextSecondary
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    // 1. Account Sync State
    val accountInfo by viewModel.accountInfo.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val backupSummary by viewModel.lastBackupSummary.collectAsStateWithLifecycle()
    val isCheckingBackup by viewModel.isCheckingBackup.collectAsStateWithLifecycle()
    val autoBackup by viewModel.autoBackup.collectAsStateWithLifecycle()

    // 2. Appearance State
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val accentColorHex by viewModel.accentColor.collectAsStateWithLifecycle()
    val dynamicTheme by viewModel.dynamicTheme.collectAsStateWithLifecycle()
    val useArtworkColors by viewModel.useArtworkColors.collectAsStateWithLifecycle()

    // 3. Audio State
    val streamingQuality by viewModel.streamingQuality.collectAsStateWithLifecycle()
    val equalizerEnabled by viewModel.equalizerEnabled.collectAsStateWithLifecycle()
    val equalizerPreset by viewModel.equalizerPreset.collectAsStateWithLifecycle()
    val equalizerBands by viewModel.equalizerBands.collectAsStateWithLifecycle()

    // 4. Playback State
    val autoplay by viewModel.autoplay.collectAsStateWithLifecycle()
    val gaplessPlayback by viewModel.gaplessPlayback.collectAsStateWithLifecycle()
    val crossfadeSeconds by viewModel.crossfadeSeconds.collectAsStateWithLifecycle()
    val repeatMode by viewModel.repeatMode.collectAsStateWithLifecycle()
    val startPlaybackOnLaunch by viewModel.startPlaybackOnLaunch.collectAsStateWithLifecycle()
    val continuePlaybackOnOpen by viewModel.continuePlaybackOnOpen.collectAsStateWithLifecycle()

    // 5. Library & Storage State
    val downloadQuality by viewModel.downloadQuality.collectAsStateWithLifecycle()
    val downloadWifiOnly by viewModel.downloadWifiOnly.collectAsStateWithLifecycle()
    val cacheSizeBytes by viewModel.cacheSizeBytes.collectAsStateWithLifecycle()
    val downloadSizeBytes by viewModel.downloadSizeBytes.collectAsStateWithLifecycle()
    val searchHistoryCount by viewModel.searchHistoryCount.collectAsStateWithLifecycle()
    val playHistoryCount by viewModel.playHistoryCount.collectAsStateWithLifecycle()

    // 6. Notifications State
    val playbackNotifications by viewModel.playbackNotifications.collectAsStateWithLifecycle()
    val downloadNotifications by viewModel.downloadNotifications.collectAsStateWithLifecycle()

    // Dialog state holders
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAccentColorDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showDownloadQualityDialog by remember { mutableStateOf(false) }
    var showEqualizerDialog by remember { mutableStateOf(false) }
    var showCrossfadeDialog by remember { mutableStateOf(false) }
    var showRepeatDialog by remember { mutableStateOf(false) }
    var showClearDownloadsDialog by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.data != null) {
            viewModel.handleSignInResult(result.data)
        } else {
            viewModel.clearStatus()
        }
    }

    val userRecoverableAuthIntent by viewModel.userRecoverableAuthIntent.collectAsStateWithLifecycle()
    val authRecoveryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onAuthRecoveryResult(result.resultCode == Activity.RESULT_OK)
    }

    LaunchedEffect(userRecoverableAuthIntent) {
        userRecoverableAuthIntent?.let { intent ->
            authRecoveryLauncher.launch(intent)
        }
    }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { viewModel.exportBackupToUri(it) }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importBackupFromUri(it) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .testTag("settings_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SettingsTopBar()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // =============================================================
                // 1. ACCOUNT SYNC
                // =============================================================
                item(key = "section_account_sync") {
                    SettingsSectionHeader(
                        title = "ACCOUNT SYNC",
                        subtitle = "Back up and restore your VYBE data",
                        icon = Icons.Default.CloudSync
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    AccountSyncCard(
                        accountInfo = accountInfo,
                        syncStatus = syncStatus,
                        backupSummary = backupSummary,
                        isCheckingBackup = isCheckingBackup,
                        autoBackup = autoBackup,
                        onConnectClick = {
                            val intent = viewModel.getSignInIntent()
                            googleSignInLauncher.launch(intent)
                        },
                        onConnectLocalClick = { viewModel.connectLocalSyncStorage() },
                        onDisconnectClick = { viewModel.disconnectGoogleDrive() },
                        onBackupNow = { viewModel.backupNow() },
                        onRestoreBackup = { viewModel.restoreData() },
                        onAutoBackupChange = { viewModel.setAutoBackup(it) },
                        onDismissStatus = { viewModel.clearStatus() },
                        onRefreshMetadata = { viewModel.refreshBackupMetadata() },
                        onExportClick = { exportBackupLauncher.launch("vybe_backup.json") },
                        onImportClick = { importBackupLauncher.launch(arrayOf("application/json", "text/*", "*/*")) },
                        onFallbackToLocal = { viewModel.connectLocalSyncStorage() }
                    )
                }

                // =============================================================
                // 2. APPEARANCE
                // =============================================================
                item(key = "section_appearance") {
                    SettingsSectionHeader(
                        title = "APPEARANCE",
                        subtitle = "Theme and visual customization",
                        icon = Icons.Outlined.Palette
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsCard {
                        SettingsRow(
                            title = "Theme",
                            description = "Color mode for app interface",
                            value = themeMode,
                            icon = Icons.Outlined.Palette,
                            onClick = { showThemeDialog = true },
                            testTag = "item_theme"
                        )

                        SettingsDivider()

                        SettingsRow(
                            title = "Accent Color",
                            description = "Primary highlight color across the app",
                            value = getAccentColorName(accentColorHex),
                            icon = Icons.Outlined.ColorLens,
                            onClick = { showAccentColorDialog = true },
                            testTag = "item_accent_color"
                        )

                        SettingsDivider()

                        SettingsSwitchRow(
                            title = "Dynamic Theme",
                            description = "Extract accent colors from system palette (Android 12+)",
                            checked = dynamicTheme,
                            onCheckedChange = { viewModel.setDynamicTheme(it) },
                            icon = Icons.Outlined.ColorLens,
                            testTag = "item_dynamic_theme"
                        )

                        SettingsDivider()

                        SettingsSwitchRow(
                            title = "Use album artwork colors",
                            description = "Adapt player ambient glow to the currently playing song artwork",
                            checked = useArtworkColors,
                            onCheckedChange = { viewModel.setUseArtworkColors(it) },
                            icon = Icons.Outlined.Brush,
                            testTag = "item_artwork_colors"
                        )
                    }
                }

                // =============================================================
                // 3. AUDIO
                // =============================================================
                item(key = "section_audio") {
                    SettingsSectionHeader(
                        title = "AUDIO",
                        subtitle = "Playback fidelity and sound enhancements",
                        icon = Icons.Default.GraphicEq
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsCard {
                        SettingsRow(
                            title = "Audio Quality",
                            description = "Streaming bitrate preference",
                            value = streamingQuality,
                            icon = Icons.Outlined.GraphicEq,
                            onClick = { showQualityDialog = true },
                            testTag = "item_audio_quality"
                        )

                        SettingsDivider()

                        SettingsRow(
                            title = "Equalizer",
                            description = "Presets and manual frequency control",
                            value = if (equalizerEnabled) equalizerPreset else "Off",
                            icon = Icons.Outlined.Tune,
                            onClick = { showEqualizerDialog = true },
                            testTag = "item_equalizer"
                        )
                    }
                }

                // =============================================================
                // 4. PLAYBACK
                // =============================================================
                item(key = "section_playback") {
                    SettingsSectionHeader(
                        title = "PLAYBACK",
                        subtitle = "Queue management and audio transitions",
                        icon = Icons.Default.PlayCircle
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsCard {
                        SettingsSwitchRow(
                            title = "Autoplay",
                            description = "Keep playing recommended music when queue finishes",
                            checked = autoplay,
                            onCheckedChange = { viewModel.setAutoplay(it) },
                            icon = Icons.Outlined.PlayCircle,
                            testTag = "item_autoplay"
                        )

                        SettingsDivider()

                        SettingsSwitchRow(
                            title = "Gapless Playback",
                            description = "Eliminate silence between consecutive songs",
                            checked = gaplessPlayback,
                            onCheckedChange = { viewModel.setGaplessPlayback(it) },
                            icon = Icons.Outlined.QueueMusic,
                            testTag = "item_gapless"
                        )

                        SettingsDivider()

                        SettingsRow(
                            title = "Crossfade",
                            description = "Fade out ending song and fade in next",
                            value = if (crossfadeSeconds == 0) "Off" else "$crossfadeSeconds sec",
                            icon = Icons.Outlined.SyncAlt,
                            onClick = { showCrossfadeDialog = true },
                            testTag = "item_crossfade"
                        )

                        SettingsDivider()

                        SettingsRow(
                            title = "Repeat Mode",
                            description = "Default queue repeat policy",
                            value = repeatMode,
                            icon = Icons.Outlined.Repeat,
                            onClick = { showRepeatDialog = true },
                            testTag = "item_repeat_mode"
                        )

                        SettingsDivider()

                        SettingsSwitchRow(
                            title = "Start playback on app launch",
                            description = "Automatically resume last played song on launch",
                            checked = startPlaybackOnLaunch,
                            onCheckedChange = { viewModel.setStartPlaybackOnLaunch(it) },
                            icon = Icons.Outlined.FlashOn,
                            testTag = "item_start_launch"
                        )

                        SettingsDivider()

                        SettingsSwitchRow(
                            title = "Continue playback when app opens",
                            description = "Preserve ongoing queue state across sessions",
                            checked = continuePlaybackOnOpen,
                            onCheckedChange = { viewModel.setContinuePlaybackOnOpen(it) },
                            icon = Icons.Outlined.History,
                            testTag = "item_continue_open"
                        )
                    }
                }

                // =============================================================
                // 5. LIBRARY & STORAGE
                // =============================================================
                item(key = "section_library_storage") {
                    SettingsSectionHeader(
                        title = "LIBRARY & STORAGE",
                        subtitle = "Downloads and storage management",
                        icon = Icons.Default.Storage
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsCard {
                        // Download Location
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Folder,
                                contentDescription = null,
                                tint = VybeTextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Download Location",
                                    color = VybeTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "App Internal Storage (Private)",
                                    color = VybeOrange,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "User data is stored on the user's device.",
                                    color = VybeTextMuted,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        SettingsDivider()

                        SettingsRow(
                            title = "Download Quality",
                            description = "Audio bitrate for offline music files",
                            value = downloadQuality,
                            icon = Icons.Outlined.GraphicEq,
                            onClick = { showDownloadQualityDialog = true },
                            testTag = "item_download_quality"
                        )

                        SettingsDivider()

                        SettingsSwitchRow(
                            title = "Download over Wi-Fi only",
                            description = "Prevent downloads on mobile data to save cellular bandwidth",
                            checked = downloadWifiOnly,
                            onCheckedChange = { viewModel.setDownloadWifiOnly(it) },
                            icon = Icons.Outlined.SyncAlt,
                            testTag = "item_download_wifi_only"
                        )

                        SettingsDivider()

                        // Storage Used Breakdown
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                text = "Storage Used",
                                color = VybeTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                StorageMetricItem(
                                    label = "Cache",
                                    value = formatStorage(cacheSizeBytes),
                                    color = VybePink
                                )
                                StorageMetricItem(
                                    label = "Downloads",
                                    value = formatStorage(downloadSizeBytes),
                                    color = VybeElectricCyan
                                )
                                StorageMetricItem(
                                    label = "Total",
                                    value = formatStorage(cacheSizeBytes + downloadSizeBytes),
                                    color = VybeOrange
                                )
                            }
                        }

                        SettingsDivider()

                        // Action: Clear Cache
                        ActionRow(
                            title = "Clear Cache",
                            description = "Removes temporary images and query cache (${formatStorage(cacheSizeBytes)})",
                            buttonText = "Clear",
                            onClick = { viewModel.clearCache() },
                            testTag = "btn_clear_cache"
                        )

                        SettingsDivider()

                        // Action: Clear Downloaded Songs
                        ActionRow(
                            title = "Clear Downloaded Songs",
                            description = "Delete all offline tracks (${formatStorage(downloadSizeBytes)})",
                            buttonText = "Delete",
                            buttonColor = Color(0xFFEF4444),
                            onClick = { showClearDownloadsDialog = true },
                            testTag = "btn_clear_downloads"
                        )

                        SettingsDivider()

                        // Action: Clear Play History
                        ActionRow(
                            title = "Clear Play History",
                            description = "Reset $playHistoryCount listening records",
                            buttonText = "Clear",
                            onClick = { showClearHistoryDialog = true },
                            testTag = "btn_clear_history"
                        )

                        SettingsDivider()

                        // Action: Clear Search History
                        ActionRow(
                            title = "Clear Search History",
                            description = "Remove $searchHistoryCount recent search queries",
                            buttonText = "Clear",
                            onClick = { viewModel.clearSearchHistory() },
                            testTag = "btn_clear_search"
                        )
                    }
                }

                // =============================================================
                // 6. NOTIFICATIONS
                // =============================================================
                item(key = "section_notifications") {
                    SettingsSectionHeader(
                        title = "NOTIFICATIONS",
                        subtitle = "System alerts and playback controls",
                        icon = Icons.Default.Notifications
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsCard {
                        SettingsSwitchRow(
                            title = "Playback Notifications",
                            description = "Show media playback controls in notification shade and lockscreen",
                            checked = playbackNotifications,
                            onCheckedChange = { viewModel.setPlaybackNotifications(it) },
                            icon = Icons.Outlined.Notifications,
                            testTag = "item_playback_notif"
                        )

                        SettingsDivider()

                        SettingsSwitchRow(
                            title = "Download Notifications",
                            description = "Show notification alert when an offline music download finishes",
                            checked = downloadNotifications,
                            onCheckedChange = { viewModel.setDownloadNotifications(it) },
                            icon = Icons.Outlined.DownloadDone,
                            testTag = "item_download_notif"
                        )
                    }
                }

                // =============================================================
                // 7. ABOUT
                // =============================================================
                item(key = "section_about") {
                    SettingsSectionHeader(
                        title = "ABOUT",
                        subtitle = "Application details and licenses",
                        icon = Icons.Default.Info
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SettingsCard {
                        // Branded Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(listOf(VybeOrange, VybePink))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "V",
                                    color = Color.White,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = OutfitFontFamily
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column {
                                Text(
                                    text = "VYBE",
                                    color = VybeTextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = OutfitFontFamily
                                )
                                Text(
                                    text = "Premium Streaming & Offline Music",
                                    color = VybeTextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        SettingsDivider()

                        SettingsRow(
                            title = "Version",
                            value = "1.0.0 (Build 100)",
                            icon = Icons.Default.Info,
                            testTag = "item_version"
                        )

                        SettingsDivider()

                        SettingsRow(
                            title = "Open Source Licenses",
                            description = "Libraries powering VYBE",
                            icon = Icons.Outlined.Policy,
                            onClick = { showLicensesDialog = true },
                            testTag = "item_licenses"
                        )

                        SettingsDivider()

                        SettingsRow(
                            title = "Privacy",
                            description = "How your local data is protected",
                            icon = Icons.Outlined.Policy,
                            onClick = { showPrivacyDialog = true },
                            testTag = "item_privacy"
                        )

                        SettingsDivider()

                        SettingsRow(
                            title = "Terms",
                            description = "Terms of service and usage conditions",
                            icon = Icons.Outlined.Policy,
                            onClick = { showTermsDialog = true },
                            testTag = "item_terms"
                        )
                    }

                    // Bottom padding for floating navigation bar
                    Spacer(modifier = Modifier.height(140.dp))
                }
            }
        }
    }

    // =============================================================
    // MODALS & DIALOGS
    // =============================================================

    // Equalizer Dialog
    if (showEqualizerDialog) {
        SettingsEqualizerDialog(
            enabled = equalizerEnabled,
            selectedPreset = equalizerPreset,
            bands = equalizerBands,
            onEnabledChange = { viewModel.setEqualizerEnabled(it) },
            onPresetSelected = { viewModel.setEqualizerPreset(it) },
            onBandChange = { band, value -> viewModel.setEqualizerBand(band, value) },
            onReset = { viewModel.resetEqualizer() },
            onDismiss = { showEqualizerDialog = false }
        )
    }

    // Theme Selection Dialog
    if (showThemeDialog) {
        val themeOptions = listOf("Dark", "Light", "System Default")
        SelectionDialog(
            title = "Choose Theme",
            options = themeOptions,
            selectedOption = themeMode,
            onOptionSelected = {
                viewModel.setThemeMode(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }

    // Accent Color Selection Dialog
    if (showAccentColorDialog) {
        val accentOptions = listOf(
            Triple("Vybe Orange", "#FF6436", VybeOrange),
            Triple("Sunset Coral", "#FF5252", VybeCoral),
            Triple("Neon Pink", "#F43F5E", VybePink),
            Triple("Electric Cyan", "#06B6D4", VybeElectricCyan),
            Triple("Deep Violet", "#8B5CF6", VybePurple),
            Triple("Emerald Green", "#10B981", Color(0xFF10B981)),
            Triple("Amber Gold", "#F59E0B", Color(0xFFF59E0B)),
            Triple("Ocean Blue", "#3B82F6", Color(0xFF3B82F6))
        )
        AlertDialog(
            onDismissRequest = { showAccentColorDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "Choose Accent Color",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = OutfitFontFamily
                )
            },
            text = {
                Column {
                    accentOptions.forEach { (name, hex, color) ->
                        val isSelected = accentColorHex.equals(hex, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setAccentColor(hex)
                                    showAccentColorDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(2.dp, if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = name,
                                color = if (isSelected) color else MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = color,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccentColorDialog = false }) {
                    Text(text = "Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Audio Quality Dialog
    if (showQualityDialog) {
        val qualityOptions = listOf(
            "Auto",
            "Low (96 kbps)",
            "Medium (160 kbps)",
            "High (320 kbps)"
        )
        SelectionDialog(
            title = "Select Streaming Quality",
            options = qualityOptions,
            selectedOption = streamingQuality,
            onOptionSelected = {
                viewModel.setStreamingQuality(it)
                showQualityDialog = false
            },
            onDismiss = { showQualityDialog = false }
        )
    }

    // Download Quality Dialog
    if (showDownloadQualityDialog) {
        val downloadQualityOptions = listOf(
            "High (320 kbps)",
            "Medium (160 kbps)",
            "Low (96 kbps)"
        )
        SelectionDialog(
            title = "Download Audio Quality",
            options = downloadQualityOptions,
            selectedOption = downloadQuality,
            onOptionSelected = {
                viewModel.setDownloadQuality(it)
                showDownloadQualityDialog = false
            },
            onDismiss = { showDownloadQualityDialog = false }
        )
    }

    // Crossfade Dialog
    if (showCrossfadeDialog) {
        val crossfadeOptions = listOf("Off", "2 sec", "4 sec", "6 sec", "8 sec", "10 sec")
        val currentLabel = if (crossfadeSeconds == 0) "Off" else "$crossfadeSeconds sec"
        SelectionDialog(
            title = "Crossfade Duration",
            options = crossfadeOptions,
            selectedOption = currentLabel,
            onOptionSelected = {
                val sec = when (it) {
                    "2 sec" -> 2
                    "4 sec" -> 4
                    "6 sec" -> 6
                    "8 sec" -> 8
                    "10 sec" -> 10
                    else -> 0
                }
                viewModel.setCrossfadeSeconds(sec)
                showCrossfadeDialog = false
            },
            onDismiss = { showCrossfadeDialog = false }
        )
    }

    // Repeat Mode Dialog
    if (showRepeatDialog) {
        val repeatOptions = listOf("Off", "All", "One")
        SelectionDialog(
            title = "Default Repeat Mode",
            options = repeatOptions,
            selectedOption = repeatMode,
            onOptionSelected = {
                viewModel.setRepeatMode(it)
                showRepeatDialog = false
            },
            onDismiss = { showRepeatDialog = false }
        )
    }

    // Clear Downloads Confirmation Dialog
    if (showClearDownloadsDialog) {
        ConfirmationDialog(
            title = "Delete Downloaded Songs?",
            message = "This will remove all downloaded offline audio files from your device storage (${formatStorage(downloadSizeBytes)}). You can re-download them anytime.",
            confirmText = "Delete All",
            isDestructive = true,
            onConfirm = {
                viewModel.clearDownloadedSongs()
                showClearDownloadsDialog = false
            },
            onDismiss = { showClearDownloadsDialog = false }
        )
    }

    // Clear Play History Confirmation Dialog
    if (showClearHistoryDialog) {
        ConfirmationDialog(
            title = "Clear Listening History?",
            message = "This will erase your past song listening records and play counts. Your liked songs and custom playlists will NOT be affected.",
            confirmText = "Clear History",
            isDestructive = true,
            onConfirm = {
                viewModel.clearPlayHistory()
                showClearHistoryDialog = false
            },
            onDismiss = { showClearHistoryDialog = false }
        )
    }

    // Open Source Licenses Dialog
    if (showLicensesDialog) {
        InfoDialog(
            title = "Open Source Licenses",
            content = """
                VYBE is built with open source software:

                • Jetpack Compose & Material 3 (Apache 2.0)
                • AndroidX Media3 / ExoPlayer (Apache 2.0)
                • Room Persistence Library (Apache 2.0)
                • Kotlin Coroutines & Serialization (Apache 2.0)
                • Coil Image Loading (Apache 2.0)
                • Square Retrofit & OkHttp (Apache 2.0)
                • Square Moshi (Apache 2.0)
                • Google Play Services Auth (Apache 2.0)
            """.trimIndent(),
            onDismiss = { showLicensesDialog = false }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyDialog) {
        InfoDialog(
            title = "Privacy Policy",
            content = """
                Your privacy is fundamental to VYBE:

                1. Local-First: Your listening history, playlists, favorites, and settings are stored locally on your device.
                2. Private Cloud Backup: If enabled, backups are stored exclusively in your personal Google Drive hidden app data folder (appDataFolder). Nobody else, including VYBE, can access your backups.
                3. Zero Tracking: VYBE does not collect personal identifiers, sell your data, or serve targeted third-party advertising.
                4. Offline Audio: Downloaded music files remain strictly on your local device storage.
            """.trimIndent(),
            onDismiss = { showPrivacyDialog = false }
        )
    }

    // Terms of Service Dialog
    if (showTermsDialog) {
        InfoDialog(
            title = "Terms of Service",
            content = """
                Terms and Conditions for using VYBE:

                • VYBE is provided for personal, non-commercial entertainment use.
                • Music content and metadata are streamed from public API sources. All rights belong to their respective copyright holders.
                • You are responsible for maintaining your Google Drive account access for optional backup synchronization.
                • The application is provided "as is" without warranties of any kind.
            """.trimIndent(),
            onDismiss = { showTermsDialog = false }
        )
    }
}

// =============================================================
// SUB-COMPONENTS
// =============================================================

@Composable
private fun SettingsTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Settings",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = OutfitFontFamily
            )
            Text(
                text = "Preferences, data sync & accounts",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "v1.0",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun AccountSyncCard(
    accountInfo: DriveAccountInfo?,
    syncStatus: SyncStatus,
    backupSummary: DriveBackupSummary?,
    isCheckingBackup: Boolean,
    autoBackup: Boolean,
    onConnectClick: () -> Unit,
    onConnectLocalClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onBackupNow: () -> Unit,
    onRestoreBackup: () -> Unit,
    onAutoBackupChange: (Boolean) -> Unit,
    onDismissStatus: () -> Unit,
    onRefreshMetadata: () -> Unit,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onFallbackToLocal: () -> Unit
) {
    SettingsCard {
        // Status Alerts Banner
        AnimatedVisibility(
            visible = syncStatus !is SyncStatus.Idle,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            SyncStatusBanner(
                status = syncStatus,
                onDismiss = onDismissStatus,
                onRetry = onBackupNow,
                onFallbackToLocal = onFallbackToLocal
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Account connection state
        if (accountInfo != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!accountInfo.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = accountInfo.photoUrl,
                            contentDescription = accountInfo.displayName,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (accountInfo.isLocalSync) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Text(
                                    text = accountInfo.displayName.take(1).uppercase(),
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = accountInfo.displayName,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = accountInfo.email,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (accountInfo.isLocalSync) "Device Sync Storage (Active)" else "Connected to Google Drive",
                                color = Color(0xFF10B981),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                TextButton(
                    onClick = onDisconnectClick,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Text(text = "Disconnect", fontSize = 12.sp)
                }
            }
        } else {
            // Not connected state
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Connect Google Drive & Sync Storage",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Back up your playlists, favorites, and settings securely into Google Drive or private device sync storage.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onConnectClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_connect_drive"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Google Drive",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onConnectLocalClick,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_connect_local"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Device Storage",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                TextButton(
                    onClick = onImportClick,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Import Existing Backup (.json)", fontSize = 12.sp)
                }
            }
        }

        SettingsDivider()

        // Backup Info & Date
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Last Backup",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (backupSummary != null) {
                    val dateStr = DateFormat.format("MMM d, yyyy, h:mm a", Date(backupSummary.modifiedTime)).toString()
                    Text(
                        text = "$dateStr (${backupSummary.totalSongs} songs, ${backupSummary.totalPlaylists} playlists)",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Source: ${backupSummary.source}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                } else if (isCheckingBackup) {
                    Text(
                        text = "Checking backup status...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                } else {
                    Text(
                        text = "Your data hasn't been backed up yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            if (accountInfo != null) {
                IconButton(onClick = onRefreshMetadata, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Action Buttons: Backup Now & Restore Backup
        if (accountInfo != null) {
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onBackupNow,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_backup_now"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = syncStatus !is SyncStatus.BackingUp && syncStatus !is SyncStatus.Restoring
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Backup Now", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onRestoreBackup,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_restore_backup"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    enabled = backupSummary != null && syncStatus !is SyncStatus.BackingUp && syncStatus !is SyncStatus.Restoring
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Restore", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    onClick = onExportClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Export JSON", fontSize = 12.sp)
                }

                TextButton(
                    onClick = onImportClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Import JSON", fontSize = 12.sp)
                }
            }
        }

        SettingsDivider()

        // Auto Backup toggle
        SettingsSwitchRow(
            title = "Auto Backup",
            description = "Automatically save changes when connected to Wi-Fi",
            checked = autoBackup,
            onCheckedChange = onAutoBackupChange,
            testTag = "toggle_auto_backup"
        )
    }
}

@Composable
private fun SyncStatusBanner(
    status: SyncStatus,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onFallbackToLocal: () -> Unit
) {
    val (bgColor, borderColor, icon, text) = when (status) {
        is SyncStatus.Connecting -> Quad(
            VybeSurfaceVariant, VybeBorder,
            Icons.Default.CloudSync, "Connecting to Google Drive..."
        )
        is SyncStatus.BackingUp -> Quad(
            VybeOrange.copy(alpha = 0.15f), VybeOrange.copy(alpha = 0.4f),
            Icons.Default.CloudUpload, status.message
        )
        is SyncStatus.Restoring -> Quad(
            VybePink.copy(alpha = 0.15f), VybePink.copy(alpha = 0.4f),
            Icons.Default.CloudDownload, status.message
        )
        is SyncStatus.Success -> Quad(
            Color(0xFF064E3B).copy(alpha = 0.4f), Color(0xFF10B981).copy(alpha = 0.5f),
            Icons.Default.CheckCircle, status.message
        )
        is SyncStatus.Error -> Quad(
            Color(0xFF7F1D1D).copy(alpha = 0.4f), Color(0xFFEF4444).copy(alpha = 0.5f),
            Icons.Default.ErrorOutline, status.message
        )
        else -> Quad(VybeSurfaceVariant, VybeBorder, Icons.Default.Info, "")
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (status is SyncStatus.BackingUp || status is SyncStatus.Restoring || status is SyncStatus.Connecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (status is SyncStatus.Success) Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = text,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Dismiss",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (status is SyncStatus.Error && status.canFallbackToLocal) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onFallbackToLocal,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Use Device Sync Storage (Instant Offline)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (status is SyncStatus.BackingUp || status is SyncStatus.Restoring) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = VybeSurfaceVariant
                )
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
private fun StorageMetricItem(
    label: String,
    value: String,
    color: Color
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, color = VybeTextMuted, fontSize = 12.sp)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            color = VybeTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ActionRow(
    title: String,
    description: String,
    buttonText: String,
    buttonColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        OutlinedButton(
            onClick = onClick,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = buttonColor),
            modifier = Modifier.then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
        ) {
            Text(text = buttonText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SelectionDialog(
    title: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = OutfitFontFamily
            )
        },
        text = {
            Column {
                options.forEach { option ->
                    val isSelected = option == selectedOption
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOptionSelected(option) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onOptionSelected(option) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = primaryColor,
                                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = option,
                            color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String,
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = OutfitFontFamily
            )
        },
        text = {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) Color(0xFFEF4444) else primaryColor
                )
            ) {
                Text(text = confirmText, color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun InfoDialog(
    title: String,
    content: String,
    onDismiss: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = OutfitFontFamily
            )
        },
        text = {
            Text(
                text = content,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.verticalScroll(rememberScrollState())
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Close", color = primaryColor, fontWeight = FontWeight.Bold)
            }
        }
    )
}

private fun getAccentColorName(hex: String): String {
    return when (hex.uppercase()) {
        "#FF6436" -> "Vybe Orange"
        "#FF5252" -> "Sunset Coral"
        "#F43F5E" -> "Neon Pink"
        "#06B6D4" -> "Electric Cyan"
        "#8B5CF6" -> "Deep Violet"
        "#10B981" -> "Emerald Green"
        "#F59E0B" -> "Amber Gold"
        "#3B82F6" -> "Ocean Blue"
        else -> "Custom ($hex)"
    }
}

private fun formatStorage(bytes: Long): String {
    return when {
        bytes >= 1024 * 1024 * 1024 -> String.format(Locale.US, "%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
        bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
