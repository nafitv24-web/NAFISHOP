package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object DatabaseBackupHelper {

    private const val DB_NAME = "shop_khata_database"

    /**
     * Copies the local Room/SQLite database (.db file) to a shareable backup file in cache.
     * Checkpoints WAL mode first so all in-memory/WAL transactions are flushed to the main file.
     */
    suspend fun exportDatabaseFile(context: Context, database: AppDatabase): File? = withContext(Dispatchers.IO) {
        try {
            // 1. Force SQLite WAL checkpoint to ensure all data is written to disk
            try {
                val db = database.openHelper.writableDatabase
                val cursor = db.query("PRAGMA wal_checkpoint(FULL)")
                cursor.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val originDbFile = context.getDatabasePath(DB_NAME)
            if (!originDbFile.exists()) {
                return@withContext null
            }

            val backupDir = File(context.cacheDir, "backups").apply {
                if (!exists()) mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            val backupFile = File(backupDir, "DokanKhata_DB_Backup_$timestamp.db")

            FileInputStream(originDbFile).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }

            backupFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves JSON backup content into a formatted .json file in cache for export/sharing.
     */
    suspend fun exportJsonBackupFile(context: Context, jsonData: String): File? = withContext(Dispatchers.IO) {
        try {
            val backupDir = File(context.cacheDir, "backups").apply {
                if (!exists()) mkdirs()
            }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
            val jsonFile = File(backupDir, "DokanKhata_Backup_$timestamp.json")

            jsonFile.writeText(jsonData, Charsets.UTF_8)
            jsonFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Shares a backup file (DB or JSON) directly to Google Drive, Gmail, WhatsApp, or File Manager.
     */
    fun shareBackupFile(
        context: Context,
        file: File,
        subject: String = "দোকান খাতা ডাটাবেস ব্যাকআপ",
        message: String = "দোকান খাতার ডাটাবেস ব্যাকআপ ফাইল সংযুক্ত করা হলো।"
    ) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val fileUri: Uri = FileProvider.getUriForFile(context, authority, file)

            val mimeType = when {
                file.name.endsWith(".json", ignoreCase = true) -> "application/json"
                file.name.endsWith(".db", ignoreCase = true) -> "application/x-sqlite3"
                else -> "application/octet-stream"
            }

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, message)
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "ব্যাকআপ শেয়ার / গুগল ড্রাইভে সেভ করুন")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "শেয়ার করতে ত্রুটি: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Directly opens Gmail or Mail client with the backup file attached to user's email.
     */
    fun shareViaEmail(
        context: Context,
        file: File,
        recipientEmail: String = "",
        subject: String = "দোকান খাতা ব্যাকআপ ফাইল",
        body: String = "দোকানের সকল পণ্যের হিসাব, বিক্রয় ও বাকি খাতার ডাটাবেস ব্যাকআপ ফাইল নিচে সংযুক্ত করা হয়েছে।"
    ) {
        sendBackupToGmail(context, file, recipientEmail, subject, body)
    }

    /**
     * Directly launches Gmail app with user's own email pre-filled as recipient
     */
    fun sendBackupToGmail(
        context: Context,
        file: File,
        recipientEmail: String = "",
        subject: String = "নাফী খাতা ডাটা ব্যাকআপ",
        body: String = "আসসালামু আলাইকুম,\n\nআপনার নাফী খাতা শপের সকল পণ্যের হিসাব, বিক্রয়, ক্যাশ ও বাকি খাতার সম্পূর্ণ ব্যাকআপ ফাইলটি নিচে সংযুক্ত করা হলো।\n\nএই ফাইলটি আপনার জিমেইলে চিরদিনের জন্য সুরক্ষিত থাকবে। যেকোনো নতুন ফোনে অ্যাপ ইন্সটল করে এই ফাইলটি দিয়ে ১-ক্লিকে সকল ডাটা রিস্টোর করতে পারবেন।\n\n- নাফী খাতা অ্যাপ"
    ) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val fileUri: Uri = FileProvider.getUriForFile(context, authority, file)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                if (recipientEmail.isNotBlank()) {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
                }
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // Attempt to target Gmail app directly
                setPackage("com.google.android.gm")
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                // Fallback to standard email chooser
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "*/*"
                    if (recipientEmail.isNotBlank()) {
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
                    }
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(fallbackIntent, "Gmail-এ ব্যাকআপ পাঠান")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to universal share
            shareBackupFile(context, file, subject, body)
        }
    }

    /**
     * Directly launches Google Drive "Save to Drive" or file chooser
     */
    fun saveBackupDirectlyToGoogleDrive(
        context: Context,
        file: File
    ) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val fileUri: Uri = FileProvider.getUriForFile(context, authority, file)

            val driveIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "নাফী খাতা ডাটা ব্যাকআপ")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage("com.google.android.apps.docs")
            }

            if (driveIntent.resolveActivity(context.packageManager) != null) {
                driveIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(driveIntent)
            } else {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    putExtra(Intent.EXTRA_SUBJECT, "নাফী খাতা ডাটা ব্যাকআপ")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Google Drive-এ সংরক্ষণ করুন")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            shareBackupFile(context, file, "নাফী খাতা ব্যাকআপ", "ব্যাকআপ ফাইলটি গুগল ড্রাইভে সেভ করুন")
        }
    }

    /**
     * Writes text (JSON) to a user-selected SAF Document Uri (e.g. Downloads or Google Drive folder).
     */
    suspend fun writeTextToUri(context: Context, uri: Uri, text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(text.toByteArray(Charsets.UTF_8))
                output.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Writes SQLite DB binary to a user-selected SAF Document Uri.
     */
    suspend fun writeDatabaseToUri(context: Context, database: AppDatabase, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            try {
                val db = database.openHelper.writableDatabase
                val cursor = db.query("PRAGMA wal_checkpoint(FULL)")
                cursor.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val originDbFile = context.getDatabasePath(DB_NAME)
            if (!originDbFile.exists()) return@withContext false

            context.contentResolver.openOutputStream(uri)?.use { output ->
                FileInputStream(originDbFile).use { input ->
                    input.copyTo(output)
                }
                output.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Reads text from any user-selected Document Uri (e.g. from Google Drive, Downloads, WhatsApp).
     */
    suspend fun readTextFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Restores SQLite DB file directly from a user-selected Uri.
     */
    suspend fun restoreDatabaseFromFileUri(context: Context, database: AppDatabase, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val targetDb = context.getDatabasePath(DB_NAME)

            // Close current Room database helper if possible
            try {
                database.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Clean up temporary WAL files
            val walFile = File(targetDb.parentFile, "$DB_NAME-wal")
            val shmFile = File(targetDb.parentFile, "$DB_NAME-shm")
            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetDb).use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
