package com.flam

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Main activity integrating complete pipeline:
 * Camera2 API -> YUV to RGBA -> JNI Processing -> OpenGL ES 2.0 Rendering
 *
 * Demonstrates real-time video processing with Canny edge detection at 15+ FPS
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CAMERA_PERMISSION = 1

        init {
            System.loadLibrary("flam")
        }
    }

    // UI Components
    private lateinit var textureView: TextureView
    private lateinit var statusText: TextView
    private lateinit var fpsText: TextView
    private lateinit var performanceOverlay: PerformanceOverlay
    private lateinit var processingToggle: Switch
    private lateinit var saveFrameButton: Button
    private lateinit var exportFrameButton: Button
    private lateinit var lowThresholdSeekBar: SeekBar
    private lateinit var highThresholdSeekBar: SeekBar
    private lateinit var lowThresholdLabel: TextView
    private lateinit var highThresholdLabel: TextView
    private lateinit var controlPanel: LinearLayout

    // Processing pipeline components
    private lateinit var cameraManager: CameraManager
    private lateinit var imageProcessor: ImageProcessor
    private lateinit var frameSaver: FrameSaver
    private lateinit var frameExporter: FrameExporter

    // Performance monitoring
    private lateinit var performanceMonitor: PerformanceMonitor

    // State
    private var isProcessingEnabled = false
    private var lowThreshold = 50
    private var highThreshold = 150

    // Reusable buffer for processed frames
    private var processedBuffer: ByteArray? = null
    private var lastFrameData: ByteArray? = null
    private var lastFrameWidth = 0
    private var lastFrameHeight = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        initializeProcessingPipeline()
        setupControls()

        // Check and request camera permission
        if (checkCameraPermission()) {
            startPipeline()
        } else {
            requestCameraPermission()
        }
    }

    private fun initializeViews() {
        textureView = findViewById(R.id.mainTextureView)
        statusText = findViewById(R.id.statusText)
        fpsText = findViewById(R.id.fpsText)
        performanceOverlay = findViewById(R.id.performanceOverlay)
        processingToggle = findViewById(R.id.processingToggle)
        saveFrameButton = findViewById(R.id.saveFrameButton)
        exportFrameButton = findViewById(R.id.exportFrameButton)
        lowThresholdSeekBar = findViewById(R.id.lowThresholdSeekBar)
        highThresholdSeekBar = findViewById(R.id.highThresholdSeekBar)
        lowThresholdLabel = findViewById(R.id.lowThresholdLabel)
        highThresholdLabel = findViewById(R.id.highThresholdLabel)
        controlPanel = findViewById(R.id.controlPanel)

        // Set continuous rendering for real-time preview
        textureView.setContinuousRendering()

        Log.i(TAG, "Views initialized")
    }

    private fun initializeProcessingPipeline() {
        try {
            // Initialize performance monitor
            performanceMonitor = PerformanceMonitor(TAG)

            // Initialize camera manager
            cameraManager = CameraManager(this)
            cameraManager.initialize()

            // Initialize image processor
            imageProcessor = ImageProcessor()

            // Initialize frame saver
            frameSaver = FrameSaver(this)

            // Initialize frame exporter for web viewer
            frameExporter = FrameExporter(this)

            updateStatus("Pipeline initialized", true)
            Log.i(TAG, "Processing pipeline initialized")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize pipeline: ${e.message}", e)
            updateStatus("Initialization failed: ${e.message}", false)
        }
    }

    private fun setupControls() {
        // Processing toggle
        processingToggle.setOnCheckedChangeListener { _, isChecked ->
            isProcessingEnabled = isChecked

            // Show/hide threshold controls
            lowThresholdSeekBar.visibility = if (isChecked) View.VISIBLE else View.GONE
            highThresholdSeekBar.visibility = if (isChecked) View.VISIBLE else View.GONE
            lowThresholdLabel.visibility = if (isChecked) View.VISIBLE else View.GONE
            highThresholdLabel.visibility = if (isChecked) View.VISIBLE else View.GONE

            updateStatus(if (isChecked) "Processing: ON" else "Processing: OFF", true)
            Log.d(TAG, "Processing enabled: $isChecked")
        }

        // Low threshold control
        lowThresholdSeekBar.max = 200
        lowThresholdSeekBar.progress = lowThreshold
        updateLowThresholdLabel()

        lowThresholdSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                lowThreshold = progress
                updateLowThresholdLabel()

                // Ensure low < high
                if (lowThreshold >= highThreshold) {
                    highThreshold = lowThreshold + 10
                    highThresholdSeekBar.progress = highThreshold
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // High threshold control
        highThresholdSeekBar.max = 300
        highThresholdSeekBar.progress = highThreshold
        updateHighThresholdLabel()

        highThresholdSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                highThreshold = progress
                updateHighThresholdLabel()

                // Ensure high > low
                if (highThreshold <= lowThreshold) {
                    lowThreshold = highThreshold - 10
                    lowThresholdSeekBar.progress = lowThreshold
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Initially hide threshold controls
        lowThresholdSeekBar.visibility = View.GONE
        highThresholdSeekBar.visibility = View.GONE
        lowThresholdLabel.visibility = View.GONE
        highThresholdLabel.visibility = View.GONE

        // Save frame button
        saveFrameButton.setOnClickListener {
            saveCurrentFrame()
        }
    }

    private fun startPipeline() {
        try {
            // Allocate processing buffer (1280x720 RGBA)
            processedBuffer = ByteArray(1280 * 720 * 4)

            // Start camera with frame callback
            cameraManager.startCamera { rgbaData, width, height ->
                processFrame(rgbaData, width, height)
            }

            updateStatus("Camera started - ${1280}x${720}", true)
            startFpsUpdater()

            Log.i(TAG, "Pipeline started successfully")

        } catch (e: SecurityException) {
            Log.e(TAG, "Camera permission denied", e)
            updateStatus("Camera permission required", false)
            requestCameraPermission()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start pipeline: ${e.message}", e)
            updateStatus("Failed to start: ${e.message}", false)
        }
    }

    private fun processFrame(inputData: ByteArray, width: Int, height: Int) {
        // Start frame timing
        val frameStartTime = performanceMonitor.startFrame()

        try {
            if (isProcessingEnabled) {
                // Process with Canny edge detection
                val outputBuffer = processedBuffer ?: return

                // Time OpenCV processing
                val processingStartTime = System.nanoTime()
                val success = imageProcessor.processFrameDirect(
                    width, height,
                    inputData, outputBuffer,
                    lowThreshold, highThreshold
                )
                val processingTime = (System.nanoTime() - processingStartTime) / 1_000_000
                performanceMonitor.recordOpenCVProcessing(processingTime)

                if (success) {
                    // Time GL render
                    val renderStartTime = System.nanoTime()
                    textureView.updateTexture(outputBuffer, width, height)
                    val renderTime = (System.nanoTime() - renderStartTime) / 1_000_000
                    performanceMonitor.recordGLRender(renderTime)

                    // Store last processed frame for saving
                    storeLastFrame(outputBuffer, width, height)
                } else {
                    // Fallback to raw camera feed
                    performanceMonitor.recordDroppedFrame()
                    textureView.updateTexture(inputData, width, height)
                    storeLastFrame(inputData, width, height)
                }
            } else {
                // Direct camera feed (no processing)
                val renderStartTime = System.nanoTime()
                textureView.updateTexture(inputData, width, height)
                val renderTime = (System.nanoTime() - renderStartTime) / 1_000_000
                performanceMonitor.recordGLRender(renderTime)

                // Store last frame (raw camera)
                storeLastFrame(inputData, width, height)
            }

            // End frame timing and calculate FPS
            performanceMonitor.endFrame(frameStartTime)

        } catch (e: Exception) {
            Log.e(TAG, "Frame processing error: ${e.message}", e)
            performanceMonitor.recordDroppedFrame()
        }
    }

    private fun storeLastFrame(frameData: ByteArray, width: Int, height: Int) {
        // Store copy of frame data for saving
        if (lastFrameData == null || lastFrameData!!.size != frameData.size) {
            lastFrameData = ByteArray(frameData.size)
        }
        System.arraycopy(frameData, 0, lastFrameData!!, 0, frameData.size)
        lastFrameWidth = width
        lastFrameHeight = height
    }

    private fun saveCurrentFrame() {
        val frameData = lastFrameData
        if (frameData == null || lastFrameWidth == 0 || lastFrameHeight == 0) {
            Toast.makeText(this, "No frame available to save", Toast.LENGTH_SHORT).show()
            Log.w(TAG, "No frame data available")
            return
        }

        // Save frame in background thread
        Thread {
            try {
                val cameraFps = cameraManager.getCurrentFps()
                val summary = performanceMonitor.getPerformanceSummary()

                // Build metadata
                val metadata = mapOf(
                    "fps" to cameraFps,
                    "resolution" to "${lastFrameWidth}x${lastFrameHeight}",
                    "processing_time_ms" to summary.avgProcessingTime,
                    "opencv_time_ms" to summary.opencvProcessingTime,
                    "gl_time_ms" to summary.glRenderTime,
                    "edge_detection_enabled" to isProcessingEnabled,
                    "low_threshold" to lowThreshold,
                    "high_threshold" to highThreshold,
                    "device" to "${Build.MANUFACTURER} ${Build.MODEL}",
                    "android_version" to Build.VERSION.SDK_INT
                )

                val uri = frameSaver.saveFrameWithMetadata(
                    frameData,
                    lastFrameWidth,
                    lastFrameHeight,
                    metadata
                )

                runOnUiThread {
                    if (uri != null) {
                        val path = frameSaver.getSavedFramesPath()
                        Toast.makeText(
                            this,
                            "Frame saved to Downloads/$path",
                            Toast.LENGTH_LONG
                        ).show()
                        Log.i(TAG, "Frame saved successfully: $uri")
                    } else {
                        Toast.makeText(this, "Failed to save frame", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Failed to save frame")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error saving frame: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun startFpsUpdater() {
        val handler = android.os.Handler(mainLooper)
        val updateRunnable = object : Runnable {
            override fun run() {
                if (!isFinishing) {
                    updatePerformanceDisplay()
                    handler.postDelayed(this, 500)
                }
            }
        }
        handler.post(updateRunnable)
    }

    private fun updatePerformanceDisplay() {
        val cameraFps = cameraManager.getCurrentFps()
        val summary = performanceMonitor.getPerformanceSummary()

        // Update performance overlay with real-time graph
        performanceOverlay.updateMetrics(
            fps = summary.fps,
            avgTime = summary.avgProcessingTime,
            minTime = summary.minProcessingTime,
            maxTime = summary.maxProcessingTime,
            cvTime = summary.opencvProcessingTime,
            glRenderTime = summary.glRenderTime
        )

        // Update FPS text with detailed timing
        val fpsDisplay = buildString {
            append("Camera: %.1f FPS | Pipeline: %.1f FPS\n".format(cameraFps, summary.fps))
            append("Frame: %.1fms (%.1f-%.1fms)".format(
                summary.avgProcessingTime,
                summary.minProcessingTime.toDouble(),
                summary.maxProcessingTime.toDouble()
            ))

            if (isProcessingEnabled && summary.opencvProcessingTime > 0) {
                append("\nOpenCV: %dms | GL: %dms".format(
                    summary.opencvProcessingTime,
                    summary.glRenderTime
                ))
            }

            if (summary.droppedFrames > 0 || summary.slowFrames > 0) {
                append("\n⚠ Dropped: %d | Slow: %d".format(
                    summary.droppedFrames,
                    summary.slowFrames
                ))
            }
        }

        fpsText.text = fpsDisplay

        // Log detailed performance to Logcat every 2 seconds
        if (System.currentTimeMillis() % 2000 < 500) {
            Log.i(TAG, "=== Performance Metrics ===")
            Log.i(TAG, "Camera FPS: %.1f".format(cameraFps))
            Log.i(TAG, "Pipeline FPS: %.1f".format(summary.fps))
            Log.i(TAG, "Frame time: Avg=%.1fms, Min=%dms, Max=%dms".format(
                summary.avgProcessingTime, summary.minProcessingTime, summary.maxProcessingTime
            ))
            if (isProcessingEnabled) {
                Log.i(TAG, "Stage timings: OpenCV=%dms, GL=%dms".format(
                    summary.opencvProcessingTime, summary.glRenderTime
                ))
            }
            if (summary.droppedFrames > 0 || summary.slowFrames > 0) {
                Log.w(TAG, "Issues: Dropped=%d, Slow=%d".format(
                    summary.droppedFrames, summary.slowFrames
                ))
            }
            Log.i(TAG, "==========================")
        }
    }

    private fun updateStatus(message: String, isSuccess: Boolean) {
        runOnUiThread {
            statusText.text = message
            statusText.setTextColor(
                if (isSuccess)
                    ContextCompat.getColor(this, android.R.color.holo_green_light)
                else
                    ContextCompat.getColor(this, android.R.color.holo_red_light)
            )
        }
    }

    private fun updateLowThresholdLabel() {
        lowThresholdLabel.text = "Low Threshold: $lowThreshold"
    }

    private fun updateHighThresholdLabel() {
        highThresholdLabel.text = "High Threshold: $highThreshold"
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            REQUEST_CAMERA_PERMISSION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "Camera permission granted")
                    startPipeline()
                } else {
                    Log.e(TAG, "Camera permission denied")
                    updateStatus("Camera permission denied", false)
                    Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        textureView.onResume()
        Log.d(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        textureView.onPause()

        // Stop camera to save resources
        if (::cameraManager.isInitialized) {
            cameraManager.stopCamera()
        }

        Log.d(TAG, "onPause - camera stopped")
    }

    override fun onDestroy() {
        super.onDestroy()

        // Clean up all resources
        if (::cameraManager.isInitialized) {
            cameraManager.stopCamera()
        }

        if (::imageProcessor.isInitialized) {
            imageProcessor.cleanup()
        }

        textureView.cleanup()

        // Clear buffers
        processedBuffer = null

        Log.i(TAG, "onDestroy - all resources cleaned up")
    }
}
