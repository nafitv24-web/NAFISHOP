package com.example.data.firebase

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class FirebaseUserAccount(
    val email: String,
    val passwordHash: String,
    val shopName: String,
    val ownerName: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)

data class FirebaseOperationResult(
    val success: Boolean,
    val message: String,
    val data: String? = null
)

class FirebaseRealtimeManager {

    companion object {
        private const val TAG = "FirebaseRealtime"
        const val DATABASE_URL = "https://nafishop-54e99-default-rtdb.firebaseio.com"

        fun sanitizeEmail(email: String): String {
            return email.lowercase().trim()
                .replace(".", "_dot_")
                .replace("@", "_at_")
                .replace("#", "_hash_")
                .replace("$", "_dollar_")
                .replace("[", "_lb_")
                .replace("]", "_rb_")
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()

    /**
     * Test if Firebase Realtime Database is accessible
     */
    suspend fun testConnection(): FirebaseOperationResult = withContext(Dispatchers.IO) {
        try {
            val url = "$DATABASE_URL/status.json"
            val bodyObj = JSONObject().apply {
                put("online", true)
                put("appName", "NAFI SHOP 24")
                put("lastPing", System.currentTimeMillis())
            }
            val requestBody = bodyObj.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .put(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                FirebaseOperationResult(true, "Firebase ক্লাউড ডাটাবেস সফলভাবে সংযুক্ত রয়েছে")
            } else {
                FirebaseOperationResult(false, "সার্ভার কোড: ${response.code} - ${response.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase connection test failed", e)
            FirebaseOperationResult(false, "ফায়ারবেস সংযোগ ব্যর্থ: ${e.localizedMessage ?: "নেটওয়ার্ক সমস্যা"}")
        }
    }

    /**
     * Create or Register account directly in Firebase Realtime Database
     */
    suspend fun registerAccount(
        email: String,
        password: String,
        shopName: String,
        ownerName: String
    ): FirebaseOperationResult = withContext(Dispatchers.IO) {
        try {
            val sanitized = sanitizeEmail(email)
            val checkUrl = "$DATABASE_URL/users/$sanitized.json"

            // Check if user already exists
            val getRequest = Request.Builder().url(checkUrl).get().build()
            val getResp = client.newCall(getRequest).execute()
            val existingBody = getResp.body?.string()

            if (getResp.isSuccessful && !existingBody.isNullOrBlank() && existingBody != "null") {
                return@withContext FirebaseOperationResult(
                    success = false,
                    message = "এই জিমেইল (${email}) দিয়ে ইতিমধ্যে অ্যাকাউন্ট তৈরি আছে! অনুগ্রহ করে 'লগইন করুন' ট্যাবে যান।"
                )
            }

            // Create new user in Firebase Realtime DB
            val userObj = JSONObject().apply {
                put("email", email.trim())
                put("password", password) // In production can hash
                put("shopName", shopName.ifBlank { "NAFI SHOP 24" })
                put("ownerName", ownerName.ifBlank { "দোকানদার" })
                put("createdAt", System.currentTimeMillis())
                put("lastLoginAt", System.currentTimeMillis())
            }

            val putRequest = Request.Builder()
                .url(checkUrl)
                .put(userObj.toString().toRequestBody(jsonMediaType))
                .build()

            val putResp = client.newCall(putRequest).execute()
            if (putResp.isSuccessful) {
                FirebaseOperationResult(
                    success = true,
                    message = "Firebase ক্লাউডে অ্যাকাউন্ট সফলভাবে তৈরি হয়েছে!",
                    data = userObj.toString()
                )
            } else {
                FirebaseOperationResult(
                    success = false,
                    message = "Firebase রেসপন্স ত্রুটি (${putResp.code}): ${putResp.message}"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Account creation failed", e)
            FirebaseOperationResult(false, "অ্যাকাউন্ট তৈরি ব্যর্থ: ${e.localizedMessage ?: "সংযোগ সমস্যা"}")
        }
    }

    /**
     * Authenticate / Login against Firebase Realtime Database
     */
    suspend fun loginAccount(
        email: String,
        password: String
    ): FirebaseOperationResult = withContext(Dispatchers.IO) {
        try {
            val sanitized = sanitizeEmail(email)
            val url = "$DATABASE_URL/users/$sanitized.json"

            val request = Request.Builder().url(url).get().build()
            val resp = client.newCall(request).execute()
            val body = resp.body?.string()

            if (!resp.isSuccessful || body.isNullOrBlank() || body == "null") {
                return@withContext FirebaseOperationResult(
                    success = false,
                    message = "এই জিমেইল (${email}) দিয়ে কোনো অ্যাকাউন্ট পাওয়া যায়নি! প্রথমে 'নতুন অ্যাকাউন্ট তৈরি' করুন।"
                )
            }

            val userObj = JSONObject(body)
            val savedPass = userObj.optString("password", "")
            if (savedPass != password) {
                return@withContext FirebaseOperationResult(
                    success = false,
                    message = "ভুল পাসওয়ার্ড দেওয়া হয়েছে! সঠিক পাসওয়ার্ড দিয়ে চেষ্টা করুন।"
                )
            }

            // Update last login
            val patchObj = JSONObject().apply {
                put("lastLoginAt", System.currentTimeMillis())
            }
            val patchReq = Request.Builder()
                .url(url)
                .patch(patchObj.toString().toRequestBody(jsonMediaType))
                .build()
            client.newCall(patchReq).execute()

            FirebaseOperationResult(
                success = true,
                message = "Firebase লগইন সফল হয়েছে!",
                data = body
            )
        } catch (e: Exception) {
            Log.e(TAG, "Login failed", e)
            FirebaseOperationResult(false, "লগইন ব্যর্থ: ${e.localizedMessage ?: "সংযোগ সমস্যা"}")
        }
    }

    /**
     * Backup complete shop data to Firebase Realtime Database
     */
    suspend fun backupShopData(
        email: String,
        backupJsonString: String
    ): FirebaseOperationResult = withContext(Dispatchers.IO) {
        try {
            val sanitized = sanitizeEmail(email.ifBlank { "default_shop" })
            val userBackupUrl = "$DATABASE_URL/shops/$sanitized/backup.json"
            val latestBackupUrl = "$DATABASE_URL/latest_backup.json"

            val body = backupJsonString.toRequestBody(jsonMediaType)

            // Save to user path
            val req1 = Request.Builder().url(userBackupUrl).put(body).build()
            val resp1 = client.newCall(req1).execute()

            // Also update global latest
            val req2 = Request.Builder().url(latestBackupUrl).put(backupJsonString.toRequestBody(jsonMediaType)).build()
            client.newCall(req2).execute()

            if (resp1.isSuccessful) {
                FirebaseOperationResult(
                    success = true,
                    message = "Firebase Realtime ক্লাউড ডাটাবেসে সম্পূর্ণ ব্যাকআপ সফলভাবে সংরক্ষিত হয়েছে!"
                )
            } else {
                FirebaseOperationResult(
                    success = false,
                    message = "Firebase ক্লাউড আপলোড ত্রুটি (${resp1.code})"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Firebase backup failed", e)
            FirebaseOperationResult(false, "Firebase ক্লাউড ব্যাকআপ ব্যর্থ: ${e.localizedMessage ?: "সংযোগ বিচ্ছিন্ন"}")
        }
    }

    /**
     * Restore complete shop data from Firebase Realtime Database
     */
    suspend fun restoreShopData(
        email: String
    ): FirebaseOperationResult = withContext(Dispatchers.IO) {
        try {
            val sanitized = sanitizeEmail(email.ifBlank { "default_shop" })
            val userBackupUrl = "$DATABASE_URL/shops/$sanitized/backup.json"

            var req = Request.Builder().url(userBackupUrl).get().build()
            var resp = client.newCall(req).execute()
            var body = resp.body?.string()

            // Fallback to latest_backup if user specific is empty
            if (body.isNullOrBlank() || body == "null") {
                val latestUrl = "$DATABASE_URL/latest_backup.json"
                req = Request.Builder().url(latestUrl).get().build()
                resp = client.newCall(req).execute()
                body = resp.body?.string()
            }

            if (!resp.isSuccessful || body.isNullOrBlank() || body == "null") {
                return@withContext FirebaseOperationResult(
                    success = false,
                    message = "Firebase ক্লাউডে এই অ্যাকাউন্টের কোনো পূর্ববর্তী ব্যাকআপ পাওয়া যায়নি!"
                )
            }

            FirebaseOperationResult(
                success = true,
                message = "Firebase ক্লাউড থেকে ব্যাকআপ সফলভাবে পাওয়া গেছে!",
                data = body
            )
        } catch (e: Exception) {
            Log.e(TAG, "Firebase restore failed", e)
            FirebaseOperationResult(false, "Firebase থেকে রিস্টোর ব্যর্থ: ${e.localizedMessage ?: "সংযোগ সমস্যা"}")
        }
    }
}
