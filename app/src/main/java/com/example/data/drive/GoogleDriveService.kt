package com.example.data.drive

import android.accounts.Account
import android.content.Context
import android.util.Log
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

private const val TAG = "GoogleDriveService"
private const val DRIVE_APP_DATA_SCOPE = "oauth2:https://www.googleapis.com/auth/drive.appdata"
private const val BACKUP_FILE_NAME = "vybe_backup.json"

data class DriveFileInfo(
    val id: String,
    val name: String,
    val modifiedTime: Long,
    val sizeBytes: Long
)

class GoogleDriveService(
    private val context: Context,
    private val account: Account
) {
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var cachedToken: String? = null

    /**
     * Obtains an OAuth2 bearer access token for Google Drive appDataFolder access.
     */
    suspend fun getAccessToken(forceRefresh: Boolean = false): String = withContext(Dispatchers.IO) {
        if (forceRefresh && cachedToken != null) {
            try {
                GoogleAuthUtil.clearToken(context, cachedToken!!)
            } catch (e: Exception) {
                Log.w(TAG, "Error clearing cached token", e)
            }
            cachedToken = null
        }

        cachedToken?.let { return@withContext it }

        try {
            val token = GoogleAuthUtil.getToken(context, account, DRIVE_APP_DATA_SCOPE)
            cachedToken = token
            token
        } catch (e: UserRecoverableAuthException) {
            Log.e(TAG, "User recoverable auth exception during getToken", e)
            throw e
        } catch (e: GoogleAuthException) {
            Log.e(TAG, "Google auth exception during getToken", e)
            throw e
        }
    }

    /**
     * Searches for an existing vybe_backup.json file inside the app-specific folder (appDataFolder).
     */
    suspend fun findExistingBackup(): DriveFileInfo? = executeWithRetry { token ->
        val url = "https://www.googleapis.com/drive/v3/files" +
                "?spaces=appDataFolder" +
                "&q=name%3D'$BACKUP_FILE_NAME'+and+trashed%3Dfalse" +
                "&fields=files(id,name,modifiedTime,size)" +
                "&pageSize=1"

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .header("Accept", "application/json")
            .get()
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty()
                Log.e(TAG, "Failed to query appDataFolder files: ${response.code} $errorBody")
                if (response.code == 401) throw UnauthorizedException()
                throw IOException("Google Drive query failed with HTTP ${response.code}")
            }

            val body = response.body?.string().orEmpty()
            val json = JSONObject(body)
            val filesArray = json.optJSONArray("files")

            if (filesArray != null && filesArray.length() > 0) {
                val fileObj = filesArray.getJSONObject(0)
                val id = fileObj.getString("id")
                val name = fileObj.optString("name", BACKUP_FILE_NAME)
                val modifiedStr = fileObj.optString("modifiedTime")
                val sizeBytes = fileObj.optLong("size", 0L)
                val modifiedTime = parseIsoTimestamp(modifiedStr)

                DriveFileInfo(
                    id = id,
                    name = name,
                    modifiedTime = modifiedTime,
                    sizeBytes = sizeBytes
                )
            } else {
                null
            }
        }
    }

    /**
     * Uploads the backup JSON data to the appDataFolder.
     * Updates in place if file already exists, or creates a new file.
     */
    suspend fun uploadBackupData(jsonData: String): DriveFileInfo {
        val existing = findExistingBackup()
        return if (existing != null) {
            updateExistingFile(existing.id, jsonData)
        } else {
            createNewFile(jsonData)
        }
    }

    private suspend fun createNewFile(jsonData: String): DriveFileInfo = executeWithRetry { token ->
        val boundary = "vybe_backup_boundary_${System.currentTimeMillis()}"
        val metadataJson = JSONObject().apply {
            put("name", BACKUP_FILE_NAME)
            put("parents", org.json.JSONArray().apply { put("appDataFolder") })
            put("description", "VYBE Music Player portable data backup")
        }.toString()

        val multipartBody = buildString {
            append("--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(metadataJson)
            append("\r\n--$boundary\r\n")
            append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            append(jsonData)
            append("\r\n--$boundary--\r\n")
        }

        val requestBody = multipartBody.toRequestBody("multipart/related; boundary=$boundary".toMediaType())

        val request = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name,modifiedTime,size")
            .header("Authorization", "Bearer $token")
            .post(requestBody)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty()
                Log.e(TAG, "Failed to create backup file: ${response.code} $errorBody")
                if (response.code == 401) throw UnauthorizedException()
                throw IOException("Google Drive upload failed with HTTP ${response.code}")
            }

            val body = response.body?.string().orEmpty()
            val fileObj = JSONObject(body)
            DriveFileInfo(
                id = fileObj.getString("id"),
                name = fileObj.optString("name", BACKUP_FILE_NAME),
                modifiedTime = parseIsoTimestamp(fileObj.optString("modifiedTime")),
                sizeBytes = fileObj.optLong("size", jsonData.toByteArray().size.toLong())
            )
        }
    }

    private suspend fun updateExistingFile(fileId: String, jsonData: String): DriveFileInfo = executeWithRetry { token ->
        val requestBody = jsonData.toRequestBody("application/json; charset=UTF-8".toMediaType())

        val request = Request.Builder()
            .url("https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=media&fields=id,name,modifiedTime,size")
            .header("Authorization", "Bearer $token")
            .patch(requestBody)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty()
                Log.e(TAG, "Failed to update backup file: ${response.code} $errorBody")
                if (response.code == 401) throw UnauthorizedException()
                throw IOException("Google Drive update failed with HTTP ${response.code}")
            }

            val body = response.body?.string().orEmpty()
            val fileObj = JSONObject(body)
            DriveFileInfo(
                id = fileObj.optString("id", fileId),
                name = fileObj.optString("name", BACKUP_FILE_NAME),
                modifiedTime = parseIsoTimestamp(fileObj.optString("modifiedTime")),
                sizeBytes = fileObj.optLong("size", jsonData.toByteArray().size.toLong())
            )
        }
    }

    /**
     * Downloads the backup payload string from Google Drive appDataFolder.
     */
    suspend fun downloadBackupData(fileId: String): String = executeWithRetry { token ->
        val request = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files/$fileId?alt=media")
            .header("Authorization", "Bearer $token")
            .get()
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string().orEmpty()
                Log.e(TAG, "Failed to download backup file: ${response.code} $errorBody")
                if (response.code == 401) throw UnauthorizedException()
                throw IOException("Google Drive download failed with HTTP ${response.code}")
            }

            response.body?.string() ?: throw IOException("Empty response body from Google Drive")
        }
    }

    /**
     * Deletes the backup file from Google Drive appDataFolder if needed.
     */
    suspend fun deleteBackup(fileId: String): Boolean = executeWithRetry { token ->
        val request = Request.Builder()
            .url("https://www.googleapis.com/drive/v3/files/$fileId")
            .header("Authorization", "Bearer $token")
            .delete()
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            response.isSuccessful || response.code == 404
        }
    }

    /**
     * Executes the network call with automatic token refresh on HTTP 401 Unauthorized.
     */
    private suspend fun <T> executeWithRetry(block: suspend (token: String) -> T): T = withContext(Dispatchers.IO) {
        val initialToken = getAccessToken(forceRefresh = false)
        try {
            block(initialToken)
        } catch (e: UnauthorizedException) {
            Log.w(TAG, "Token expired (401). Refreshing token and retrying operation...")
            val freshToken = getAccessToken(forceRefresh = true)
            block(freshToken)
        }
    }

    private fun parseIsoTimestamp(isoString: String?): Long {
        if (isoString.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            format.parse(isoString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            try {
                val formatSec = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                formatSec.parse(isoString)?.time ?: System.currentTimeMillis()
            } catch (e2: Exception) {
                System.currentTimeMillis()
            }
        }
    }

    private class UnauthorizedException : IOException("401 Unauthorized")
}
