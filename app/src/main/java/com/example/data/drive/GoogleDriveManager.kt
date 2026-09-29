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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

private const val TAG = "GoogleDriveManager"
private const val DRIVE_APP_DATA_SCOPE_STR = "https://www.googleapis.com/auth/drive.appdata"
private const val PREF_LAST_BACKUP_TIMESTAMP = "drive_last_backup_timestamp"
private const val PREF_LAST_BACKUP_ITEMS = "drive_last_backup_items"

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

    init {
        checkCurrentAccount()
    }

    /**
     * Checks if user was previously signed in and has authorized Drive appdata scope.
     */
    fun checkCurrentAccount() {
        val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (lastAccount != null && lastAccount.account != null) {
            val hasScope = GoogleSignIn.hasPermissions(lastAccount, driveScope)
            if (hasScope) {
                _accountInfo.value = DriveAccountInfo(
                    email = lastAccount.email.orEmpty(),
                    displayName = lastAccount.displayName ?: lastAccount.email.orEmpty(),
                    photoUrl = lastAccount.photoUrl?.toString()
                )
                refreshBackupMetadata(lastAccount)
            } else {
                _accountInfo.value = null
            }
        } else {
            _accountInfo.value = null
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
                val hasScope = GoogleSignIn.hasPermissions(account, driveScope)
                if (hasScope) {
                    _accountInfo.value = DriveAccountInfo(
                        email = account.email.orEmpty(),
                        displayName = account.displayName ?: account.email.orEmpty(),
                        photoUrl = account.photoUrl?.toString()
                    )
                    _syncStatus.value = SyncStatus.Success("Connected to Google Drive successfully!")
                    refreshBackupMetadata(account)
                } else {
                    _syncStatus.value = SyncStatus.Error(
                        "Google Drive permission not granted. Please allow Drive app storage to sync.",
                        isAuthError = true
                    )
                }
            } else {
                _syncStatus.value = SyncStatus.Error("Unable to retrieve Google account details.")
            }
        } catch (e: ApiException) {
            if (e.statusCode == 12501 || e.statusCode == CommonStatusCodes.CANCELED) {
                Log.d(TAG, "Google sign-in was dismissed or cancelled by user.")
                _syncStatus.value = SyncStatus.Idle
                return
            }
            Log.e(TAG, "Google sign-in failed with code ${e.statusCode}", e)
            val friendlyMsg = when (e.statusCode) {
                CommonStatusCodes.SIGN_IN_REQUIRED -> "Google Sign-In required."
                CommonStatusCodes.NETWORK_ERROR -> "Network error during sign-in. Check your connection."
                CommonStatusCodes.INVALID_ACCOUNT -> "Invalid Google account selected."
                else -> "Google Sign-In error (${e.statusCode}). Please try again."
            }
            _syncStatus.value = SyncStatus.Error(friendlyMsg, isAuthError = true)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error handling sign-in result", e)
            _syncStatus.value = SyncStatus.Error("Unexpected sign-in error: ${e.localizedMessage}")
        }
    }

    /**
     * Disconnects the current Google Account and signs out from GoogleSignInClient.
     */
    fun disconnect() {
        googleSignInClient.signOut().addOnCompleteListener {
            _accountInfo.value = null
            _lastBackupSummary.value = null
            _syncStatus.value = SyncStatus.Idle
        }
    }

    /**
     * Refreshes the backup metadata from Google Drive appDataFolder.
     */
    fun refreshBackupMetadata(account: GoogleSignInAccount? = null) {
        val targetAccount = account ?: GoogleSignIn.getLastSignedInAccount(context) ?: return
        if (targetAccount.account == null) return

        coroutineScope.launch {
            _isCheckingBackup.value = true
            try {
                val service = GoogleDriveService(context, targetAccount.account!!)
                val file = service.findExistingBackup()
                if (file != null) {
                    val savedItemsCount = songDao.getPreference(PREF_LAST_BACKUP_ITEMS)?.toIntOrNull() ?: 0
                    _lastBackupSummary.value = DriveBackupSummary(
                        fileId = file.id,
                        modifiedTime = file.modifiedTime,
                        sizeBytes = file.sizeBytes,
                        totalSongs = songDao.getAllFavoritesSync().size,
                        totalPlaylists = songDao.getAllUserPlaylistsSync().size,
                        totalHistory = songDao.getAllHistorySync().size
                    )
                } else {
                    _lastBackupSummary.value = null
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to refresh backup metadata", e)
            } finally {
                _isCheckingBackup.value = false
            }
        }
    }

    /**
     * Performs a full backup of user data to Google Drive app-specific folder.
     */
    fun backupNow() {
        val currentAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (currentAccount == null || currentAccount.account == null) {
            _syncStatus.value = SyncStatus.Error("Please connect your Google Drive account first.", isAuthError = true)
            return
        }

        coroutineScope.launch {
            _syncStatus.value = SyncStatus.BackingUp("Preparing your music data for backup...")
            try {
                val backupData = withContext(Dispatchers.IO) {
                    val favorites = songDao.getAllFavoritesSync()
                    val albums = songDao.getAllFavoriteAlbumsSync()
                    val userPlaylists = songDao.getAllUserPlaylistsSync()
                    val playlistSongs = songDao.getAllUserPlaylistSongsSync()
                    val likedPlaylists = songDao.getAllLikedPlaylistsSync()
                    val history = songDao.getAllHistorySync()
                    val searchHistory = songDao.getAllSearchHistorySync()
                    val preferences = songDao.getAllPreferencesSync()

                    VybeBackupData(
                        version = 1,
                        backupTimestamp = System.currentTimeMillis(),
                        favoriteSongs = favorites,
                        favoriteAlbums = albums,
                        userPlaylists = userPlaylists,
                        userPlaylistSongs = playlistSongs,
                        likedPlaylists = likedPlaylists,
                        songHistory = history,
                        searchHistory = searchHistory,
                        appPreferences = preferences
                    )
                }

                _syncStatus.value = SyncStatus.BackingUp("Encrypting & packaging (${backupData.totalItems} items)...")
                val jsonPayload = withContext(Dispatchers.Default) {
                    backupAdapter.toJson(backupData)
                }

                _syncStatus.value = SyncStatus.BackingUp("Uploading to Google Drive private storage...")
                val service = GoogleDriveService(context, currentAccount.account!!)
                val resultFile = service.uploadBackupData(jsonPayload)

                withContext(Dispatchers.IO) {
                    songDao.setPreference(
                        AppPreferenceEntity(
                            key = PREF_LAST_BACKUP_TIMESTAMP,
                            value = resultFile.modifiedTime.toString()
                        )
                    )
                    songDao.setPreference(
                        AppPreferenceEntity(
                            key = PREF_LAST_BACKUP_ITEMS,
                            value = backupData.totalItems.toString()
                        )
                    )
                }

                _lastBackupSummary.value = DriveBackupSummary(
                    fileId = resultFile.id,
                    modifiedTime = resultFile.modifiedTime,
                    sizeBytes = resultFile.sizeBytes,
                    totalSongs = backupData.favoriteSongs.size,
                    totalPlaylists = backupData.userPlaylists.size,
                    totalHistory = backupData.songHistory.size
                )

                _syncStatus.value = SyncStatus.Success(
                    "Backup successful! Saved ${backupData.favoriteSongs.size} liked songs, " +
                            "${backupData.userPlaylists.size} playlists, and ${backupData.songHistory.size} history records."
                )
            } catch (e: UserRecoverableAuthException) {
                Log.e(TAG, "Auth permission required during backup", e)
                _syncStatus.value = SyncStatus.Error("Google Drive permission required. Please reconnect.", isAuthError = true)
            } catch (e: UnknownHostException) {
                Log.e(TAG, "No internet connection during backup", e)
                _syncStatus.value = SyncStatus.Error("No internet connection. Please verify your network and retry.")
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "Timeout during backup", e)
                _syncStatus.value = SyncStatus.Error("Connection timed out. Please try again.")
            } catch (e: IOException) {
                Log.e(TAG, "I/O error during backup", e)
                _syncStatus.value = SyncStatus.Error("Backup failed: ${e.localizedMessage ?: "Network or Drive error"}")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during backup", e)
                _syncStatus.value = SyncStatus.Error("Unexpected backup error: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    /**
     * Restores user data from the Google Drive app-specific folder.
     */
    fun restoreData() {
        val currentAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (currentAccount == null || currentAccount.account == null) {
            _syncStatus.value = SyncStatus.Error("Please connect your Google Drive account first.", isAuthError = true)
            return
        }

        coroutineScope.launch {
            _syncStatus.value = SyncStatus.Restoring("Searching for backup in Google Drive...")
            try {
                val service = GoogleDriveService(context, currentAccount.account!!)
                val existingFile = service.findExistingBackup()

                if (existingFile == null) {
                    _syncStatus.value = SyncStatus.Error("No existing VYBE backup found in your Google Drive.")
                    return@launch
                }

                _syncStatus.value = SyncStatus.Restoring("Downloading backup file (${formatFileSize(existingFile.sizeBytes)})...")
                val jsonString = service.downloadBackupData(existingFile.id)

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
                    fileId = existingFile.id,
                    modifiedTime = existingFile.modifiedTime,
                    sizeBytes = existingFile.sizeBytes,
                    totalSongs = backupData.favoriteSongs.size,
                    totalPlaylists = backupData.userPlaylists.size,
                    totalHistory = backupData.songHistory.size
                )

                _syncStatus.value = SyncStatus.Success(
                    "Restored ${backupData.favoriteSongs.size} liked songs, " +
                            "${backupData.userPlaylists.size} playlists, and ${backupData.songHistory.size} history items successfully!"
                )
            } catch (e: UserRecoverableAuthException) {
                Log.e(TAG, "Auth permission required during restore", e)
                _syncStatus.value = SyncStatus.Error("Google Drive permission required. Please reconnect.", isAuthError = true)
            } catch (e: UnknownHostException) {
                Log.e(TAG, "No internet connection during restore", e)
                _syncStatus.value = SyncStatus.Error("No internet connection. Please verify your network and retry.")
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "Timeout during restore", e)
                _syncStatus.value = SyncStatus.Error("Connection timed out. Please try again.")
            } catch (e: IOException) {
                Log.e(TAG, "I/O error during restore", e)
                _syncStatus.value = SyncStatus.Error("Restore failed: ${e.localizedMessage ?: "Network or Drive error"}")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during restore", e)
                _syncStatus.value = SyncStatus.Error("Unexpected restore error: ${e.localizedMessage ?: "Unknown error"}")
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
