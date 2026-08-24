package com.example.data.remote

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Real Google Drive REST API v3 Client
 * Handles real Multipart HTTP file uploads and downloads using Google Drive REST API.
 */
class GoogleDriveApiClient(private val context: Context) {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "GoogleDriveApiClient"
        private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
        private const val DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files"
        const val BACKUP_FILENAME_PREFIX = "NAFI_SHOP_24_Backup_"
    }

    /**
     * Uploads shop backup JSON to user's Google Drive via real REST API Multipart POST request.
     */
    suspend fun uploadBackupToDrive(
        accessToken: String?,
        userEmail: String,
        jsonData: String
    ): Result<DriveUploadResponse> = withContext(Dispatchers.IO) {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "${BACKUP_FILENAME_PREFIX}${timestamp}.json"

        if (accessToken.isNullOrBlank()) {
            Log.w(TAG, "No OAuth access token provided, proceeding with offline & Drive sync cache.")
            return@withContext Result.failure(
                Exception("Google OAuth token is missing. Please sign in to authorize Google Drive upload.")
            )
        }

        try {
            // Google Drive v3 Multipart upload format
            val metadataJson = JSONObject().apply {
                put("name", fileName)
                put("description", "NAFI SHOP 24 Database Backup for $userEmail")
                put("mimeType", "application/json")
                // Store in appDataFolder or root
                put("properties", JSONObject().apply {
                    put("app", "NAFI_SHOP_24")
                    put("user", userEmail)
                })
            }.toString()

            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "metadata",
                    null,
                    metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType())
                )
                .addFormDataPart(
                    "file",
                    fileName,
                    jsonData.toRequestBody("application/json; charset=UTF-8".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url(DRIVE_UPLOAD_URL)
                .addHeader("Authorization", "Bearer $accessToken")
                .post(multipartBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val jsonObject = JSONObject(responseBody)
                val fileId = jsonObject.optString("id", "")
                val name = jsonObject.optString("name", fileName)
                Log.i(TAG, "Successfully uploaded to Google Drive! File ID: $fileId")
                Result.success(DriveUploadResponse(fileId = fileId, fileName = name, timestamp = timestamp))
            } else {
                Log.e(TAG, "Drive upload failed with HTTP code ${response.code}: $responseBody")
                Result.failure(Exception("Google Drive API Error (${response.code}): $responseBody"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error during Google Drive upload", e)
            Result.failure(e)
        }
    }

    /**
     * Finds and downloads the latest backup file from Google Drive via REST API.
     */
    suspend fun downloadLatestBackupFromDrive(
        accessToken: String?,
        userEmail: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (accessToken.isNullOrBlank()) {
            return@withContext Result.failure(
                Exception("Google OAuth token is missing. Please sign in to fetch Google Drive backups.")
            )
        }

        try {
            // Query for files starting with prefix
            val query = "name contains '$BACKUP_FILENAME_PREFIX' and trashed = false"
            val listUrl = "$DRIVE_FILES_URL?q=${java.net.URLEncoder.encode(query, "UTF-8")}&orderBy=createdTime desc&pageSize=1&fields=files(id,name,createdTime)"

            val listRequest = Request.Builder()
                .url(listUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val listResponse = client.newCall(listRequest).execute()
            val listBody = listResponse.body?.string() ?: ""

            if (!listResponse.isSuccessful) {
                return@withContext Result.failure(
                    Exception("Google Drive list API Error (${listResponse.code}): $listBody")
                )
            }

            val jsonList = JSONObject(listBody)
            val filesArray = jsonList.optJSONArray("files")
            if (filesArray == null || filesArray.length() == 0) {
                return@withContext Result.failure(
                    Exception("Google Drive-এ কোনো ব্যাকআপ ফাইল পাওয়া যায়নি।")
                )
            }

            val fileId = filesArray.getJSONObject(0).getString("id")

            // Download file content (alt=media)
            val downloadUrl = "$DRIVE_FILES_URL/$fileId?alt=media"
            val downloadRequest = Request.Builder()
                .url(downloadUrl)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            val downloadResponse = client.newCall(downloadRequest).execute()
            val content = downloadResponse.body?.string() ?: ""

            if (downloadResponse.isSuccessful && content.isNotBlank()) {
                Result.success(content)
            } else {
                Result.failure(
                    Exception("Google Drive download error (${downloadResponse.code})")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error during Google Drive download", e)
            Result.failure(e)
        }
    }
}

data class DriveUploadResponse(
    val fileId: String,
    val fileName: String,
    val timestamp: String
)
