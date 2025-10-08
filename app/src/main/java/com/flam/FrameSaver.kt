package com.flam

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class for saving processed frames to Downloads folder
 * Supports both legacy (pre-Android 10) and scoped storage (Android 10+)
 */
class FrameSaver(private val context: Context) {

    companion object {
        private const val TAG = "FrameSaver"
        private const val FOLDER_NAME = "Flam"
        private const val FILE_PREFIX = "flam_frame_"
        private const val MIME_TYPE = "image/png"
        private const val QUALITY = 100 // PNG quality (lossless)
    }

    /**
     * Save RGBA byte array as PNG to Downloads folder
     *
     * @param rgbaData RGBA byte array
     * @param width Image width
     * @param height Image height
     * @param includeBase64 If true, also saves base64-encoded version
     * @return Pair of file Uri and optional base64 string
     */
    fun saveFrame(
        rgbaData: ByteArray,
        width: Int,
        height: Int,
        includeBase64: Boolean = false
    ): Pair<Uri?, String?> {
        try {
            // Convert RGBA byte array to Bitmap
            val bitmap = rgbaDataToBitmap(rgbaData, width, height)
            if (bitmap == null) {
                Log.e(TAG, "Failed to convert RGBA data to Bitmap")
                return Pair(null, null)
            }

            // Generate filename with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val filename = "${FILE_PREFIX}${timestamp}.png"

            // Save to Downloads folder
            val uri = saveBitmapToDownloads(bitmap, filename)

            // Generate base64 if requested
            val base64String = if (includeBase64) {
                bitmapToBase64(bitmap)
            } else {
                null
            }

            // Recycle bitmap
            bitmap.recycle()

            Log.i(TAG, "Frame saved successfully: $filename")
            return Pair(uri, base64String)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save frame: ${e.message}", e)
            return Pair(null, null)
        }
    }

    /**
     * Save processed frame with metadata as JSON + PNG
     */
    fun saveFrameWithMetadata(
        rgbaData: ByteArray,
        width: Int,
        height: Int,
        metadata: Map<String, Any>
    ): Uri? {
        try {
            val bitmap = rgbaDataToBitmap(rgbaData, width, height) ?: return null
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val filename = "${FILE_PREFIX}${timestamp}.png"

            // Save PNG
            val uri = saveBitmapToDownloads(bitmap, filename)

            // Save metadata JSON
            val metadataFilename = "${FILE_PREFIX}${timestamp}_metadata.json"
            val metadataJson = buildMetadataJson(metadata, width, height)
            saveTextToDownloads(metadataJson, metadataFilename)

            bitmap.recycle()
            return uri

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save frame with metadata: ${e.message}", e)
            return null
        }
    }

    /**
     * Convert RGBA byte array to Bitmap
     */
    private fun rgbaDataToBitmap(rgbaData: ByteArray, width: Int, height: Int): Bitmap? {
        return try {
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val buffer = ByteBuffer.wrap(rgbaData)
            bitmap.copyPixelsFromBuffer(buffer)
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error creating bitmap: ${e.message}", e)
            null
        }
    }

    /**
     * Save bitmap to Downloads folder using MediaStore (Android 10+) or File API (older)
     */
    private fun saveBitmapToDownloads(bitmap: Bitmap, filename: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ (Scoped Storage)
            saveBitmapWithMediaStore(bitmap, filename)
        } else {
            // Android 9 and below
            saveBitmapWithFileApi(bitmap, filename)
        }
    }

    /**
     * Save bitmap using MediaStore API (Android 10+)
     */
    private fun saveBitmapWithMediaStore(bitmap: Bitmap, filename: String): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, MIME_TYPE)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER_NAME")
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        return uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, QUALITY, outputStream)
                outputStream.flush()
            }
            Log.i(TAG, "Saved to MediaStore: $uri")
            it
        }
    }

    /**
     * Save bitmap using File API (Android 9 and below)
     */
    private fun saveBitmapWithFileApi(bitmap: Bitmap, filename: String): Uri? {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val flamDir = File(downloadsDir, FOLDER_NAME)

        if (!flamDir.exists()) {
            flamDir.mkdirs()
        }

        val file = File(flamDir, filename)

        return try {
            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, QUALITY, outputStream)
                outputStream.flush()
            }
            Log.i(TAG, "Saved to file: ${file.absolutePath}")
            Uri.fromFile(file)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving with File API: ${e.message}", e)
            null
        }
    }

    /**
     * Save text content to Downloads folder
     */
    private fun saveTextToDownloads(content: String, filename: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER_NAME")
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(content.toByteArray())
                    outputStream.flush()
                }
                it
            }
        } else {
            // Android 9 and below
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val flamDir = File(downloadsDir, FOLDER_NAME)
            if (!flamDir.exists()) flamDir.mkdirs()

            val file = File(flamDir, filename)
            FileOutputStream(file).use { it.write(content.toByteArray()) }
            Uri.fromFile(file)
        }
    }

    /**
     * Convert bitmap to base64 PNG string
     */
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, QUALITY, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    /**
     * Build metadata JSON
     */
    private fun buildMetadataJson(metadata: Map<String, Any>, width: Int, height: Int): String {
        val timestamp = System.currentTimeMillis()
        return """
        {
            "width": $width,
            "height": $height,
            "timestamp": $timestamp,
            "resolution": "${width}x${height}",
            ${metadata.entries.joinToString(",\n    ") { (key, value) ->
                when (value) {
                    is String -> "\"$key\": \"$value\""
                    is Number -> "\"$key\": $value"
                    is Boolean -> "\"$key\": $value"
                    else -> "\"$key\": \"$value\""
                }
            }}
        }
        """.trimIndent()
    }

    /**
     * Get saved frames directory path
     */
    fun getSavedFramesPath(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER_NAME"
        } else {
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                FOLDER_NAME
            ).absolutePath
        }
    }
}
