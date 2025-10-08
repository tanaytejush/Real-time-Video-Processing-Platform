package com.flam

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.util.Base64
import android.util.Log
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*

/**
 * Export processed frames as JSON with base64-encoded images
 * For web viewer integration
 */
class FrameExporter(private val context: Context) {

    companion object {
        private const val TAG = "FrameExporter"
        private const val EXPORT_FOLDER = "Flam/WebExport"
        private const val LATEST_FRAME_FILE = "latest_frame.json"
        private const val HISTORY_FILE = "frame_history.json"
        private const val MAX_HISTORY = 100
    }

    data class FrameData(
        val image: String,  // base64-encoded PNG
        val fps: Float,
        val resolution: String,
        val width: Int,
        val height: Int,
        val timestamp: String,
        val timestampMs: Long,
        val processingTimeMs: Long,
        val cannyLowThreshold: Int,
        val cannyHighThreshold: Int,
        val deviceInfo: String,
        val frameNumber: Int
    )

    /**
     * Export current frame as JSON with base64 image
     *
     * @param frameData RGBA byte array
     * @param width Frame width
     * @param height Frame height
     * @param fps Current FPS
     * @param processingTimeMs Processing time in milliseconds
     * @param lowThreshold Canny low threshold
     * @param highThreshold Canny high threshold
     * @param frameNumber Sequential frame number
     * @return Pair of (JSON file URI, JSON string)
     */
    fun exportFrameAsJson(
        frameData: ByteArray,
        width: Int,
        height: Int,
        fps: Float,
        processingTimeMs: Long,
        lowThreshold: Int = 50,
        highThreshold: Int = 150,
        frameNumber: Int = 0
    ): Pair<Uri?, String?> {
        try {
            // Convert RGBA to bitmap
            val bitmap = rgbaDataToBitmap(frameData, width, height) ?: return Pair(null, null)

            // Convert bitmap to base64 PNG
            val base64Image = bitmapToBase64Png(bitmap)
            bitmap.recycle()

            // Build JSON object
            val timestamp = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            dateFormat.timeZone = TimeZone.getTimeZone("UTC")
            val timestampStr = dateFormat.format(Date(timestamp))

            val json = JSONObject().apply {
                put("image", "data:image/png;base64,$base64Image")
                put("fps", fps.toDouble())
                put("resolution", "${width}x${height}")
                put("width", width)
                put("height", height)
                put("timestamp", timestampStr)
                put("timestampMs", timestamp)
                put("processingTimeMs", processingTimeMs)
                put("cannyLowThreshold", lowThreshold)
                put("cannyHighThreshold", highThreshold)
                put("deviceInfo", "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
                put("androidVersion", android.os.Build.VERSION.SDK_INT)
                put("frameNumber", frameNumber)
            }

            val jsonString = json.toString(2)  // Pretty print with indent

            // Save to external storage
            val exportDir = getExportDirectory()
            val latestFile = File(exportDir, LATEST_FRAME_FILE)

            FileOutputStream(latestFile).use { out ->
                out.write(jsonString.toByteArray())
                out.flush()
            }

            Log.i(TAG, "Exported frame to: ${latestFile.absolutePath}")
            Log.d(TAG, "JSON size: ${jsonString.length / 1024}KB, base64 size: ${base64Image.length / 1024}KB")

            // Also add to history
            addToHistory(json)

            return Pair(Uri.fromFile(latestFile), jsonString)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to export frame as JSON: ${e.message}", e)
            return Pair(null, null)
        }
    }

    /**
     * Export frame with metadata object
     */
    fun exportFrameWithMetadata(
        frameData: ByteArray,
        width: Int,
        height: Int,
        metadata: Map<String, Any>
    ): Pair<Uri?, String?> {
        val fps = (metadata["fps"] as? Number)?.toFloat() ?: 0f
        val processingTime = (metadata["processing_time_ms"] as? Number)?.toLong() ?: 0L
        val lowThreshold = (metadata["low_threshold"] as? Number)?.toInt() ?: 50
        val highThreshold = (metadata["high_threshold"] as? Number)?.toInt() ?: 150
        val frameNumber = (metadata["frame_number"] as? Number)?.toInt() ?: 0

        return exportFrameAsJson(
            frameData, width, height,
            fps, processingTime,
            lowThreshold, highThreshold,
            frameNumber
        )
    }

    /**
     * Get the latest exported frame JSON
     */
    fun getLatestFrameJson(): String? {
        return try {
            val exportDir = getExportDirectory()
            val latestFile = File(exportDir, LATEST_FRAME_FILE)

            if (latestFile.exists()) {
                latestFile.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read latest frame: ${e.message}", e)
            null
        }
    }

    /**
     * Get frame history as JSON array
     */
    fun getFrameHistory(): String? {
        return try {
            val exportDir = getExportDirectory()
            val historyFile = File(exportDir, HISTORY_FILE)

            if (historyFile.exists()) {
                historyFile.readText()
            } else {
                "[]"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read frame history: ${e.message}", e)
            null
        }
    }

    /**
     * Get export directory path (for sharing with user)
     */
    fun getExportPath(): String {
        return getExportDirectory().absolutePath
    }

    /**
     * Clear all exported frames
     */
    fun clearExports() {
        try {
            val exportDir = getExportDirectory()
            exportDir.listFiles()?.forEach { file ->
                file.delete()
            }
            Log.i(TAG, "Cleared all exports")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear exports: ${e.message}", e)
        }
    }

    private fun getExportDirectory(): File {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            EXPORT_FOLDER
        )
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun addToHistory(frameJson: JSONObject) {
        try {
            val exportDir = getExportDirectory()
            val historyFile = File(exportDir, HISTORY_FILE)

            // Read existing history
            val historyArray = if (historyFile.exists()) {
                org.json.JSONArray(historyFile.readText())
            } else {
                org.json.JSONArray()
            }

            // Add new frame
            historyArray.put(frameJson)

            // Keep only last MAX_HISTORY frames
            if (historyArray.length() > MAX_HISTORY) {
                val trimmed = org.json.JSONArray()
                for (i in (historyArray.length() - MAX_HISTORY) until historyArray.length()) {
                    trimmed.put(historyArray.get(i))
                }
                historyFile.writeText(trimmed.toString(2))
            } else {
                historyFile.writeText(historyArray.toString(2))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to update history: ${e.message}", e)
        }
    }

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

    private fun bitmapToBase64Png(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
