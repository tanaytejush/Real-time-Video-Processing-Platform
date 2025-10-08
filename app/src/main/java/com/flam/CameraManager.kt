package com.flam

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.core.content.ContextCompat
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * Camera2 API manager optimized for 15+ FPS frame capture
 * Captures YUV_420_888 frames, converts to RGBA, and passes to processing pipeline
 */
class CameraManager(private val context: Context) {

    companion object {
        private const val TAG = "CameraManager"

        // Camera configuration for 15+ FPS performance
        private const val TARGET_FPS = 30
        private const val IMAGE_WIDTH = 1280
        private const val IMAGE_HEIGHT = 720
        private const val MAX_IMAGES = 2  // Double buffering
    }

    // Camera2 API components
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null

    // Background thread for camera operations
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    // Camera state
    private var cameraId: String = ""
    private var isRunning = false
    private val cameraOpenCloseLock = Semaphore(1)

    // Frame callback
    private var frameCallback: ((ByteArray, Int, Int) -> Unit)? = null

    // Performance tracking
    private var frameCount = 0L
    private var lastFpsTime = System.currentTimeMillis()
    private var currentFps = 0.0

    // Reusable buffer for conversions (avoids allocations)
    private var rgbaBuffer: ByteArray? = null

    /**
     * Camera device state callback
     */
    private val stateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            cameraOpenCloseLock.release()
            cameraDevice = camera
            Log.i(TAG, "Camera opened: ${camera.id}")
            createCaptureSession()
        }

        override fun onDisconnected(camera: CameraDevice) {
            cameraOpenCloseLock.release()
            camera.close()
            cameraDevice = null
            Log.i(TAG, "Camera disconnected")
        }

        override fun onError(camera: CameraDevice, error: Int) {
            cameraOpenCloseLock.release()
            camera.close()
            cameraDevice = null
            Log.e(TAG, "Camera error: $error")
        }
    }

    /**
     * Image available callback - processes frames at 15+ FPS
     */
    private val onImageAvailableListener = ImageReader.OnImageAvailableListener { reader ->
        val startTime = System.nanoTime()

        val image = reader.acquireLatestImage() ?: return@OnImageAvailableListener

        try {
            processFrame(image)

            // Track FPS
            frameCount++
            val currentTime = System.currentTimeMillis()
            val elapsed = currentTime - lastFpsTime

            if (elapsed >= 1000) {
                currentFps = frameCount * 1000.0 / elapsed
                Log.d(TAG, "Camera FPS: %.1f".format(currentFps))
                frameCount = 0
                lastFpsTime = currentTime
            }

            val processingTime = (System.nanoTime() - startTime) / 1_000_000
            if (processingTime > 33) {  // Warn if processing takes >33ms (< 30 FPS)
                Log.w(TAG, "Frame processing slow: ${processingTime}ms")
            }

        } finally {
            image.close()
        }
    }

    /**
     * Initialize camera manager
     */
    fun initialize() {
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager

        try {
            // Find back-facing camera
            for (id in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)

                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id

                    // Log camera capabilities
                    val configMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    val outputSizes = configMap?.getOutputSizes(ImageFormat.YUV_420_888)
                    Log.i(TAG, "Camera $id supports YUV sizes: ${outputSizes?.contentToString()}")

                    break
                }
            }

            if (cameraId.isEmpty()) {
                throw RuntimeException("No back-facing camera found")
            }

            Log.i(TAG, "Camera manager initialized with camera: $cameraId")

        } catch (e: CameraAccessException) {
            Log.e(TAG, "Camera access exception: ${e.message}")
            throw e
        }
    }

    /**
     * Start camera capture
     */
    @SuppressLint("MissingPermission")
    fun startCamera(callback: (ByteArray, Int, Int) -> Unit) {
        if (isRunning) {
            Log.w(TAG, "Camera already running")
            return
        }

        // Check camera permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Camera permission not granted")
        }

        frameCallback = callback

        // Start background thread
        startBackgroundThread()

        // Create ImageReader for YUV frames
        imageReader = ImageReader.newInstance(
            IMAGE_WIDTH,
            IMAGE_HEIGHT,
            ImageFormat.YUV_420_888,
            MAX_IMAGES
        ).apply {
            setOnImageAvailableListener(onImageAvailableListener, backgroundHandler)
        }

        // Allocate reusable RGBA buffer
        rgbaBuffer = ByteArray(IMAGE_WIDTH * IMAGE_HEIGHT * 4)

        // Open camera
        val manager = context.getSystemService(Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager

        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Timeout waiting to lock camera opening")
            }

            manager.openCamera(cameraId, stateCallback, backgroundHandler)
            isRunning = true

            Log.i(TAG, "Camera starting: ${IMAGE_WIDTH}x${IMAGE_HEIGHT} @ ${TARGET_FPS} FPS target")

        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to open camera: ${e.message}")
            throw e
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while opening camera", e)
        }
    }

    /**
     * Stop camera capture
     */
    fun stopCamera() {
        if (!isRunning) {
            return
        }

        try {
            cameraOpenCloseLock.acquire()

            captureSession?.close()
            captureSession = null

            cameraDevice?.close()
            cameraDevice = null

            imageReader?.close()
            imageReader = null

            rgbaBuffer = null

        } catch (e: InterruptedException) {
            Log.e(TAG, "Interrupted while stopping camera", e)
        } finally {
            cameraOpenCloseLock.release()
            stopBackgroundThread()
            isRunning = false
            frameCallback = null

            Log.i(TAG, "Camera stopped")
        }
    }

    /**
     * Get current FPS
     */
    fun getCurrentFps(): Double = currentFps

    /**
     * Process captured frame: YUV -> RGBA -> callback
     */
    private fun processFrame(image: Image) {
        val width = image.width
        val height = image.height

        // Convert YUV to RGBA using fast conversion
        val buffer = rgbaBuffer ?: return

        val success = YuvConverter.yuv420ToRgbaFast(image, buffer)

        if (success) {
            // Pass RGBA data to callback
            frameCallback?.invoke(buffer, width, height)
        } else {
            Log.e(TAG, "Failed to convert YUV to RGBA")
        }
    }

    /**
     * Create camera capture session
     */
    private fun createCaptureSession() {
        try {
            val device = cameraDevice ?: return
            val reader = imageReader ?: return

            // Create capture request for repeating preview
            val captureRequestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(reader.surface)

                // Set FPS range for target frame rate
                set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    android.util.Range(TARGET_FPS, TARGET_FPS))

                // Fast autofocus for real-time processing
                set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)

                // Auto exposure
                set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON)

                // Auto white balance
                set(CaptureRequest.CONTROL_AWB_MODE,
                    CaptureRequest.CONTROL_AWB_MODE_AUTO)
            }

            // Create capture session
            device.createCaptureSession(
                listOf(reader.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        if (cameraDevice == null) return

                        captureSession = session

                        try {
                            // Start repeating capture for continuous frames
                            session.setRepeatingRequest(
                                captureRequestBuilder.build(),
                                null,
                                backgroundHandler
                            )

                            Log.i(TAG, "Capture session configured and started")

                        } catch (e: CameraAccessException) {
                            Log.e(TAG, "Failed to start capture request: ${e.message}")
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "Capture session configuration failed")
                    }
                },
                backgroundHandler
            )

        } catch (e: CameraAccessException) {
            Log.e(TAG, "Failed to create capture session: ${e.message}")
        }
    }

    /**
     * Start background thread for camera operations
     */
    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    /**
     * Stop background thread
     */
    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "Error stopping background thread", e)
        }
    }

    /**
     * Check if camera permission is granted
     */
    fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
    }
}
