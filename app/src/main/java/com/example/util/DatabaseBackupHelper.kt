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
        subject: String = "Shop Database Backup / দোকান খাতা ব্যাকআপ",
        body: String = "দোকানের সকল পণ্যের হিসাব, বিক্রয় ও বাকি খাতার ডাটাবেস ব্যাকআপ ফাইল নিচে সংযুক্ত করা হয়েছে।"
    ) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val fileUri: Uri = FileProvider.getUriForFile(context, authority, file)

            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                if (recipientEmail.isNotBlank()) {
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
                }
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(emailIntent, "Gmail বা ইমেইলে ব্যাকআপ পাঠান")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to universal share
            shareBackupFile(context, file, subject, body)
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
