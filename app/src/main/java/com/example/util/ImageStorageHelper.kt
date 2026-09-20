package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import android.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

object ImageStorageHelper {

    private const val MAX_DIMENSION = 400
    private const val BACKUP_MAX_DIMENSION = 350
    private const val QUALITY = 75
    private const val BACKUP_QUALITY = 70

    /**
     * Compresses and saves an image from any Uri (Camera/Gallery) into private app storage.
     * Returns the local file Uri string (e.g. file:///data/user/0/.../prod_123.jpg).
     */
    fun saveCompressedImage(context: Context, sourceUri: Uri, prefix: String = "prod"): String? {
        return try {
            val bitmap = decodeSampledBitmapFromUri(context, sourceUri, MAX_DIMENSION, MAX_DIMENSION) ?: return null
            val rotatedBitmap = fixOrientation(context, sourceUri, bitmap)

            val dir = File(context.filesDir, "product_images").apply { if (!exists()) mkdirs() }
            val destFile = File(dir, "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")

            FileOutputStream(destFile).use { out ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
            }
            if (rotatedBitmap != bitmap) {
                rotatedBitmap.recycle()
            }
            bitmap.recycle()

            Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Converts a local image URI or path into a compact Base64 string for cloud/drive backup.
     */
    fun imageUriToBase64(context: Context, uriString: String?): String? {
        if (uriString.isNullOrBlank()) return null
        if (uriString.startsWith("data:image/")) return uriString

        return try {
            val uri = if (uriString.startsWith("/") || uriString.startsWith("file:")) {
                val cleanPath = uriString.removePrefix("file://")
                val file = File(cleanPath)
                if (!file.exists()) return null
                Uri.fromFile(file)
            } else {
                Uri.parse(uriString)
            }

            val bitmap = decodeSampledBitmapFromUri(context, uri, BACKUP_MAX_DIMENSION, BACKUP_MAX_DIMENSION) ?: return null
            val rotated = fixOrientation(context, uri, bitmap)

            val baos = ByteArrayOutputStream()
            rotated.compress(Bitmap.CompressFormat.JPEG, BACKUP_QUALITY, baos)
            val bytes = baos.toByteArray()

            if (rotated != bitmap) rotated.recycle()
            bitmap.recycle()

            "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Converts a Base64 image string back into a local private image file on restore.
     */
    fun base64ToLocalImageUri(context: Context, base64String: String?, prefix: String = "restored"): String? {
        if (base64String.isNullOrBlank()) return null
        return try {
            val cleanB64 = if (base64String.contains(",")) {
                base64String.substringAfter(",")
            } else {
                base64String
            }
            val bytes = Base64.decode(cleanB64, Base64.DEFAULT)
            if (bytes == null || bytes.isEmpty()) return null

            val dir = File(context.filesDir, "product_images").apply { if (!exists()) mkdirs() }
            val destFile = File(dir, "${prefix}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")

            FileOutputStream(destFile).use { out ->
                out.write(bytes)
            }
            Uri.fromFile(destFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun decodeSampledBitmapFromUri(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        var input: InputStream? = null
        return try {
            input = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(input, null, options)
            input?.close()

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false

            input = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(input, null, options)
        } catch (e: Exception) {
            null
        } finally {
            try { input?.close() } catch (ignored: Exception) {}
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun fixOrientation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        var input: InputStream? = null
        return try {
            input = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(input)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            bitmap
        } finally {
            try { input?.close() } catch (ignored: Exception) {}
        }
    }
}
