package com.example.data.drive

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.AppPreferenceEntity
import com.example.data.local.SongDao
import com.example.data.local.VybeDatabase
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

private const val TAG = "GoogleDriveManager"
private const val DRIVE_APP_DATA_SCOPE_STR = "https://www.googleapis.com/auth/drive.appdata"
private const val PREF_LAST_BACKUP_TIMESTAMP = "drive_last_backup_timestamp"
private const val PREF_LAST_BACKUP_ITEMS = "drive_last_backup_items"
private const val PREF_LAST_BACKUP_SOURCE = "drive_last_backup_source"
private const val PREF_ACCOUNT_TYPE = "drive_account_type"
private const val PREF_ACCOUNT_EMAIL = "drive_account_email"
private const val PREF_ACCOUNT_NAME = "drive_account_name"
private const val PREF_ACCOUNT_PHOTO = "drive_account_photo"
private const val LOCAL_BACKUP_FILENAME = "vybe_backup.json"

class GoogleDriveManager(
    private val context: Context,
    private val database: VybeDatabase,
    private val coroutineScope: CoroutineScope
) {
    private val songDao: SongDao = database.songDao()

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val backupAdapter = moshi.adapter(VybeBackupData::class.java)

    private val driveScope = Scope(DRIVE_APP_DATA_SCOPE_STR)

    private val googleSignInOptions: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestProfile()
        .requestScopes(driveScope)
        .build()

    val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(context, googleSignInOptions)

    private val _accountInfo = MutableStateFlow<DriveAccountInfo?>(null)
    val accountInfo: StateFlow<DriveAccountInfo?> = _accountInfo.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastBackupSummary = MutableStateFlow<DriveBackupSummary?>(null)
    val lastBackupSummary: StateFlow<DriveBackupSummary?> = _lastBackupSummary.asStateFlow()

    private val _isCheckingBackup = MutableStateFlow(false)
    val isCheckingBackup: StateFlow<Boolean> = _isCheckingBackup.asStateFlow()

    private val _userRecoverableAuthIntent = MutableStateFlow<Intent?>(null)
    val userRecoverableAuthIntent: StateFlow<Intent?> = _userRecoverableAuthIntent.asStateFlow()

    private enum class PendingAction { NONE, BACKUP, RESTORE }
    private var pendingAction: PendingAction = PendingAction.NONE

    private val localSyncFolder: File
        get() = File(context.filesDir, "sync_storage").apply { if (!exists()) mkdirs() }

    private val localBackupFile: File
        get() = File(localSyncFolder, LOCAL_BACKUP_FILENAME)

    init {
        checkCurrentAccount()
        initAutoBackupObserver()
    }

    /**
     * Checks if user was previously signed in (Google Drive or Device Sync Storage)
     * and loads the latest backup metadata.
     */
    fun checkCurrentAccount() {
        coroutineScope.launch {
            try {
                val accountType = songDao.getPreference(PREF_ACCOUNT_TYPE)

                if (accountType == "local") {
                    _accountInfo.value = DriveAccountInfo(
                        email = "device.storage@vybe.app",
                        displayName = "Device Sync Storage",
                        photoUrl = null,
                        isLocalSync = true
                    )
                    loadLocalBackupMetadata()
                    return@launch
                }

                // Check Google Sign In
                val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
                if (lastAccount != null && lastAccount.account != null) {
                    val hasScope = GoogleSignIn.hasPermissions(lastAccount, driveScope)
                    _accountInfo.value = DriveAccountInfo(
                        email = lastAccount.email.orEmpty(),
                        displayName = lastAccount.displayName ?: lastAccount.email.orEmpty(),
                        photoUrl = lastAccount.photoUrl?.toString(),
                        isLocalSync = false
                    )
                    refreshBackupMetadata(lastAccount)
                } else {
                    // Check if saved Google account in DB
                    val savedEmail = songDao.getPreference(PREF_ACCOUNT_EMAIL)
                    val savedName = songDao.getPreference(PREF_ACCOUNT_NAME)
                    if (!savedEmail.isNullOrBlank()) {
                        _accountInfo.value = DriveAccountInfo(
                            email = savedEmail,
                            displayName = savedName ?: savedEmail,
                            photoUrl = songDao.getPreference(PREF_ACCOUNT_PHOTO),
                            isLocalSync = false
                        )
                        loadLocalBackupMetadata()
                    } else {
                        // Check if we have an existing local backup file
                        loadLocalBackupMetadata()
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error checking current account", e)
            }
        }
    }

    /**
     * Connects to Device Sync Storage mode, enabling instant offline backup & restore.
     */
    fun connectLocalSyncStorage() {
        coroutineScope.launch {
            songDao.setPreference(AppPreferenceEntity(key = PREF_ACCOUNT_TYPE, value = "local"))
            songDao.setPreference(AppPreferenceEntity(key = PREF_ACCOUNT_EMAIL, value = "device.storage@vybe.app"))
            songDao.setPreference(AppPreferenceEntity(key = PREF_ACCOUNT_NAME, value = "Device Sync Storage"))

            _accountInfo.value = DriveAccountInfo(
                email = "device.storage@vybe.app",
                displayName = "Device Sync Storage",
                photoUrl = null,
                isLocalSync = true
            )
            loadLocalBackupMetadata()
            _syncStatus.value = SyncStatus.Success("Connected to Device Sync Storage!")
        }
    }

    /**
     * Returns the intent to launch for Google OAuth sign-in.
     */
    fun getSignInIntent(): Intent {
        _syncStatus.value = SyncStatus.Connecting
        return googleSignInClient.signInIntent
    }

    /**
     * Processes the result returned from the Google Sign-In activity launcher.
     */
    fun handleSignInResult(data: Intent?) {
        if (data == null) {
            _syncStatus.value = SyncStatus.Idle
            return
        }

        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val account = task.getResult(ApiException::class.java)
            if (account != null && account.account != null) {
                _accountInfo.value = DriveAccountInfo(
                    email = account.email.orEmpty(),
                    displayName = account.displayName ?: account.email.orEmpty(),
                    photoUrl = account.photoUrl?.toString(),
                    isLocalSync = false
                )

                coroutineScope.launch(Dispatchers.IO) {
                    songDao.setPreference(AppPreferenceEntity(key = PREF_ACCOUNT_TYPE, value = "google"))
                    songDao.setPreference(AppPreferenceEntity(key = PREF_ACCOUNT_EMAIL, value = account.email.orEmpty()))
                    songDao.setPreference(AppPreferenceEntity(key = PREF_ACCOUNT_NAME, value = account.displayName ?: account.email.orEmpty()))
                    account.photoUrl?.let {
                        songDao.setPreference(AppPreferenceEntity(key = PREF_ACCOUNT_PHOTO, value = it.toString()))
                    }
                }

                _syncStatus.value = SyncStatus.Success("Connected to Google Drive successfully!")
                refreshBackupMetadata(account)
            } else {
                _syncStatus.value = SyncStatus.Error(
                    message = "Google Sign-In returned empty account. You can use Device Sync Storage for private offline backups.",
                    canFallbackToLocal = true
                )
            }
        } catch (e: ApiException) {
            if (e.statusCode == 12501 || e.statusCode == CommonStatusCodes.CANCELED) {
                Log.d(TAG, "Google sign-in was dismissed or cancelled by user.")
                _syncStatus.value = SyncStatus.Idle
                return
            }
            Log.e(TAG, "Google sign-in failed with code ${e.statusCode}", e)
            val friendlyMsg = when (e.statusCode) {
                CommonStatusCodes.DEVELOPER_ERROR -> "Google Play Services OAuth configuration is required for cloud sync (Code 10). You can use Device Sync Storage for instant private backups!"
                12500 -> "Google Sign-In failed (Code 12500). You can use Device Sync Storage for instant private backups!"
                CommonStatusCodes.SIGN_IN_REQUIRED -> "Google Sign-In required."
                CommonStatusCodes.NETWORK_ERROR -> "Network error during sign-in. Check your connection or use Device Sync Storage."
                CommonStatusCodes.INVALID_ACCOUNT -> "Invalid Google account selected."
                else -> "Google Sign-In error (${e.statusCode}). You can use Device Sync Storage for instant private backups!"
            }
            _syncStatus.value = SyncStatus.Error(friendlyMsg, isAuthError = true, canFallbackToLocal = true)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error handling sign-in result", e)
            _syncStatus.value = SyncStatus.Error("Sign-in issue: ${e.localizedMessage}. You can use Device Sync Storage!", canFallbackToLocal = true)
        }
    }

    /**
     * Handles result when returning from UserRecoverableAuthException intent.
     */
    fun onAuthRecoveryResult(granted: Boolean) {
        _userRecoverableAuthIntent.value = null
        if (granted) {
            _syncStatus.value = SyncStatus.Idle
            when (pendingAction) {
                PendingAction.BACKUP -> backupNow()
                PendingAction.RESTORE -> restoreData()
                PendingAction.NONE -> refreshBackupMetadata()
            }
        } else {
            _syncStatus.value = SyncStatus.Error("Google Drive permission was not granted.")
        }
        pendingAction = PendingAction.NONE
    }

    /**
     * Disconnects the current account and clears persisted credentials.
     */
    fun disconnect() {
        try {
            googleSignInClient.signOut()
        } catch (e: Exception) {
            Log.w(TAG, "Error during Google sign-out", e)
        }

        coroutineScope.launch(Dispatchers.IO) {
            songDao.setPreference(AppPreferenceEntity(key = PREF_ACCOUNT_TYPE, value = ""))
            songDao.setPreference(AppPreferenceEntity(key = PREF_ACCOUNT_EMAIL, value = ""))
            songDao.setPreference(AppPreferenceEntity(key = PREF_ACCOUNT_NAME, value = ""))
            songDao.setPreference(AppPreferenceEntity(key = PREF_ACCOUNT_PHOTO, value = ""))
        }

        _accountInfo.value = null
        _syncStatus.value = SyncStatus.Idle
        // Retain local backup file metadata if available
        loadLocalBackupMetadata()
    }

    /**
     * Refreshes the backup metadata from Google Drive or local sync storage.
     */
    fun refreshBackupMetadata(account: GoogleSignInAccount? = null) {
        val current = _accountInfo.value
        if (current?.isLocalSync == true) {
            loadLocalBackupMetadata()
            return
        }

        val targetAccount = account ?: GoogleSignIn.getLastSignedInAccount(context)
        if (targetAccount?.account == null) {
            loadLocalBackupMetadata()
            return
        }

        coroutineScope.launch {
            _isCheckingBackup.value = true
            try {
                val service = GoogleDriveService(context, targetAccount.account!!)
                val file = service.findExistingBackup()
                if (file != null) {
                    _lastBackupSummary.value = DriveBackupSummary(
                        fileId = file.id,
                        modifiedTime = file.modifiedTime,
                        sizeBytes = file.sizeBytes,
                        totalSongs = songDao.getAllFavoritesSync().size,
                        totalPlaylists = songDao.getAllUserPlaylistsSync().size,
                        totalHistory = songDao.getAllHistorySync().size,
                        source = "Google Drive"
                    )
                } else {
                    loadLocalBackupMetadata()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to refresh cloud backup metadata, checking local", e)
                loadLocalBackupMetadata()
            } finally {
                _isCheckingBackup.value = false
            }
        }
    }

    private fun loadLocalBackupMetadata() {
        coroutineScope.launch(Dispatchers.IO) {
            val file = localBackupFile
            if (file.exists() && file.length() > 0) {
                val savedTimestamp = songDao.getPreference(PREF_LAST_BACKUP_TIMESTAMP)?.toLongOrNull() ?: file.lastModified()
                _lastBackupSummary.value = DriveBackupSummary(
                    fileId = "local_storage",
                    modifiedTime = savedTimestamp,
                    sizeBytes = file.length(),
                    totalSongs = songDao.getAllFavoritesSync().size,
                    totalPlaylists = songDao.getAllUserPlaylistsSync().size,
                    totalHistory = songDao.getAllHistorySync().size,
                    source = if (_accountInfo.value?.isLocalSync == true) "Device Storage" else "Google Drive / Local Cache"
                )
            } else {
                val savedTimestamp = songDao.getPreference(PREF_LAST_BACKUP_TIMESTAMP)?.toLongOrNull()
                if (savedTimestamp != null) {
                    _lastBackupSummary.value = DriveBackupSummary(
                        fileId = "persisted_info",
                        modifiedTime = savedTimestamp,
                        sizeBytes = 0L,
                        totalSongs = songDao.getAllFavoritesSync().size,
                        totalPlaylists = songDao.getAllUserPlaylistsSync().size,
                        totalHistory = songDao.getAllHistorySync().size,
                        source = "Previous Backup"
                    )
                } else {
                    _lastBackupSummary.value = null
                }
            }
        }
    }

    /**
     * Performs a full backup of user data to Google Drive and/or Device Sync Storage.
     */
    fun backupNow(silent: Boolean = false) {
        val currentInfo = _accountInfo.value
        if (currentInfo == null) {
            if (!silent) {
                _syncStatus.value = SyncStatus.Error(
                    message = "Please connect to Google Drive or Device Sync Storage first.",
                    isAuthError = true,
                    canFallbackToLocal = true
                )
            }
            return
        }

        coroutineScope.launch {
            if (!silent) _syncStatus.value = SyncStatus.BackingUp("Preparing your music data for backup...")
            try {
                val backupData = withContext(Dispatchers.IO) {
                    VybeBackupData(
                        version = 1,
                        backupTimestamp = System.currentTimeMillis(),
                        favoriteSongs = songDao.getAllFavoritesSync(),
                        favoriteAlbums = songDao.getAllFavoriteAlbumsSync(),
                        userPlaylists = songDao.getAllUserPlaylistsSync(),
                        userPlaylistSongs = songDao.getAllUserPlaylistSongsSync(),
                        likedPlaylists = songDao.getAllLikedPlaylistsSync(),
                        songHistory = songDao.getAllHistorySync(),
                        searchHistory = songDao.getAllSearchHistorySync(),
                        appPreferences = songDao.getAllPreferencesSync()
                    )
                }

                if (!silent) _syncStatus.value = SyncStatus.BackingUp("Packaging (${backupData.totalItems} items)...")
                val jsonPayload = withContext(Dispatchers.Default) {
                    backupAdapter.toJson(backupData)
                }

                // 1. ALWAYS write to local sync file as a reliable, offline safeguard
                withContext(Dispatchers.IO) {
                    localBackupFile.writeText(jsonPayload)
                    songDao.setPreference(
                        AppPreferenceEntity(
                            key = PREF_LAST_BACKUP_TIMESTAMP,
                            value = System.currentTimeMillis().toString()
                        )
                    )
                    songDao.setPreference(
                        AppPreferenceEntity(
                            key = PREF_LAST_BACKUP_ITEMS,
                            value = backupData.totalItems.toString()
                        )
                    )
                }

                // 2. If Device Sync Storage, we're successfully done!
                if (currentInfo.isLocalSync) {
                    _lastBackupSummary.value = DriveBackupSummary(
                        fileId = "local_storage",
                        modifiedTime = System.currentTimeMillis(),
                        sizeBytes = localBackupFile.length(),
                        totalSongs = backupData.favoriteSongs.size,
                        totalPlaylists = backupData.userPlaylists.size,
                        totalHistory = backupData.songHistory.size,
                        source = "Device Storage"
                    )

                    if (!silent) {
                        _syncStatus.value = SyncStatus.Success(
                            "Backup successful! Saved ${backupData.favoriteSongs.size} liked songs, " +
                                    "${backupData.userPlaylists.size} playlists, and ${backupData.songHistory.size} history items to Device Sync Storage."
                        )
                    }
                    return@launch
                }

                // 3. Otherwise, upload to Google Drive
                val currentAccount = GoogleSignIn.getLastSignedInAccount(context)
                if (currentAccount?.account != null) {
                    if (!silent) _syncStatus.value = SyncStatus.BackingUp("Uploading to Google Drive private storage...")
                    val service = GoogleDriveService(context, currentAccount.account!!)
                    val resultFile = service.uploadBackupData(jsonPayload)

                    _lastBackupSummary.value = DriveBackupSummary(
                        fileId = resultFile.id,
                        modifiedTime = resultFile.modifiedTime,
                        sizeBytes = resultFile.sizeBytes,
                        totalSongs = backupData.favoriteSongs.size,
                        totalPlaylists = backupData.userPlaylists.size,
                        totalHistory = backupData.songHistory.size,
                        source = "Google Drive"
                    )

                    if (!silent) {
                        _syncStatus.value = SyncStatus.Success(
                            "Backup successful! Synced ${backupData.favoriteSongs.size} liked songs, " +
                                    "${backupData.userPlaylists.size} playlists, and ${backupData.songHistory.size} history items to Google Drive."
                        )
                    }
                } else {
                    // Google Account not attached to Play Services in this session; saved to local sync cache
                    _lastBackupSummary.value = DriveBackupSummary(
                        fileId = "local_cache",
                        modifiedTime = System.currentTimeMillis(),
                        sizeBytes = localBackupFile.length(),
                        totalSongs = backupData.favoriteSongs.size,
                        totalPlaylists = backupData.userPlaylists.size,
                        totalHistory = backupData.songHistory.size,
                        source = "Device Storage (Local)"
                    )
                    if (!silent) {
                        _syncStatus.value = SyncStatus.Success(
                            "Backup saved to Device Sync Storage! (Google Drive login session not active)."
                        )
                    }
                }
            } catch (e: UserRecoverableAuthException) {
                Log.e(TAG, "Auth permission required during backup", e)
                pendingAction = PendingAction.BACKUP
                _userRecoverableAuthIntent.value = e.intent
                _syncStatus.value = SyncStatus.Error("Google Drive permission required. Tap to authorize.", isAuthError = true)
            } catch (e: UnknownHostException) {
                Log.e(TAG, "No internet connection during backup", e)
                // Local backup was still saved!
                _syncStatus.value = SyncStatus.Success("Backup saved to Device Sync Storage (Cloud upload deferred: no internet).")
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "Timeout during backup", e)
                _syncStatus.value = SyncStatus.Success("Backup saved to Device Sync Storage (Cloud timed out).")
            } catch (e: IOException) {
                Log.e(TAG, "I/O error during backup", e)
                _syncStatus.value = SyncStatus.Success("Backup saved to Device Sync Storage (Cloud sync error: ${e.message ?: "network issue"}).")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during backup", e)
                if (!silent) {
                    _syncStatus.value = SyncStatus.Error("Backup issue: ${e.localizedMessage ?: "Unknown error"}", canFallbackToLocal = true)
                }
            }
        }
    }

    /**
     * Restores user data from Google Drive or Device Sync Storage.
     */
    fun restoreData() {
        val currentInfo = _accountInfo.value
        if (currentInfo == null) {
            _syncStatus.value = SyncStatus.Error("Please connect an account or Device Sync Storage first.", isAuthError = true, canFallbackToLocal = true)
            return
        }

        coroutineScope.launch {
            _syncStatus.value = SyncStatus.Restoring("Searching for backup file...")
            try {
                var jsonString: String? = null
                var backupSource = "Device Storage"

                // 1. If connected to Google Drive, try cloud restore first
                if (!currentInfo.isLocalSync) {
                    val currentAccount = GoogleSignIn.getLastSignedInAccount(context)
                    if (currentAccount?.account != null) {
                        try {
                            val service = GoogleDriveService(context, currentAccount.account!!)
                            val existingFile = service.findExistingBackup()
                            if (existingFile != null) {
                                _syncStatus.value = SyncStatus.Restoring("Downloading backup from Google Drive (${formatFileSize(existingFile.sizeBytes)})...")
                                jsonString = service.downloadBackupData(existingFile.id)
                                backupSource = "Google Drive"
                            }
                        } catch (e: UserRecoverableAuthException) {
                            pendingAction = PendingAction.RESTORE
                            _userRecoverableAuthIntent.value = e.intent
                            _syncStatus.value = SyncStatus.Error("Google Drive permission required. Tap to authorize.", isAuthError = true)
                            return@launch
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not fetch from Google Drive, falling back to local storage", e)
                        }
                    }
                }

                // 2. If not found in Google Drive or using Device Sync, check local storage
                if (jsonString.isNullOrBlank()) {
                    val localFile = localBackupFile
                    if (localFile.exists() && localFile.length() > 0) {
                        _syncStatus.value = SyncStatus.Restoring("Reading backup from Device Sync Storage (${formatFileSize(localFile.length())})...")
                        jsonString = withContext(Dispatchers.IO) { localFile.readText() }
                        backupSource = "Device Sync Storage"
                    }
                }

                if (jsonString.isNullOrBlank()) {
                    _syncStatus.value = SyncStatus.Error("No existing backup found in Google Drive or Device Sync Storage.")
                    return@launch
                }

                _syncStatus.value = SyncStatus.Restoring("Restoring your music data...")
                val backupData = withContext(Dispatchers.Default) {
                    backupAdapter.fromJson(jsonString)
                }

                if (backupData == null) {
                    _syncStatus.value = SyncStatus.Error("Backup file was invalid or could not be decoded.")
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    if (backupData.favoriteSongs.isNotEmpty()) {
                        songDao.insertFavoritesList(backupData.favoriteSongs)
                    }
                    if (backupData.favoriteAlbums.isNotEmpty()) {
                        songDao.insertFavoriteAlbumsList(backupData.favoriteAlbums)
                    }
                    if (backupData.userPlaylists.isNotEmpty()) {
                        songDao.insertUserPlaylistsList(backupData.userPlaylists)
                    }
                    if (backupData.userPlaylistSongs.isNotEmpty()) {
                        songDao.insertUserPlaylistSongsList(backupData.userPlaylistSongs)
                    }
                    if (backupData.likedPlaylists.isNotEmpty()) {
                        songDao.insertLikedPlaylistsList(backupData.likedPlaylists)
                    }
                    if (backupData.songHistory.isNotEmpty()) {
                        songDao.insertHistoryList(backupData.songHistory)
                    }
                    if (backupData.searchHistory.isNotEmpty()) {
                        songDao.insertSearchHistoryList(backupData.searchHistory)
                    }
                    if (backupData.appPreferences.isNotEmpty()) {
                        songDao.insertPreferencesList(backupData.appPreferences)
                    }
                }

                _lastBackupSummary.value = DriveBackupSummary(
                    fileId = "restored",
                    modifiedTime = backupData.backupTimestamp,
                    sizeBytes = jsonString.toByteArray().size.toLong(),
                    totalSongs = backupData.favoriteSongs.size,
                    totalPlaylists = backupData.userPlaylists.size,
                    totalHistory = backupData.songHistory.size,
                    source = backupSource
                )

                _syncStatus.value = SyncStatus.Success(
                    "Restored ${backupData.favoriteSongs.size} liked songs, " +
                            "${backupData.userPlaylists.size} playlists, and ${backupData.songHistory.size} history items successfully from $backupSource!"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during restore", e)
                _syncStatus.value = SyncStatus.Error("Restore issue: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    /**
     * Exports current data to an external OutputStream (e.g. user-selected file).
     */
    fun exportBackup(outputStream: OutputStream) {
        coroutineScope.launch {
            _syncStatus.value = SyncStatus.BackingUp("Exporting backup file...")
            try {
                val backupData = withContext(Dispatchers.IO) {
                    VybeBackupData(
                        version = 1,
                        backupTimestamp = System.currentTimeMillis(),
                        favoriteSongs = songDao.getAllFavoritesSync(),
                        favoriteAlbums = songDao.getAllFavoriteAlbumsSync(),
                        userPlaylists = songDao.getAllUserPlaylistsSync(),
                        userPlaylistSongs = songDao.getAllUserPlaylistSongsSync(),
                        likedPlaylists = songDao.getAllLikedPlaylistsSync(),
                        songHistory = songDao.getAllHistorySync(),
                        searchHistory = songDao.getAllSearchHistorySync(),
                        appPreferences = songDao.getAllPreferencesSync()
                    )
                }

                val jsonPayload = withContext(Dispatchers.Default) {
                    backupAdapter.toJson(backupData)
                }

                withContext(Dispatchers.IO) {
                    outputStream.use { out ->
                        out.write(jsonPayload.toByteArray(Charsets.UTF_8))
                        out.flush()
                    }
                }

                _syncStatus.value = SyncStatus.Success("Exported ${backupData.totalItems} items successfully!")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to export backup", e)
                _syncStatus.value = SyncStatus.Error("Failed to export backup: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Imports data from an external InputStream (e.g. user-selected file).
     */
    fun importBackup(inputStream: InputStream) {
        coroutineScope.launch {
            _syncStatus.value = SyncStatus.Restoring("Importing backup file...")
            try {
                val jsonString = withContext(Dispatchers.IO) {
                    inputStream.use { it.bufferedReader(Charsets.UTF_8).readText() }
                }

                val backupData = withContext(Dispatchers.Default) {
                    backupAdapter.fromJson(jsonString)
                }

                if (backupData == null) {
                    _syncStatus.value = SyncStatus.Error("Invalid backup file.")
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    if (backupData.favoriteSongs.isNotEmpty()) {
                        songDao.insertFavoritesList(backupData.favoriteSongs)
                    }
                    if (backupData.favoriteAlbums.isNotEmpty()) {
                        songDao.insertFavoriteAlbumsList(backupData.favoriteAlbums)
                    }
                    if (backupData.userPlaylists.isNotEmpty()) {
                        songDao.insertUserPlaylistsList(backupData.userPlaylists)
                    }
                    if (backupData.userPlaylistSongs.isNotEmpty()) {
                        songDao.insertUserPlaylistSongsList(backupData.userPlaylistSongs)
                    }
                    if (backupData.likedPlaylists.isNotEmpty()) {
                        songDao.insertLikedPlaylistsList(backupData.likedPlaylists)
                    }
                    if (backupData.songHistory.isNotEmpty()) {
                        songDao.insertHistoryList(backupData.songHistory)
                    }
                    if (backupData.searchHistory.isNotEmpty()) {
                        songDao.insertSearchHistoryList(backupData.searchHistory)
                    }
                    if (backupData.appPreferences.isNotEmpty()) {
                        songDao.insertPreferencesList(backupData.appPreferences)
                    }
                }

                _lastBackupSummary.value = DriveBackupSummary(
                    fileId = "imported",
                    modifiedTime = backupData.backupTimestamp,
                    sizeBytes = jsonString.toByteArray().size.toLong(),
                    totalSongs = backupData.favoriteSongs.size,
                    totalPlaylists = backupData.userPlaylists.size,
                    totalHistory = backupData.songHistory.size,
                    source = "Imported File"
                )

                _syncStatus.value = SyncStatus.Success("Imported ${backupData.totalItems} items successfully!")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to import backup", e)
                _syncStatus.value = SyncStatus.Error("Failed to import backup: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Sets up auto-backup observer that listens to changes in user library and runs
     * a background backup if pref_auto_backup is enabled.
     */
    @OptIn(FlowPreview::class)
    private fun initAutoBackupObserver() {
        coroutineScope.launch {
            combine(
                songDao.getAllFavorites(),
                songDao.getAllUserPlaylists()
            ) { favs, playlists ->
                favs.size + playlists.size
            }
                .drop(1) // Skip initial emission
                .debounce(15000L) // Debounce 15s
                .collect {
                    triggerAutoBackup()
                }
        }
    }

    /**
     * Silently triggers a backup if auto-backup preference is enabled.
     */
    fun triggerAutoBackup() {
        coroutineScope.launch {
            val autoBackupEnabled = songDao.getPreference("pref_auto_backup")?.toBooleanStrictOrNull() ?: false
            if (autoBackupEnabled && _accountInfo.value != null) {
                backupNow(silent = true)
            }
        }
    }

    fun clearStatus() {
        _syncStatus.value = SyncStatus.Idle
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
