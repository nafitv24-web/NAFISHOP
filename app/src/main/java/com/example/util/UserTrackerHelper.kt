package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import com.example.data.firebase.FirebaseRealtimeManager
import com.example.data.firebase.UserSessionLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

data class UserLocationInfo(
    val ip: String = "",
    val city: String = "",
    val region: String = "",
    val country: String = "",
    val countryCode: String = "",
    val isp: String = "",
    val networkType: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val deviceModel: String = "",
    val androidVersion: String = "",
    val appVersion: String = "",
    val locationDisplay: String = ""
)

object UserTrackerHelper {

    private const val TAG = "UserTrackerHelper"
    private const val PREFS_NAME = "user_tracking_cache"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaTypeOrNull()

    /**
     * Resolves the user's current network type (WiFi, Mobile Data, Ethernet, Offline)
     */
    fun getNetworkType(context: Context): String {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork ?: return "অফলাইন (Offline)"
            val caps = cm.getNetworkCapabilities(network) ?: return "অজানা নেটওয়ার্ক"

            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                    val carrier = getCarrierName(context)
                    if (carrier.isNotBlank()) "Mobile Data ($carrier)" else "Mobile Data"
                }
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "ইন্টারনেট"
            }
        } catch (e: Exception) {
            "মোবাইল নেটওয়ার্ক"
        }
    }

    private fun getCarrierName(context: Context): String {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val simOperator = tm?.simOperatorName?.trim()
            val netOperator = tm?.networkOperatorName?.trim()
            when {
                !simOperator.isNullOrBlank() -> simOperator
                !netOperator.isNullOrBlank() -> netOperator
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Formats device model name nicely
     */
    fun getDeviceModel(): String {
        val manufacturer = Build.MANUFACTURER.orEmpty().replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
        val model = Build.MODEL.orEmpty()
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model
        } else {
            "$manufacturer $model".trim()
        }
    }

    /**
     * Formats Android OS and SDK version
     */
    fun getAndroidVersion(): String {
        return "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    }

    /**
     * Fetches real-time geographic location and IP information.
     * Uses free public IP Geolocation APIs with automatic fallback and local caching.
     */
    suspend fun detectLocationAndIp(context: Context, appVersion: String): UserLocationInfo = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val networkType = getNetworkType(context)
        val deviceModel = getDeviceModel()
        val androidVersion = getAndroidVersion()

        // 1. Try ip-api.com (fast, free, returns city, region, country, isp, lat, lon, ip)
        var info: UserLocationInfo? = fetchFromIpApi(networkType, deviceModel, androidVersion, appVersion)

        // 2. Fallback to ipapi.co if ip-api.com failed
        if (info == null || info.city.isBlank()) {
            info = fetchFromIpApiCo(networkType, deviceModel, androidVersion, appVersion)
        }

        // 3. Fallback to cached location or device locale/timezone
        if (info == null || info.city.isBlank()) {
            val cachedCity = prefs.getString("cached_city", "") ?: ""
            val cachedCountry = prefs.getString("cached_country", "") ?: ""
            val cachedIp = prefs.getString("cached_ip", "") ?: ""
            val cachedIsp = prefs.getString("cached_isp", "") ?: ""
            val cachedLat = prefs.getFloat("cached_lat", 0f).toDouble()
            val cachedLon = prefs.getFloat("cached_lon", 0f).toDouble()

            val fallbackCountry = if (cachedCountry.isNotBlank()) cachedCountry else Locale.getDefault().displayCountry.ifBlank { "Bangladesh" }
            val fallbackCity = if (cachedCity.isNotBlank()) cachedCity else {
                val tz = TimeZone.getDefault().id
                if (tz.contains("Dhaka", ignoreCase = true)) "Dhaka" else tz.substringAfter("/")
            }
            val display = if (fallbackCity.isNotBlank()) "$fallbackCity, $fallbackCountry" else fallbackCountry

            info = UserLocationInfo(
                ip = if (cachedIp.isNotBlank()) cachedIp else "103.xxx (মোবাইল আইপি)",
                city = fallbackCity,
                region = prefs.getString("cached_region", "") ?: "",
                country = fallbackCountry,
                countryCode = if (fallbackCountry.contains("Bangla", ignoreCase = true)) "BD" else Locale.getDefault().country,
                isp = if (cachedIsp.isNotBlank()) cachedIsp else getCarrierName(context).ifBlank { "ইন্টারনেট সার্ভিস" },
                networkType = networkType,
                lat = cachedLat,
                lon = cachedLon,
                deviceModel = deviceModel,
                androidVersion = androidVersion,
                appVersion = appVersion,
                locationDisplay = display
            )
        }

        // Cache the successful location
        if (info.city.isNotBlank()) {
            prefs.edit()
                .putString("cached_city", info.city)
                .putString("cached_region", info.region)
                .putString("cached_country", info.country)
                .putString("cached_ip", info.ip)
                .putString("cached_isp", info.isp)
                .putFloat("cached_lat", info.lat.toFloat())
                .putFloat("cached_lon", info.lon.toFloat())
                .apply()
        }

        info
    }

    private fun fetchFromIpApi(
        networkType: String,
        deviceModel: String,
        androidVersion: String,
        appVersion: String
    ): UserLocationInfo? {
        return try {
            val url = "http://ip-api.com/json/?fields=status,message,country,countryCode,region,regionName,city,district,lat,lon,timezone,isp,org,as,query"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "NafiKhataAndroidApp/2.4")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string()
            if (response.isSuccessful && !body.isNullOrBlank()) {
                val json = JSONObject(body)
                if (json.optString("status") == "success") {
                    val city = json.optString("city", "").trim()
                    val region = json.optString("regionName", "").trim()
                    val country = json.optString("country", "Bangladesh").trim()
                    val countryCode = json.optString("countryCode", "BD").trim()
                    val isp = json.optString("isp", "").ifBlank { json.optString("org", "") }.trim()
                    val ip = json.optString("query", "").trim()
                    val lat = json.optDouble("lat", 0.0)
                    val lon = json.optDouble("lon", 0.0)

                    val display = when {
                        city.isNotBlank() && country.isNotBlank() -> "$city, $country"
                        city.isNotBlank() -> city
                        country.isNotBlank() -> country
                        else -> "বাংলাদেশ"
                    }

                    return UserLocationInfo(
                        ip = ip,
                        city = city,
                        region = region,
                        country = country,
                        countryCode = countryCode,
                        isp = isp,
                        networkType = networkType,
                        lat = lat,
                        lon = lon,
                        deviceModel = deviceModel,
                        androidVersion = androidVersion,
                        appVersion = appVersion,
                        locationDisplay = display
                    )
                }
            }
            null
        } catch (e: Exception) {
            Log.d(TAG, "ip-api.com lookup skipped: ${e.message}")
            null
        }
    }

    private fun fetchFromIpApiCo(
        networkType: String,
        deviceModel: String,
        androidVersion: String,
        appVersion: String
    ): UserLocationInfo? {
        return try {
            val url = "https://ipapi.co/json/"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "NafiKhataAndroidApp/2.4")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val body = response.body?.string()
            if (response.isSuccessful && !body.isNullOrBlank()) {
                val json = JSONObject(body)
                val city = json.optString("city", "").trim()
                val region = json.optString("region", "").trim()
                val country = json.optString("country_name", "Bangladesh").trim()
                val countryCode = json.optString("country_code", "BD").trim()
                val isp = json.optString("org", "").trim()
                val ip = json.optString("ip", "").trim()
                val lat = json.optDouble("latitude", 0.0)
                val lon = json.optDouble("longitude", 0.0)

                val display = when {
                    city.isNotBlank() && country.isNotBlank() -> "$city, $country"
                    city.isNotBlank() -> city
                    country.isNotBlank() -> country
                    else -> "বাংলাদেশ"
                }

                return UserLocationInfo(
                    ip = ip,
                    city = city,
                    region = region,
                    country = country,
                    countryCode = countryCode,
                    isp = isp,
                    networkType = networkType,
                    lat = lat,
                    lon = lon,
                    deviceModel = deviceModel,
                    androidVersion = androidVersion,
                    appVersion = appVersion,
                    locationDisplay = display
                )
            }
            null
        } catch (e: Exception) {
            Log.d(TAG, "ipapi.co lookup skipped: ${e.message}")
            null
        }
    }

    /**
     * Records app entry, user location, IP, device, and network to Firebase Realtime Database.
     * Also appends to user's session history so admin can see full access timeline.
     */
    suspend fun recordAppEntryInCloud(
        context: Context,
        email: String,
        shopName: String,
        ownerName: String,
        appVersion: String
    ) = withContext(Dispatchers.IO) {
        if (email.isBlank()) return@withContext

        try {
            val locationInfo = detectLocationAndIp(context, appVersion)
            val sanitized = FirebaseRealtimeManager.sanitizeEmail(email)
            val now = System.currentTimeMillis()

            // 1. Fetch existing user entry to read prior appOpenCount and recent sessions
            val userUrl = "${FirebaseRealtimeManager.DATABASE_URL}/users/$sanitized.json"
            val getReq = Request.Builder().url(userUrl).get().build()
            val getResp = httpClient.newCall(getReq).execute()
            val existingBody = getResp.body?.string()

            var previousCount = 0
            var previousCreatedAt = now
            val existingSessions = mutableListOf<JSONObject>()

            if (getResp.isSuccessful && !existingBody.isNullOrBlank() && existingBody != "null") {
                val existingObj = JSONObject(existingBody)
                previousCount = existingObj.optInt("appEntryCount", 0)
                previousCreatedAt = existingObj.optLong("createdAt", now)

                val sessArray = existingObj.optJSONArray("sessions")
                if (sessArray != null) {
                    for (i in 0 until sessArray.length()) {
                        existingSessions.add(sessArray.getJSONObject(i))
                    }
                }
            }

            val newCount = previousCount + 1

            // 2. Build new session log object
            val newSession = JSONObject().apply {
                put("timestamp", now)
                put("ip", locationInfo.ip)
                put("city", locationInfo.city)
                put("region", locationInfo.region)
                put("country", locationInfo.country)
                put("isp", locationInfo.isp)
                put("deviceModel", locationInfo.deviceModel)
                put("networkType", locationInfo.networkType)
                put("lat", locationInfo.lat)
                put("lon", locationInfo.lon)
            }

            // Keep only latest 20 sessions (newest at start)
            val updatedSessionsArray = JSONArray()
            updatedSessionsArray.put(newSession)
            for (s in existingSessions.take(19)) {
                updatedSessionsArray.put(s)
            }

            // 3. Patch user record with updated access time, location, IP, and count
            val patchObj = JSONObject().apply {
                put("email", email.trim())
                if (shopName.isNotBlank()) put("shopName", shopName)
                if (ownerName.isNotBlank()) put("ownerName", ownerName)
                put("createdAt", previousCreatedAt)
                put("lastLoginAt", now)
                put("lastAppEntryAt", now)
                put("lastActiveAt", now)
                put("appEntryCount", newCount)
                put("ipAddress", locationInfo.ip)
                put("city", locationInfo.city)
                put("region", locationInfo.region)
                put("country", locationInfo.country)
                put("countryCode", locationInfo.countryCode)
                put("isp", locationInfo.isp)
                put("networkType", locationInfo.networkType)
                put("deviceModel", locationInfo.deviceModel)
                put("androidVersion", locationInfo.androidVersion)
                put("appVersion", locationInfo.appVersion)
                put("latitude", locationInfo.lat)
                put("longitude", locationInfo.lon)
                put("locationDisplay", locationInfo.locationDisplay)
                put("isOnline", true)
                put("sessions", updatedSessionsArray)
            }

            val patchReq = Request.Builder()
                .url(userUrl)
                .patch(patchObj.toString().toRequestBody(jsonMediaType))
                .build()

            val patchResp = httpClient.newCall(patchReq).execute()
            if (patchResp.isSuccessful) {
                Log.d(TAG, "Recorded user entry and location for $email: ${locationInfo.city}, ${locationInfo.ip}")
            }

            // 4. Update live presence heartbeat
            val pingUrl = "${FirebaseRealtimeManager.DATABASE_URL}/active_pings/$sanitized.json"
            val pingObj = JSONObject().apply {
                put("email", email.trim())
                put("lastPing", now)
                put("city", locationInfo.city)
                put("country", locationInfo.country)
                put("deviceModel", locationInfo.deviceModel)
                put("networkType", locationInfo.networkType)
            }
            val pingReq = Request.Builder()
                .url(pingUrl)
                .put(pingObj.toString().toRequestBody(jsonMediaType))
                .build()
            httpClient.newCall(pingReq).execute()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to record cloud app entry: ${e.localizedMessage}")
        }
    }

    /**
     * Heartbeat ping to mark user as currently online while app is active
     */
    suspend fun pingHeartbeat(email: String) = withContext(Dispatchers.IO) {
        if (email.isBlank()) return@withContext
        try {
            val sanitized = FirebaseRealtimeManager.sanitizeEmail(email)
            val now = System.currentTimeMillis()
            val url = "${FirebaseRealtimeManager.DATABASE_URL}/users/$sanitized.json"
            val patchObj = JSONObject().apply {
                put("lastActiveAt", now)
                put("isOnline", true)
            }
            val req = Request.Builder()
                .url(url)
                .patch(patchObj.toString().toRequestBody(jsonMediaType))
                .build()
            httpClient.newCall(req).execute()
        } catch (e: Exception) {
            // ignore heartbeat errors
        }
    }
}
