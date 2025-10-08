package com.flam

import android.graphics.ImageFormat
import android.media.Image
import android.util.Log
import java.nio.ByteBuffer

/**
 * High-performance YUV to RGBA converter
 * Optimized for Camera2 API YUV_420_888 format
 */
object YuvConverter {

    private const val TAG = "YuvConverter"

    /**
     * Convert YUV_420_888 Image to RGBA byte array
     * Optimized for performance to maintain 15+ FPS
     *
     * @param image YUV_420_888 format image from Camera2 API
     * @return RGBA byte array, or null on error
     */
    fun yuv420ToRgba(image: Image): ByteArray? {
        if (image.format != ImageFormat.YUV_420_888) {
            Log.e(TAG, "Invalid image format: ${image.format}")
            return null
        }

        val width = image.width
        val height = image.height
        val planes = image.planes

        if (planes.size != 3) {
            Log.e(TAG, "Invalid plane count: ${planes.size}")
            return null
        }

        // Get Y, U, V planes
        val yPlane = planes[0]
        val uPlane = planes[1]
        val vPlane = planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        // Create output RGBA array
        val rgbaData = ByteArray(width * height * 4)

        // Get strides and pixel strides
        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride
        val uvRowStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride

        // Convert YUV to RGBA
        var rgbaIndex = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                // Get Y value
                val yIndex = y * yRowStride + x * yPixelStride
                val yValue = (yBuffer.get(yIndex).toInt() and 0xFF)

                // Get U and V values (subsampled at 2x2)
                val uvY = y / 2
                val uvX = x / 2
                val uvIndex = uvY * uvRowStride + uvX * uvPixelStride

                val uValue = (uBuffer.get(uvIndex).toInt() and 0xFF) - 128
                val vValue = (vBuffer.get(uvIndex).toInt() and 0xFF) - 128

                // YUV to RGB conversion (ITU-R BT.601 standard)
                val r = (yValue + 1.402 * vValue).toInt()
                val g = (yValue - 0.344 * uValue - 0.714 * vValue).toInt()
                val b = (yValue + 1.772 * uValue).toInt()

                // Clamp values to [0, 255]
                rgbaData[rgbaIndex++] = r.coerceIn(0, 255).toByte()
                rgbaData[rgbaIndex++] = g.coerceIn(0, 255).toByte()
                rgbaData[rgbaIndex++] = b.coerceIn(0, 255).toByte()
                rgbaData[rgbaIndex++] = 0xFF.toByte() // Alpha
            }
        }

        return rgbaData
    }

    /**
     * Native YUV to RGBA conversion (faster, uses NEON if available)
     * Falls back to Kotlin implementation if native fails
     */
    fun yuv420ToRgbaNative(image: Image): ByteArray? {
        try {
            val width = image.width
            val height = image.height
            val planes = image.planes

            if (planes.size != 3) {
                return yuv420ToRgba(image)
            }

            // Extract plane data
            val yPlane = planes[0]
            val uPlane = planes[1]
            val vPlane = planes[2]

            val yBuffer = yPlane.buffer
            val uBuffer = uPlane.buffer
            val vBuffer = vPlane.buffer

            val yData = ByteArray(yBuffer.remaining())
            val uData = ByteArray(uBuffer.remaining())
            val vData = ByteArray(vBuffer.remaining())

            yBuffer.get(yData)
            uBuffer.get(uData)
            vBuffer.get(vData)

            // Rewind buffers for potential reuse
            yBuffer.rewind()
            uBuffer.rewind()
            vBuffer.rewind()

            val output = ByteArray(width * height * 4)

            val success = nativeYuv420ToRgba(
                yData, uData, vData,
                width, height,
                yPlane.rowStride, yPlane.pixelStride,
                uPlane.rowStride, uPlane.pixelStride,
                output
            )

            return if (success) output else yuv420ToRgba(image)

        } catch (e: Exception) {
            Log.e(TAG, "Native conversion failed: ${e.message}")
            return yuv420ToRgba(image)
        }
    }

    /**
     * Fast conversion using direct buffer access (no intermediate copies)
     * Best performance for real-time processing
     */
    fun yuv420ToRgbaFast(image: Image, outputBuffer: ByteArray): Boolean {
        if (image.format != ImageFormat.YUV_420_888) {
            return false
        }

        val width = image.width
        val height = image.height

        if (outputBuffer.size < width * height * 4) {
            Log.e(TAG, "Output buffer too small")
            return false
        }

        val planes = image.planes
        if (planes.size != 3) {
            return false
        }

        val yPlane = planes[0]
        val uPlane = planes[1]
        val vPlane = planes[2]

        val yBuffer = yPlane.buffer
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer

        val yRowStride = yPlane.rowStride
        val yPixelStride = yPlane.pixelStride
        val uvRowStride = uPlane.rowStride
        val uvPixelStride = uPlane.pixelStride

        var outputIndex = 0

        for (y in 0 until height) {
            val yRowOffset = y * yRowStride
            val uvRow = y / 2
            val uvRowOffset = uvRow * uvRowStride

            for (x in 0 until width) {
                val yIndex = yRowOffset + x * yPixelStride
                val uvIndex = uvRowOffset + (x / 2) * uvPixelStride

                val yVal = yBuffer.get(yIndex).toInt() and 0xFF
                val uVal = (uBuffer.get(uvIndex).toInt() and 0xFF) - 128
                val vVal = (vBuffer.get(uvIndex).toInt() and 0xFF) - 128

                // Fast integer-only conversion
                val r = yVal + ((1436 * vVal) shr 10)
                val g = yVal - ((352 * uVal + 731 * vVal) shr 10)
                val b = yVal + ((1815 * uVal) shr 10)

                outputBuffer[outputIndex++] = r.coerceIn(0, 255).toByte()
                outputBuffer[outputIndex++] = g.coerceIn(0, 255).toByte()
                outputBuffer[outputIndex++] = b.coerceIn(0, 255).toByte()
                outputBuffer[outputIndex++] = 0xFF.toByte()
            }
        }

        return true
    }

    // Native method declaration (implementation optional, falls back to Kotlin)
    private external fun nativeYuv420ToRgba(
        yData: ByteArray,
        uData: ByteArray,
        vData: ByteArray,
        width: Int,
        height: Int,
        yRowStride: Int,
        yPixelStride: Int,
        uvRowStride: Int,
        uvPixelStride: Int,
        output: ByteArray
    ): Boolean
}
