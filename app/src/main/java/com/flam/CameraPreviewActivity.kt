package com.flam

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Demo activity showing real-time camera preview with OpenGL rendering
 * Demonstrates complete pipeline: Camera2 -> YUV->RGBA -> OpenGL texture @ 15+ FPS
 */
class CameraPreviewActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CameraPreviewActivity"
        private const val REQUEST_CAMERA_PERMISSION = 1
    }

    private lateinit var textureView: TextureView
    private lateinit var fpsText: TextView
    private lateinit var cameraManager: CameraManager
    private lateinit var imageProcessor: ImageProcessor

    private var isProcessingEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_preview)

        textureView = findViewById(R.id.cameraTextureView)
        fpsText = findViewById(R.id.cameraFpsText)

        // Initialize camera manager
        cameraManager = CameraManager(this)
        cameraManager.initialize()

        // Initialize image processor
        imageProcessor = ImageProcessor()

        // Check and request camera permission
        if (checkCameraPermission()) {
            startCameraPreview()
        } else {
            requestCameraPermission()
        }

        // Update FPS display
        startFpsUpdater()
    }

    private fun startCameraPreview() {
        try {
            cameraManager.startCamera { rgbaData, width, height ->
                // Option 1: Direct rendering (fastest)
                textureView.updateTexture(rgbaData, width, height)

                // Option 2: Process with OpenCV before rendering (slower but adds edge detection)
                if (isProcessingEnabled) {
                    processAndRender(rgbaData, width, height)
                }
            }

            Log.i(TAG, "Camera preview started")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start camera: ${e.message}")
            Toast.makeText(this, "Failed to start camera: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun processAndRender(inputData: ByteArray, width: Int, height: Int) {
        // Allocate output buffer
        val outputData = ByteArray(width * height * 4)

        // Process frame with Canny edge detection
        val success = imageProcessor.processFrameDirect(
            width, height,
            inputData, outputData,
            50, 150  // Canny thresholds
        )

        if (success) {
            // Render processed frame
            textureView.updateTexture(outputData, width, height)
        }
    }

    private fun startFpsUpdater() {
        val handler = android.os.Handler(mainLooper)
        val updateRunnable = object : Runnable {
            override fun run() {
                val fps = cameraManager.getCurrentFps()
                fpsText.text = "Camera FPS: %.1f".format(fps)

                handler.postDelayed(this, 500)  // Update every 500ms
            }
        }
        handler.post(updateRunnable)
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
                    startCameraPreview()
                } else {
                    Log.e(TAG, "Camera permission denied")
                    Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        textureView.onResume()
    }

    override fun onPause() {
        super.onPause()
        textureView.onPause()
        cameraManager.stopCamera()
    }

    override fun onDestroy() {
        super.onDestroy()
        textureView.cleanup()
        imageProcessor.cleanup()
    }
}
