package com.flam

import android.util.Log
import java.util.concurrent.atomic.AtomicLong

/**
 * Performance monitoring utility for tracking FPS and frame timing
 * Thread-safe for concurrent access from camera and render threads
 */
class PerformanceMonitor(private val tag: String) {

    // Frame timing metrics
    private var frameCount = AtomicLong(0)
    private var lastFpsTime = System.currentTimeMillis()
    private var currentFps = 0.0

    // Processing time tracking
    private var totalProcessingTime = 0L
    private var minProcessingTime = Long.MAX_VALUE
    private var maxProcessingTime = 0L
    private var avgProcessingTime = 0.0

    // Individual stage timings
    private var yuvConversionTime = 0L
    private var opencvProcessingTime = 0L
    private var glRenderTime = 0L

    // Statistics
    private var droppedFrames = AtomicLong(0)
    private var slowFrames = AtomicLong(0)  // Frames taking >33ms (below 30 FPS)

    companion object {
        private const val FPS_UPDATE_INTERVAL_MS = 1000
        private const val SLOW_FRAME_THRESHOLD_MS = 33  // 30 FPS threshold
    }

    /**
     * Start timing a frame
     */
    fun startFrame(): Long {
        return System.nanoTime()
    }

    /**
     * Record frame completion and calculate FPS
     * @param startTime Frame start time from startFrame()
     * @return Current FPS
     */
    fun endFrame(startTime: Long): Double {
        val endTime = System.nanoTime()
        val processingTime = (endTime - startTime) / 1_000_000  // Convert to ms

        // Update processing time statistics
        updateProcessingTimeStats(processingTime)

        // Track slow frames
        if (processingTime > SLOW_FRAME_THRESHOLD_MS) {
            slowFrames.incrementAndGet()
        }

        // Update frame count and calculate FPS
        frameCount.incrementAndGet()
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - lastFpsTime

        if (elapsed >= FPS_UPDATE_INTERVAL_MS) {
            currentFps = frameCount.get() * 1000.0 / elapsed

            // Log detailed statistics
            logStatistics(processingTime)

            // Reset counters
            frameCount.set(0)
            lastFpsTime = currentTime
        }

        return currentFps
    }

    /**
     * Record stage timing
     */
    fun recordYuvConversion(durationMs: Long) {
        yuvConversionTime = durationMs
    }

    fun recordOpenCVProcessing(durationMs: Long) {
        opencvProcessingTime = durationMs
    }

    fun recordGLRender(durationMs: Long) {
        glRenderTime = durationMs
    }

    /**
     * Record dropped frame
     */
    fun recordDroppedFrame() {
        droppedFrames.incrementAndGet()
    }

    /**
     * Get current FPS
     */
    fun getFps(): Double = currentFps

    /**
     * Get average processing time
     */
    fun getAvgProcessingTime(): Double = avgProcessingTime

    /**
     * Get min/max processing times
     */
    fun getMinProcessingTime(): Long = if (minProcessingTime == Long.MAX_VALUE) 0 else minProcessingTime
    fun getMaxProcessingTime(): Long = maxProcessingTime

    /**
     * Get performance summary
     */
    fun getPerformanceSummary(): PerformanceSummary {
        return PerformanceSummary(
            fps = currentFps,
            avgProcessingTime = avgProcessingTime,
            minProcessingTime = getMinProcessingTime(),
            maxProcessingTime = maxProcessingTime,
            yuvConversionTime = yuvConversionTime,
            opencvProcessingTime = opencvProcessingTime,
            glRenderTime = glRenderTime,
            droppedFrames = droppedFrames.get(),
            slowFrames = slowFrames.get()
        )
    }

    /**
     * Reset all statistics
     */
    fun reset() {
        frameCount.set(0)
        lastFpsTime = System.currentTimeMillis()
        currentFps = 0.0
        totalProcessingTime = 0
        minProcessingTime = Long.MAX_VALUE
        maxProcessingTime = 0
        avgProcessingTime = 0.0
        yuvConversionTime = 0
        opencvProcessingTime = 0
        glRenderTime = 0
        droppedFrames.set(0)
        slowFrames.set(0)

        Log.i(tag, "Performance statistics reset")
    }

    private fun updateProcessingTimeStats(processingTime: Long) {
        totalProcessingTime += processingTime
        minProcessingTime = minOf(minProcessingTime, processingTime)
        maxProcessingTime = maxOf(maxProcessingTime, processingTime)

        val count = frameCount.get() + 1
        avgProcessingTime = totalProcessingTime.toDouble() / count
    }

    private fun logStatistics(currentFrameTime: Long) {
        Log.d(tag, "=== Performance Statistics ===")
        Log.d(tag, "FPS: %.2f".format(currentFps))
        Log.d(tag, "Current frame: ${currentFrameTime}ms")
        Log.d(tag, "Avg processing: %.2fms".format(avgProcessingTime))
        Log.d(tag, "Min processing: ${getMinProcessingTime()}ms")
        Log.d(tag, "Max processing: ${maxProcessingTime}ms")

        if (yuvConversionTime > 0 || opencvProcessingTime > 0 || glRenderTime > 0) {
            Log.d(tag, "--- Stage Timings ---")
            if (yuvConversionTime > 0) Log.d(tag, "YUV conversion: ${yuvConversionTime}ms")
            if (opencvProcessingTime > 0) Log.d(tag, "OpenCV processing: ${opencvProcessingTime}ms")
            if (glRenderTime > 0) Log.d(tag, "GL render: ${glRenderTime}ms")
        }

        val dropped = droppedFrames.get()
        val slow = slowFrames.get()
        if (dropped > 0 || slow > 0) {
            Log.w(tag, "Dropped frames: $dropped | Slow frames: $slow")
        }

        Log.d(tag, "=============================")
    }

    /**
     * Data class for performance summary
     */
    data class PerformanceSummary(
        val fps: Double,
        val avgProcessingTime: Double,
        val minProcessingTime: Long,
        val maxProcessingTime: Long,
        val yuvConversionTime: Long,
        val opencvProcessingTime: Long,
        val glRenderTime: Long,
        val droppedFrames: Long,
        val slowFrames: Long
    ) {
        override fun toString(): String {
            return """
                FPS: %.1f
                Avg: %.1fms (Min: %dms, Max: %dms)
                YUV: %dms | OpenCV: %dms | GL: %dms
                Dropped: %d | Slow: %d
            """.trimIndent().format(
                fps, avgProcessingTime, minProcessingTime, maxProcessingTime,
                yuvConversionTime, opencvProcessingTime, glRenderTime,
                droppedFrames, slowFrames
            )
        }
    }
}
