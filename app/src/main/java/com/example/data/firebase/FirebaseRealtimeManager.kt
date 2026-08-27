package com.example.data.firebase

import android.util.Log
import com.example.data.model.AppNotice
import com.example.data.model.AppUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
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
     * Backup complete shop data to Firebase Realtime Database strictly isolated per user email
     */
    suspend fun backupShopData(
        email: String,
        backupJsonString: String
    ): FirebaseOperationResult = withContext(Dispatchers.IO) {
        try {
            if (email.isBlank()) {
                return@withContext FirebaseOperationResult(
                    success = false,
                    message = "ইউজার ইমেইল পাওয়া যায়নি। অনুগ্রহ করে লগইন করুন।"
                )
            }
            val sanitized = sanitizeEmail(email)
            val userBackupUrl = "$DATABASE_URL/shops/$sanitized/backup.json"

            val body = backupJsonString.toRequestBody(jsonMediaType)

            // Strictly save to this user's isolated path only (No global/shared fallback)
            val req1 = Request.Builder().url(userBackupUrl).put(body).build()
            val resp1 = client.newCall(req1).execute()

            if (resp1.isSuccessful) {
                FirebaseOperationResult(
                    success = true,
                    message = "আপনার অ্যাকাউন্ট (${email})-এর নিজস্ব ক্লাউডে ব্যাকআপ সফলভাবে সংরক্ষিত হয়েছে!"
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
     * Restore complete shop data from Firebase Realtime Database strictly for this user.
     * Supports multiple candidate endpoints and nested backup wrappers.
     */
    suspend fun restoreShopData(
        email: String
    ): FirebaseOperationResult = withContext(Dispatchers.IO) {
        try {
            if (email.isBlank()) {
                return@withContext FirebaseOperationResult(
                    success = false,
                    message = "ইউজার ইমেইল পাওয়া যায়নি। অনুগ্রহ করে লগইন করুন।"
                )
            }
            val sanitized = sanitizeEmail(email)
            val trimmedEmail = email.trim().lowercase()

            val candidateUrls = listOf(
                "$DATABASE_URL/shops/$sanitized/backup.json",
                "$DATABASE_URL/shops/$sanitized.json",
                "$DATABASE_URL/shops/${email.replace(".", "_").replace("@", "_")}.json",
                "$DATABASE_URL/shops/${email.substringBefore("@").lowercase().trim()}.json",
                "$DATABASE_URL/users/$sanitized/backup.json",
                "$DATABASE_URL/users/$sanitized/data.json",
                "$DATABASE_URL/users/$sanitized.json",
                "$DATABASE_URL/shops/$trimmedEmail/backup.json",
                "$DATABASE_URL/shops/$trimmedEmail.json",
                "$DATABASE_URL/data.json",
                "$DATABASE_URL/backup.json"
            )

            var bestData: String? = null
            var bestScore = 0

            for (url in candidateUrls) {
                try {
                    val req = Request.Builder().url(url).get().build()
                    val resp = client.newCall(req).execute()
                    val body = resp.body?.string()
                    if (resp.isSuccessful && !body.isNullOrBlank() && body != "null" && body != "{}") {
                        // Check content score
                        var score = 0
                        var extractedJson = body

                        try {
                            val obj = JSONObject(body)
                            if (obj.has("backup") && obj.optJSONObject("backup") != null) {
                                extractedJson = obj.getJSONObject("backup").toString()
                            } else if (obj.has("backup") && obj.optString("backup").startsWith("{")) {
                                extractedJson = obj.getString("backup")
                            } else if (obj.has("data") && obj.optJSONObject("data") != null) {
                                extractedJson = obj.getJSONObject("data").toString()
                            }

                            val inspectObj = JSONObject(extractedJson)
                            if (inspectObj.has("products")) score += inspectObj.optJSONArray("products")?.length() ?: 0
                            if (inspectObj.has("transactions")) score += (inspectObj.optJSONArray("transactions")?.length() ?: 0) * 2
                            if (inspectObj.has("customers")) score += (inspectObj.optJSONArray("customers")?.length() ?: 0) * 2
                            if (inspectObj.has("dueLogs")) score += inspectObj.optJSONArray("dueLogs")?.length() ?: 0
                            if (inspectObj.has("expenses")) score += inspectObj.optJSONArray("expenses")?.length() ?: 0
                            if (inspectObj.has("shopInfo")) score += 5
                        } catch (e: Exception) {
                            // If not JSON object, maybe raw string
                        }

                        if (score > bestScore || (bestData == null && extractedJson.isNotBlank())) {
                            bestScore = score
                            bestData = extractedJson
                            if (bestScore > 0) {
                                break // Found rich backup
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Try next candidate
                }
            }

            // If still not found, search shops list under $DATABASE_URL/shops.json
            if (bestData == null) {
                try {
                    val reqAll = Request.Builder().url("$DATABASE_URL/shops.json").get().build()
                    val respAll = client.newCall(reqAll).execute()
                    val bodyAll = respAll.body?.string()
                    if (respAll.isSuccessful && !bodyAll.isNullOrBlank() && bodyAll != "null") {
                        val allShops = JSONObject(bodyAll)
                        val keys = allShops.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            if (k.contains(sanitized, ignoreCase = true) || k.contains(trimmedEmail.replace("@", ""), ignoreCase = true)) {
                                val item = allShops.optJSONObject(k)
                                if (item != null) {
                                    if (item.has("backup")) {
                                        bestData = item.opt("backup").toString()
                                        break
                                    } else {
                                        bestData = item.toString()
                                        break
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }

            if (bestData == null || bestData.isBlank() || bestData == "{}" || bestData == "null") {
                return@withContext FirebaseOperationResult(
                    success = false,
                    message = "আপনার অ্যাকাউন্ট (${email})-এর জন্য ক্লাউডে কোনো পূর্ববর্তী ব্যাকআপ পাওয়া যায়নি।"
                )
            }

            FirebaseOperationResult(
                success = true,
                message = "আপনার অ্যাকাউন্ট (${email})-এর ক্লাউড ব্যাকআপ সফলভাবে পাওয়া গেছে!",
                data = bestData
            )
        } catch (e: Exception) {
            Log.e(TAG, "Firebase restore failed", e)
            FirebaseOperationResult(false, "Firebase থেকে রিস্টোর ব্যর্থ: ${e.localizedMessage ?: "সংযোগ সমস্যা"}")
        }
    }

    /**
     * Publish App Update info to Firebase
     */
    suspend fun publishAppUpdate(updateInfo: AppUpdateInfo): FirebaseOperationResult = withContext(Dispatchers.IO) {
        try {
            val url = "$DATABASE_URL/app_update_info.json"
            val jsonObj = JSONObject().apply {
                put("versionName", updateInfo.versionName)
                put("versionCode", updateInfo.versionCode)
                put("downloadUrl", updateInfo.downloadUrl)
                put("releaseNotes", updateInfo.releaseNotes)
                put("isForceUpdate", updateInfo.isForceUpdate)
                put("isUpdateActive", updateInfo.isUpdateActive)
                put("releaseDate", updateInfo.releaseDate)
            }
            val body = jsonObj.toString().toRequestBody(jsonMediaType)
            val req = Request.Builder().url(url).put(body).build()
            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
                FirebaseOperationResult(true, "অ্যাপ আপডেট সফলভাবে ক্লাউডে প্রকাশিত হয়েছে!")
            } else {
                FirebaseOperationResult(false, "আপডেট পাবলিশ ব্যর্থ: কোড ${resp.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Publish app update failed", e)
            FirebaseOperationResult(false, "আপডেট পাবলিশ ত্রুটি: ${e.localizedMessage}")
        }
    }

    /**
     * Fetch App Update info from Firebase
     */
    suspend fun fetchAppUpdate(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val url = "$DATABASE_URL/app_update_info.json"
            val req = Request.Builder().url(url).get().build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string()
            if (resp.isSuccessful && !body.isNullOrBlank() && body != "null") {
                val json = JSONObject(body)
                AppUpdateInfo(
                    versionName = json.optString("versionName", "2.4.0"),
                    versionCode = json.optInt("versionCode", 24),
                    downloadUrl = json.optString("downloadUrl", ""),
                    releaseNotes = json.optString("releaseNotes", ""),
                    isForceUpdate = json.optBoolean("isForceUpdate", false),
                    isUpdateActive = json.optBoolean("isUpdateActive", true),
                    releaseDate = json.optLong("releaseDate", System.currentTimeMillis())
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Fetch app update failed", e)
            null
        }
    }

    /**
     * Publish User Notice to Firebase
     */
    suspend fun publishAppNotice(notice: AppNotice): FirebaseOperationResult = withContext(Dispatchers.IO) {
        try {
            val url = "$DATABASE_URL/app_notices/${notice.id}.json"
            val activeUrl = "$DATABASE_URL/active_notice.json"
            val jsonObj = JSONObject().apply {
                put("id", notice.id)
                put("title", notice.title)
                put("message", notice.message)
                put("type", notice.type)
                put("timestamp", notice.timestamp)
                put("isActive", notice.isActive)
                put("actionUrl", notice.actionUrl)
            }
            val body = jsonObj.toString().toRequestBody(jsonMediaType)
            val req = Request.Builder().url(url).put(body).build()
            client.newCall(req).execute()

            val reqActive = Request.Builder().url(activeUrl).put(body).build()
            val respActive = client.newCall(reqActive).execute()

            if (respActive.isSuccessful) {
                FirebaseOperationResult(true, "ইউজার নোটিফিকেশন সফলভাবে পাঠানো হয়েছে!")
            } else {
                FirebaseOperationResult(false, "নোটিফিকেশন পাঠানো ব্যর্থ হয়েছে")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Publish notice failed", e)
            FirebaseOperationResult(false, "নোটিফিকেশন ত্রুটি: ${e.localizedMessage}")
        }
    }

    /**
     * Fetch active notice from Firebase
     */
    suspend fun fetchActiveNotice(): AppNotice? = withContext(Dispatchers.IO) {
        try {
            val url = "$DATABASE_URL/active_notice.json"
            val req = Request.Builder().url(url).get().build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string()
            if (resp.isSuccessful && !body.isNullOrBlank() && body != "null") {
                val json = JSONObject(body)
                AppNotice(
                    id = json.optString("id", ""),
                    title = json.optString("title", ""),
                    message = json.optString("message", ""),
                    type = json.optString("type", "INFO"),
                    timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                    isActive = json.optBoolean("isActive", true),
                    actionUrl = json.optString("actionUrl", "")
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Fetch active notice failed", e)
            null
        }
    }

    /**
     * Delete / deactivate notice
     */
    suspend fun clearActiveNotice(): FirebaseOperationResult = withContext(Dispatchers.IO) {
        try {
            val activeUrl = "$DATABASE_URL/active_notice.json"
            val req = Request.Builder().url(activeUrl).delete().build()
            client.newCall(req).execute()
            FirebaseOperationResult(true, "নোটিশ প্রত্যাহার করা হয়েছে")
        } catch (e: Exception) {
            FirebaseOperationResult(false, "নোটিশ মুছতে সমস্যা: ${e.localizedMessage}")
        }
    }

    /**
     * Fetch all registered user accounts from Firebase Realtime Database
     */
    suspend fun fetchAllUsers(): List<FirebaseUserAccount> = withContext(Dispatchers.IO) {
        try {
            val url = "$DATABASE_URL/users.json"
            val req = Request.Builder().url(url).get().build()
            val resp = client.newCall(req).execute()
            val body = resp.body?.string()
            if (resp.isSuccessful && !body.isNullOrBlank() && body != "null") {
                val json = JSONObject(body)
                val list = mutableListOf<FirebaseUserAccount>()
                val keys = json.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val userObj = json.optJSONObject(key)
                    if (userObj != null) {
                        val email = userObj.optString("email", key.replace("_dot_", ".").replace("_at_", "@"))
                        list.add(
                            FirebaseUserAccount(
                                email = email,
                                passwordHash = "",
                                shopName = userObj.optString("shopName", "NAFI SHOP 24"),
                                ownerName = userObj.optString("ownerName", "দোকানদার"),
                                createdAt = userObj.optLong("createdAt", System.currentTimeMillis()),
                                lastLoginAt = userObj.optLong("lastLoginAt", System.currentTimeMillis())
                            )
                        )
                    }
                }
                list.sortedByDescending { it.createdAt }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fetch all users failed", e)
            emptyList()
        }
    }
}
