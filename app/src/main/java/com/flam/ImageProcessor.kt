package com.flam

import android.graphics.Bitmap
import java.nio.ByteBuffer

/**
 * Image processor with JNI bindings to OpenCV C++ backend
 * Handles RGBA to grayscale conversion and Canny edge detection
 */
class ImageProcessor {

    companion object {
        init {
            System.loadLibrary("flam")
        }

        // Default Canny thresholds
        const val DEFAULT_LOW_THRESHOLD = 50
        const val DEFAULT_HIGH_THRESHOLD = 150
    }

    /**
     * Process RGBA frame with Canny edge detection
     * Returns pointer to processed frame data (managed internally)
     *
     * @param width Frame width
     * @param height Frame height
     * @param rgbaData Input RGBA byte array
     * @param lowThreshold Canny low threshold
     * @param highThreshold Canny high threshold
     * @return Pointer to processed data, or 0 on failure
     */
    external fun processFrameNative(
        width: Int,
        height: Int,
        rgbaData: ByteArray,
        lowThreshold: Int = DEFAULT_LOW_THRESHOLD,
        highThreshold: Int = DEFAULT_HIGH_THRESHOLD
    ): Long

    /**
     * Get processed frame data into output buffer
     *
     * @param outputBuffer Buffer to receive processed frame
     * @return true if successful
     */
    external fun getProcessedFrame(outputBuffer: ByteArray): Boolean

    /**
     * Release processed frame memory
     * Should be called when frame is no longer needed
     */
    external fun releaseFrame()

    /**
     * Get dimensions of processed frame
     *
     * @return IntArray [width, height], or null if no frame
     */
    external fun getFrameDimensions(): IntArray?

    /**
     * Direct processing - input and output in same call
     * More efficient for real-time processing
     *
     * @param width Frame width
     * @param height Frame height
     * @param inputRgba Input RGBA data
     * @param outputRgba Output RGBA data
     * @param lowThreshold Canny low threshold
     * @param highThreshold Canny high threshold
     * @return true if successful
     */
    external fun processFrameDirect(
        width: Int,
        height: Int,
        inputRgba: ByteArray,
        outputRgba: ByteArray,
        lowThreshold: Int = DEFAULT_LOW_THRESHOLD,
        highThreshold: Int = DEFAULT_HIGH_THRESHOLD
    ): Boolean

    /**
     * Kotlin convenience method: Process bitmap with Canny edge detection
     *
     * @param inputBitmap Input bitmap (will be converted to RGBA if needed)
     * @param lowThreshold Canny low threshold
     * @param highThreshold Canny high threshold
     * @return Processed bitmap with edges, or null on failure
     */
    fun processBitmap(
        inputBitmap: Bitmap,
        lowThreshold: Int = DEFAULT_LOW_THRESHOLD,
        highThreshold: Int = DEFAULT_HIGH_THRESHOLD
    ): Bitmap? {
        val width = inputBitmap.width
        val height = inputBitmap.height
        val pixelCount = width * height

        // Convert bitmap to RGBA byte array
        val inputBuffer = ByteArray(pixelCount * 4)
        val outputBuffer = ByteArray(pixelCount * 4)

        val byteBuffer = ByteBuffer.wrap(inputBuffer)
        inputBitmap.copyPixelsToBuffer(byteBuffer)

        // Process frame
        val success = processFrameDirect(
            width, height,
            inputBuffer, outputBuffer,
            lowThreshold, highThreshold
        )

        if (!success) {
            return null
        }

        // Create output bitmap
        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val outBuffer = ByteBuffer.wrap(outputBuffer)
        outputBitmap.copyPixelsFromBuffer(outBuffer)

        return outputBitmap
    }

    /**
     * Cleanup method - call when processor is no longer needed
     */
    fun cleanup() {
        releaseFrame()
    }
}
