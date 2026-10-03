package com.example.data.firebase

import android.util.Log
import com.example.data.model.*
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
    val passwordHash: String = "",
    val shopName: String = "NAFI KHATA",
    val ownerName: String = "দোকানদার",
    val phone: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis(),
    val lastBackupAt: Long = 0L,
    val productCount: Int = 0,
    val customerCount: Int = 0,
    val transactionCount: Int = 0,
    val totalDue: Double = 0.0,
    val mainBalance: Double = 0.0,
    val totalSales: Double = 0.0
)

data class AdminUserShopData(
    val email: String,
    val shopName: String,
    val ownerName: String,
    val phone: String,
    val address: String,
    val currency: String,
    val mainBalance: Double,
    val lastBackupTime: Long,
    val products: List<Product>,
    val customers: List<Customer>,
    val transactions: List<TransactionRecord>,
    val expenses: List<Expense>,
    val dueLogs: List<DueLog>
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
                put("appName", "NAFI KHATA")
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
                put("shopName", shopName.ifBlank { "NAFI KHATA" })
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
     * Check if an attempted email or password matches an existing account
     * (e.g. if the user made a typo like 'nfiptv24' instead of 'nafitv24' with matching password).
     */
    suspend fun findSimilarAccountOrPasswordMatch(
        attemptedEmail: String,
        attemptedPass: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val url = "$DATABASE_URL/users.json"
            val request = Request.Builder().url(url).get().build()
            val resp = client.newCall(request).execute()
            val body = resp.body?.string() ?: return@withContext null
            if (!resp.isSuccessful || body.isBlank() || body == "null") return@withContext null

            val rootObj = JSONObject(body)
            val keys = rootObj.keys()
            var passMatchEmail: String? = null
            var closeEmailMatch: String? = null

            val cleanAttempt = attemptedEmail.lowercase().trim()
            val attemptUserPart = cleanAttempt.substringBefore("@")

            while (keys.hasNext()) {
                val key = keys.next()
                val userObj = rootObj.optJSONObject(key) ?: continue
                val uEmail = userObj.optString("email", "").lowercase().trim()
                val uPass = userObj.optString("password", "")

                if (uEmail.isBlank()) continue

                // Check exact password match
                if (uPass.isNotBlank() && uPass == attemptedPass) {
                    passMatchEmail = uEmail
                }

                // Check close email match (similarity or typo in user part)
                val targetUserPart = uEmail.substringBefore("@")
                if (isCloseEmail(attemptUserPart, targetUserPart)) {
                    closeEmailMatch = uEmail
                }
            }

            // If password matches and email is similar or matches password, return it
            return@withContext passMatchEmail ?: closeEmailMatch
        } catch (e: Exception) {
            Log.e(TAG, "Suggestion check failed", e)
            null
        }
    }

    private fun isCloseEmail(s1: String, s2: String): Boolean {
        if (s1 == s2) return true
        if (s1.length >= 3 && s2.length >= 3) {
            if (s1.contains(s2) || s2.contains(s1)) return true
            val dist = levenshteinDistance(s1, s2)
            return dist <= 2
        }
        return false
    }

    private fun levenshteinDistance(lhs: CharSequence, rhs: CharSequence): Int {
        var prev = IntArray(rhs.length + 1) { it }
        var curr = IntArray(rhs.length + 1) { 0 }
        for (i in 0 until lhs.length) {
            curr[0] = i + 1
            for (j in 0 until rhs.length) {
                val cost = if (lhs[i] == rhs[j]) 0 else 1
                curr[j + 1] = minOf(curr[j] + 1, prev[j + 1] + 1, prev[j] + cost)
            }
            val temp = prev
            prev = curr
            curr = temp
        }
        return prev[rhs.length]
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
                // Also update user metadata summary for admin panel monitoring
                try {
                    val root = JSONObject(backupJsonString)
                    val sInfo = root.optJSONObject("shopInfo")
                    val sName = sInfo?.optString("shopName") ?: ""
                    val oName = sInfo?.optString("ownerName") ?: ""
                    val uPhone = sInfo?.optString("phone") ?: ""
                    val mBalance = root.optDouble("mainBalance", sInfo?.optDouble("mainBalance", 0.0) ?: 0.0)

                    val pArr = root.optJSONArray("products")
                    val cArr = root.optJSONArray("customers")
                    val tArr = root.optJSONArray("transactions")

                    var totalDueSum = 0.0
                    if (cArr != null) {
                        for (i in 0 until cArr.length()) {
                            totalDueSum += cArr.optJSONObject(i)?.optDouble("totalDue", 0.0) ?: 0.0
                        }
                    }

                    var totalSalesSum = 0.0
                    if (tArr != null) {
                        for (i in 0 until tArr.length()) {
                            val tx = tArr.optJSONObject(i)
                            if (tx?.optString("type") == "SALE") {
                                totalSalesSum += tx.optDouble("totalAmount", 0.0)
                            }
                        }
                    }

                    val metaUpdate = JSONObject().apply {
                        put("email", email)
                        if (sName.isNotBlank()) put("shopName", sName)
                        if (oName.isNotBlank()) put("ownerName", oName)
                        if (uPhone.isNotBlank()) put("phone", uPhone)
                        put("lastBackupAt", System.currentTimeMillis())
                        put("productCount", pArr?.length() ?: 0)
                        put("customerCount", cArr?.length() ?: 0)
                        put("transactionCount", tArr?.length() ?: 0)
                        put("totalDue", totalDueSum)
                        put("mainBalance", mBalance)
                        put("totalSales", totalSalesSum)
                    }
                    val metaReq = Request.Builder()
                        .url("$DATABASE_URL/users/$sanitized.json")
                        .patch(metaUpdate.toString().toRequestBody(jsonMediaType))
                        .build()
                    client.newCall(metaReq).execute()
                } catch (ignored: Exception) {}

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
                "$DATABASE_URL/latest_backup.json",
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
                                shopName = userObj.optString("shopName", "NAFI KHATA"),
                                ownerName = userObj.optString("ownerName", "দোকানদার"),
                                phone = userObj.optString("phone", ""),
                                createdAt = userObj.optLong("createdAt", System.currentTimeMillis()),
                                lastLoginAt = userObj.optLong("lastLoginAt", System.currentTimeMillis()),
                                lastBackupAt = userObj.optLong("lastBackupAt", 0L),
                                productCount = userObj.optInt("productCount", 0),
                                customerCount = userObj.optInt("customerCount", 0),
                                transactionCount = userObj.optInt("transactionCount", 0),
                                totalDue = userObj.optDouble("totalDue", 0.0),
                                mainBalance = userObj.optDouble("mainBalance", 0.0),
                                totalSales = userObj.optDouble("totalSales", 0.0)
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

    /**
     * Fetch user's raw full backup JSON from cloud
     */
    suspend fun fetchUserFullBackupJson(email: String): String? = withContext(Dispatchers.IO) {
        val result = restoreShopData(email)
        if (result.success && !result.data.isNullOrBlank()) {
            result.data
        } else null
    }

    /**
     * Parses raw backup JSON into strongly typed AdminUserShopData for inspection and PDF export
     */
    fun parseUserShopData(email: String, jsonStr: String): AdminUserShopData? {
        return try {
            val root = JSONObject(jsonStr)
            val shopObj = root.optJSONObject("shopInfo")
            val shopName = shopObj?.optString("shopName")?.ifBlank { "NAFI KHATA" } ?: "NAFI KHATA"
            val ownerName = shopObj?.optString("ownerName")?.ifBlank { "দোকানদার" } ?: "দোকানদার"
            val phone = shopObj?.optString("phone") ?: ""
            val address = shopObj?.optString("address") ?: ""
            val currency = shopObj?.optString("currency")?.ifBlank { "৳" } ?: "৳"
            val mainBalance = root.optDouble("mainBalance", shopObj?.optDouble("mainBalance", 0.0) ?: 0.0)
            val exportTime = root.optLong("exportTime", System.currentTimeMillis())

            val pList = mutableListOf<Product>()
            val pArr = root.optJSONArray("products")
            if (pArr != null) {
                for (i in 0 until pArr.length()) {
                    val o = pArr.optJSONObject(i) ?: continue
                    pList.add(
                        Product(
                            id = o.optLong("id", i + 1L),
                            name = o.optString("name", "পণ্য"),
                            barcode = o.optString("barcode", ""),
                            category = o.optString("category", "সাধারণ"),
                            buyPrice = o.optDouble("buyPrice", 0.0),
                            sellPrice = o.optDouble("sellPrice", 0.0),
                            stockQuantity = o.optDouble("stockQuantity", 0.0),
                            unit = o.optString("unit", "পিস"),
                            minStockAlert = o.optDouble("minStockAlert", 5.0)
                        )
                    )
                }
            }

            val cList = mutableListOf<Customer>()
            val cArr = root.optJSONArray("customers")
            if (cArr != null) {
                for (i in 0 until cArr.length()) {
                    val o = cArr.optJSONObject(i) ?: continue
                    cList.add(
                        Customer(
                            id = o.optLong("id", i + 1L),
                            name = o.optString("name", "কাস্টমার"),
                            phone = o.optString("phone", ""),
                            address = o.optString("address", ""),
                            totalDue = o.optDouble("totalDue", 0.0),
                            totalPurchased = o.optDouble("totalPurchased", 0.0)
                        )
                    )
                }
            }

            val tList = mutableListOf<TransactionRecord>()
            val tArr = root.optJSONArray("transactions")
            if (tArr != null) {
                for (i in 0 until tArr.length()) {
                    val o = tArr.optJSONObject(i) ?: continue
                    tList.add(
                        TransactionRecord(
                            id = o.optLong("id", i + 1L),
                            type = o.optString("type", "SALE"),
                            invoiceNumber = o.optString("invoiceNumber", "INV-${i + 1}"),
                            productName = o.optString("productName", ""),
                            quantity = o.optDouble("quantity", 1.0),
                            unit = o.optString("unit", "পিস"),
                            unitPrice = o.optDouble("unitPrice", 0.0),
                            costPrice = o.optDouble("costPrice", 0.0),
                            totalAmount = o.optDouble("totalAmount", 0.0),
                            profitAmount = o.optDouble("profitAmount", 0.0),
                            customerName = o.optString("customerName", "ক্যাশ কাস্টমার"),
                            customerPhone = o.optString("customerPhone", ""),
                            paidAmount = o.optDouble("paidAmount", 0.0),
                            dueAmount = o.optDouble("dueAmount", 0.0),
                            paymentMethod = o.optString("paymentMethod", "CASH"),
                            timestamp = o.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            val eList = mutableListOf<Expense>()
            val eArr = root.optJSONArray("expenses")
            if (eArr != null) {
                for (i in 0 until eArr.length()) {
                    val o = eArr.optJSONObject(i) ?: continue
                    eList.add(
                        Expense(
                            id = o.optLong("id", i + 1L),
                            title = o.optString("title", "খরচ"),
                            category = o.optString("category", "অন্যান্য"),
                            amount = o.optDouble("amount", 0.0),
                            note = o.optString("note", ""),
                            timestamp = o.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            val dList = mutableListOf<DueLog>()
            val dArr = root.optJSONArray("dueLogs")
            if (dArr != null) {
                for (i in 0 until dArr.length()) {
                    val o = dArr.optJSONObject(i) ?: continue
                    dList.add(
                        DueLog(
                            id = o.optLong("id", i + 1L),
                            customerId = o.optLong("customerId", 0L),
                            customerName = o.optString("customerName", ""),
                            customerPhone = o.optString("customerPhone", ""),
                            type = o.optString("type", "DUE_GIVEN"),
                            amount = o.optDouble("amount", 0.0),
                            note = o.optString("note", ""),
                            timestamp = o.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            AdminUserShopData(
                email = email,
                shopName = shopName,
                ownerName = ownerName,
                phone = phone,
                address = address,
                currency = currency,
                mainBalance = mainBalance,
                lastBackupTime = exportTime,
                products = pList,
                customers = cList,
                transactions = tList,
                expenses = eList,
                dueLogs = dList
            )
        } catch (e: Exception) {
            Log.e(TAG, "parseUserShopData failed", e)
            null
        }
    }
}
