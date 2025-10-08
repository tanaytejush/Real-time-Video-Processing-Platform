package com.flam

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Custom view for drawing performance overlay on top of camera preview
 * Shows real-time FPS graph and timing metrics
 */
class PerformanceOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 24f
        isFakeBoldText = true
    }

    private val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 18f
    }

    // FPS history for graph (last 100 frames)
    private val fpsHistory = FloatArray(100) { 0f }
    private var historyIndex = 0

    // Current metrics
    private var currentFps = 0.0
    private var avgFrameTime = 0.0
    private var minFrameTime = 0L
    private var maxFrameTime = 0L
    private var opencvTime = 0L
    private var glTime = 0L

    /**
     * Update performance metrics
     */
    fun updateMetrics(
        fps: Double,
        avgTime: Double,
        minTime: Long,
        maxTime: Long,
        cvTime: Long,
        glRenderTime: Long
    ) {
        currentFps = fps
        avgFrameTime = avgTime
        minFrameTime = minTime
        maxFrameTime = maxTime
        opencvTime = cvTime
        glTime = glRenderTime

        // Update FPS history for graph
        fpsHistory[historyIndex] = fps.toFloat()
        historyIndex = (historyIndex + 1) % fpsHistory.size

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        // Draw semi-transparent background
        paint.color = Color.argb(180, 0, 0, 0)
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, width, height, paint)

        // Draw FPS text
        var y = 40f
        textPaint.color = getFpsColor(currentFps)
        canvas.drawText("%.1f FPS".format(currentFps), 20f, y, textPaint)

        // Draw timing information
        y += 35f
        smallTextPaint.color = Color.WHITE
        canvas.drawText("Frame: %.1fms (%.0f-%.0fms)".format(
            avgFrameTime, minFrameTime.toDouble(), maxFrameTime.toDouble()
        ), 20f, y, smallTextPaint)

        if (opencvTime > 0 || glTime > 0) {
            y += 25f
            canvas.drawText("OpenCV: ${opencvTime}ms | GL: ${glTime}ms", 20f, y, smallTextPaint)
        }

        // Draw FPS graph
        if (width > 200 && height > 150) {
            drawFpsGraph(canvas, 20f, height - 120f, width - 40f, 100f)
        }
    }

    private fun drawFpsGraph(canvas: Canvas, x: Float, y: Float, width: Float, height: Float) {
        // Draw graph background
        paint.color = Color.argb(100, 255, 255, 255)
        paint.style = Paint.Style.FILL
        canvas.drawRect(x, y, x + width, y + height, paint)

        // Draw graph border
        paint.color = Color.WHITE
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRect(x, y, x + width, y + height, paint)

        // Draw 30 FPS reference line
        val fps30Y = y + height - (30f / 60f * height)
        paint.color = Color.YELLOW
        paint.strokeWidth = 1f
        canvas.drawLine(x, fps30Y, x + width, fps30Y, paint)

        // Draw FPS history
        paint.color = Color.GREEN
        paint.strokeWidth = 2f

        val barWidth = width / fpsHistory.size
        for (i in fpsHistory.indices) {
            val fps = fpsHistory[(historyIndex + i) % fpsHistory.size]
            if (fps > 0) {
                val barHeight = (fps / 60f * height).coerceAtMost(height)
                val barX = x + i * barWidth
                val barY = y + height - barHeight

                paint.color = getFpsColor(fps.toDouble())
                canvas.drawLine(barX, y + height, barX, barY, paint)
            }
        }

        // Draw FPS labels
        smallTextPaint.color = Color.WHITE
        smallTextPaint.textSize = 14f
        canvas.drawText("60", x - 20f, y + 10f, smallTextPaint)
        canvas.drawText("30", x - 20f, fps30Y + 5f, smallTextPaint)
        canvas.drawText("0", x - 15f, y + height, smallTextPaint)
    }

    private fun getFpsColor(fps: Double): Int {
        return when {
            fps >= 25.0 -> Color.GREEN
            fps >= 15.0 -> Color.YELLOW
            else -> Color.RED
        }
    }
}
