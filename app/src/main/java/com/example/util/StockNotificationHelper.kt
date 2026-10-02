package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.data.model.Product

object StockNotificationHelper {

    const val CHANNEL_ID = "low_stock_channel"
    private const val CHANNEL_NAME = "স্টক সতর্কতা (Low Stock Alerts)"
    private const val CHANNEL_DESCRIPTION = "পণ্যের স্টক ব্যবহারকারী নির্ধারিত সীমার নিচে নামলে স্বয়ংক্রিয় নোটিফিকেশন প্রদান করে।"

    // Cache of last notified time per product to avoid duplicate alerts within short intervals (10 minutes)
    private val lastNotifiedMap = mutableMapOf<Long, Long>()
    private const val NOTIFY_DEBOUNCE_MILLIS = 10 * 60 * 1000L

    /**
     * Initializes the notification channel on Android 8.0+ (API 26+)
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 350, 150, 350)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Checks if notification permission is granted
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    /**
     * Sends an automated high-priority notification for a product whose stock has dropped below its threshold.
     */
    fun notifyProductLowStock(
        context: Context,
        product: Product,
        threshold: Double,
        force: Boolean = false
    ) {
        createNotificationChannel(context)

        // Check if notifications are enabled in user preferences
        val prefs = context.getSharedPreferences("shop_khata_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("is_low_stock_notification_enabled", true)
        if (!isEnabled) return

        if (!hasNotificationPermission(context)) return

        val now = System.currentTimeMillis()
        val lastTime = lastNotifiedMap[product.id] ?: 0L
        if (!force && (now - lastTime < NOTIFY_DEBOUNCE_MILLIS)) {
            // Recently notified for this product, skip to avoid spamming
            return
        }
        lastNotifiedMap[product.id] = now

        val isOutOfStock = product.stockQuantity <= 0.0
        val qtyDisplay = if (product.stockQuantity % 1.0 == 0.0) product.stockQuantity.toInt().toString() else product.stockQuantity.toString()
        val thresholdDisplay = if (threshold % 1.0 == 0.0) threshold.toInt().toString() else threshold.toString()

        val title = if (isOutOfStock) {
            "🚨 জরুরি: '${product.name}' এর স্টক সম্পূর্ণ শেষ!"
        } else {
            "⚠️ স্টক সতর্কতা: '${product.name}' এর স্টক কমে গেছে!"
        }

        val content = if (isOutOfStock) {
            "বর্তমানে ০ ${product.unit} স্টক রয়েছে। অবিলম্বে স্টক-ইন অথবা নতুন অর্ডার করুন।"
        } else {
            "বর্তমান স্টক মাত্র $qtyDisplay ${product.unit} (আপনার সতর্কীকরণ সীমা: $thresholdDisplay ${product.unit})। দ্রুত রিস্টক করুন।"
        }

        // Tap action: opens MainActivity and navigates directly to Inventory tab
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("NAV_SCREEN", "INVENTORY")
            putExtra("INVENTORY_TAB", 1) // 1: Low Stock Tab
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            product.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val smallIcon = android.R.drawable.stat_notify_error

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(content)
                    .setSummaryText(if (isOutOfStock) "স্টক শেষ সতর্কতা" else "কম স্টক সতর্কীকরণ")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(if (isOutOfStock) Color.RED else Color.rgb(217, 119, 6)) // Red or Amber
            .addAction(
                android.R.drawable.ic_menu_agenda,
                "স্টক খাতা দেখুন",
                pendingIntent
            )
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            val notificationId = 1000 + (product.id.toInt() % 8000)
            notificationManager.notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Permission not granted or revoked
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Sends a summary notification if multiple products are low in stock
     */
    fun notifyMultipleLowStock(
        context: Context,
        lowProducts: List<Product>,
        force: Boolean = false
    ) {
        if (lowProducts.isEmpty()) return
        if (lowProducts.size == 1) {
            notifyProductLowStock(context, lowProducts.first(), lowProducts.first().minStockAlert, force)
            return
        }

        createNotificationChannel(context)

        val prefs = context.getSharedPreferences("shop_khata_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("is_low_stock_notification_enabled", true)
        if (!isEnabled || !hasNotificationPermission(context)) return

        val now = System.currentTimeMillis()
        val lastSummaryTime = prefs.getLong("last_low_stock_summary_notified", 0L)
        if (!force && (now - lastSummaryTime < NOTIFY_DEBOUNCE_MILLIS)) return
        prefs.edit().putLong("last_low_stock_summary_notified", now).apply()

        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("NAV_SCREEN", "INVENTORY")
            putExtra("INVENTORY_TAB", 1)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            9999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "⚠️ সতর্কবার্তা: ${lowProducts.size}টি পণ্যের স্টক ন্যূনতম সীমার নিচে!"
        val productListText = lowProducts.take(4).joinToString(", ") { it.name } +
                if (lowProducts.size > 4) " এবং আরও ${lowProducts.size - 4}টি" else ""
        val content = "পণ্যসমূহ: $productListText। দ্রুত দোকানে স্টক সংগ্রহ অথবা ক্রয় অর্ডার করুন।"

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(title)
            .setSummaryText("${lowProducts.size}টি কম স্টক পণ্য")

        for (p in lowProducts.take(5)) {
            val q = if (p.stockQuantity % 1.0 == 0.0) p.stockQuantity.toInt().toString() else p.stockQuantity.toString()
            val l = if (p.minStockAlert % 1.0 == 0.0) p.minStockAlert.toInt().toString() else p.minStockAlert.toString()
            inboxStyle.addLine("• ${p.name}: স্টক $q ${p.unit} (সীমা: $l)")
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(Color.rgb(217, 119, 6))
            .addAction(
                android.R.drawable.ic_menu_agenda,
                "কম স্টক তালিকা খুলুন",
                pendingIntent
            )
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(9999, notification)
        } catch (_: Exception) {}
    }
}
