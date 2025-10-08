package com.flam

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

/**
 * Activity for processing video files with OpenCV edge detection
 */
class VideoProcessingActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "VideoProcessing"
        private const val REQUEST_VIDEO_PICK = 100
        private const val REQUEST_STORAGE_PERMISSION = 101
    }

    // UI Components
    private lateinit var selectVideoButton: Button
    private lateinit var processVideoButton: Button
    private lateinit var extractFrameButton: Button
    private lateinit var videoInfoText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var frameSkipSpinner: Spinner
    private lateinit var lowThresholdSeekBar: SeekBar
    private lateinit var highThresholdSeekBar: SeekBar
    private lateinit var lowThresholdLabel: TextView
    private lateinit var highThresholdLabel: TextView
    private lateinit var frameTimeSeekBar: SeekBar
    private lateinit var frameTimeLabel: TextView
    private lateinit var framePreview: ImageView

    private lateinit var videoProcessor: VideoProcessor
    private var selectedVideoUri: Uri? = null
    private var videoDuration: Long = 0
    private var lowThreshold = 50
    private var highThreshold = 150
    private var frameTimeMs = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_processing)

        initializeViews()
        videoProcessor = VideoProcessor(this)

        setupControls()
        checkStoragePermission()
    }

    private fun initializeViews() {
        selectVideoButton = findViewById(R.id.selectVideoButton)
        processVideoButton = findViewById(R.id.processVideoButton)
        extractFrameButton = findViewById(R.id.extractFrameButton)
        videoInfoText = findViewById(R.id.videoInfoText)
        progressBar = findViewById(R.id.progressBar)
        progressText = findViewById(R.id.progressText)
        frameSkipSpinner = findViewById(R.id.frameSkipSpinner)
        lowThresholdSeekBar = findViewById(R.id.lowThresholdSeekBar)
        highThresholdSeekBar = findViewById(R.id.highThresholdSeekBar)
        lowThresholdLabel = findViewById(R.id.lowThresholdLabel)
        highThresholdLabel = findViewById(R.id.highThresholdLabel)
        frameTimeSeekBar = findViewById(R.id.frameTimeSeekBar)
        frameTimeLabel = findViewById(R.id.frameTimeLabel)
        framePreview = findViewById(R.id.framePreview)

        processVideoButton.isEnabled = false
        extractFrameButton.isEnabled = false
    }

    private fun setupControls() {
        // Select video button
        selectVideoButton.setOnClickListener {
            openVideoPicker()
        }

        // Frame skip spinner
        val frameSkipOptions = arrayOf("All frames (1)", "Every 2nd (2)", "Every 5th (5)", "Every 10th (10)", "Every 30th (30)")
        frameSkipSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, frameSkipOptions).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // Threshold controls
        lowThresholdSeekBar.max = 200
        lowThresholdSeekBar.progress = lowThreshold
        updateLowThresholdLabel()

        lowThresholdSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                lowThreshold = progress
                updateLowThresholdLabel()
                if (lowThreshold >= highThreshold) {
                    highThreshold = lowThreshold + 10
                    highThresholdSeekBar.progress = highThreshold
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        highThresholdSeekBar.max = 300
        highThresholdSeekBar.progress = highThreshold
        updateHighThresholdLabel()

        highThresholdSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                highThreshold = progress
                updateHighThresholdLabel()
                if (highThreshold <= lowThreshold) {
                    lowThreshold = highThreshold - 10
                    lowThresholdSeekBar.progress = lowThreshold
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Frame time seekbar
        frameTimeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                frameTimeMs = (progress / 100.0 * videoDuration).toLong()
                updateFrameTimeLabel()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Preview frame at this time
                selectedVideoUri?.let { uri ->
                    previewFrame(uri, frameTimeMs)
                }
            }
        })

        // Process video button
        processVideoButton.setOnClickListener {
            selectedVideoUri?.let { uri ->
                processVideo(uri)
            }
        }

        // Extract single frame button
        extractFrameButton.setOnClickListener {
            selectedVideoUri?.let { uri ->
                extractAndSaveFrame(uri, frameTimeMs)
            }
        }
    }

    private fun openVideoPicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        intent.type = "video/*"
        startActivityForResult(intent, REQUEST_VIDEO_PICK)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_VIDEO_PICK && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                selectedVideoUri = uri
                displayVideoInfo(uri)
                processVideoButton.isEnabled = true
                extractFrameButton.isEnabled = true
            }
        }
    }

    private fun displayVideoInfo(uri: Uri) {
        val info = videoProcessor.getVideoInfo(uri)

        if (info != null) {
            videoDuration = info.duration
            frameTimeSeekBar.max = 100  // 0-100% of video

            val infoText = buildString {
                append("📹 Video Information\n\n")
                append("Resolution: ${info.width} × ${info.height}\n")
                append("Duration: ${info.duration / 1000}s\n")
                append("Frame Rate: ${info.frameRate} FPS\n")
                append("Total Frames: ${info.totalFrames}\n")
                append("Format: ${info.mimeType}\n")
            }

            videoInfoText.text = infoText
            Log.i(TAG, "Video loaded: $infoText")

            // Preview first frame
            previewFrame(uri, 0)

        } else {
            videoInfoText.text = "Failed to load video info"
        }
    }

    private fun previewFrame(uri: Uri, timeMs: Long) {
        Thread {
            val bitmap = videoProcessor.extractFrame(uri, timeMs)
            runOnUiThread {
                if (bitmap != null) {
                    framePreview.setImageBitmap(bitmap)
                } else {
                    Toast.makeText(this, "Failed to extract frame", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun processVideo(uri: Uri) {
        // Get frame skip value
        val frameSkipIndex = frameSkipSpinner.selectedItemPosition
        val frameSkip = when (frameSkipIndex) {
            0 -> 1
            1 -> 2
            2 -> 5
            3 -> 10
            4 -> 30
            else -> 1
        }

        // Create output directory
        val outputDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Flam/Videos/${System.currentTimeMillis()}"
        )

        progressBar.progress = 0
        progressText.text = "Starting..."
        processVideoButton.isEnabled = false

        // Process in background thread
        Thread {
            try {
                val frameCount = videoProcessor.processVideo(
                    videoUri = uri,
                    outputDir = outputDir,
                    lowThreshold = lowThreshold,
                    highThreshold = highThreshold,
                    frameSkip = frameSkip
                ) { progress ->
                    // Update UI on main thread
                    runOnUiThread {
                        progressBar.progress = progress.progressPercent.toInt()
                        progressText.text = buildString {
                            append("Frame ${progress.currentFrame}/${progress.totalFrames}\n")
                            append("${String.format("%.1f", progress.progressPercent)}% complete\n")
                            append("Processing: ${String.format("%.1f", progress.fps)} FPS")
                        }
                    }
                }

                runOnUiThread {
                    progressBar.progress = 100
                    progressText.text = "✅ Complete! Processed $frameCount frames\n" +
                            "Saved to: ${outputDir.absolutePath}"
                    processVideoButton.isEnabled = true

                    Toast.makeText(
                        this,
                        "Video processed: $frameCount frames saved",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error processing video", e)
                runOnUiThread {
                    progressText.text = "❌ Error: ${e.message}"
                    processVideoButton.isEnabled = true
                    Toast.makeText(this, "Processing failed", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun extractAndSaveFrame(uri: Uri, timeMs: Long) {
        Thread {
            val bitmap = videoProcessor.processSingleFrame(uri, timeMs, lowThreshold, highThreshold)

            runOnUiThread {
                if (bitmap != null) {
                    framePreview.setImageBitmap(bitmap)

                    // Save to downloads
                    val frameSaver = FrameSaver(this)
                    val rgbaData = ByteArray(bitmap.width * bitmap.height * 4)
                    val buffer = java.nio.ByteBuffer.allocate(bitmap.width * bitmap.height * 4)
                    bitmap.copyPixelsToBuffer(buffer)
                    System.arraycopy(buffer.array(), 0, rgbaData, 0, rgbaData.size)

                    val (savedUri, _) = frameSaver.saveFrame(rgbaData, bitmap.width, bitmap.height)

                    bitmap.recycle()

                    if (savedUri != null) {
                        Toast.makeText(
                            this,
                            "Frame saved to Downloads/Flam",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(this, "Failed to process frame", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun updateLowThresholdLabel() {
        lowThresholdLabel.text = "Low Threshold: $lowThreshold"
    }

    private fun updateHighThresholdLabel() {
        highThresholdLabel.text = "High Threshold: $highThreshold"
    }

    private fun updateFrameTimeLabel() {
        val seconds = frameTimeMs / 1000.0
        frameTimeLabel.text = "Frame Time: ${String.format("%.2f", seconds)}s"
    }

    private fun checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_STORAGE_PERMISSION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Storage permission granted")
            } else {
                Toast.makeText(this, "Storage permission required", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        videoProcessor.cleanup()
    }
}
