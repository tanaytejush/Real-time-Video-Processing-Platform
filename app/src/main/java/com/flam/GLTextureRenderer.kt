package com.flam

import android.opengl.GLSurfaceView
import android.util.Log
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * OpenGL ES 2.0 texture renderer with JNI bindings
 * Optimized for 60 FPS real-time rendering
 */
class GLTextureRenderer : GLSurfaceView.Renderer {

    companion object {
        private const val TAG = "GLTextureRenderer"

        init {
            System.loadLibrary("flam")
        }
    }

    // Native renderer handle
    private var nativeHandle: Long = 0L

    // State tracking
    private var isInitialized = false
    private var surfaceWidth = 0
    private var surfaceHeight = 0

    // Performance tracking
    private var frameCount = 0L
    private var lastFpsTime = System.currentTimeMillis()

    /**
     * Create native renderer instance
     */
    init {
        nativeHandle = nativeCreate()
        if (nativeHandle == 0L) {
            throw RuntimeException("Failed to create native renderer")
        }
        Log.i(TAG, "Native renderer created: $nativeHandle")
    }

    /**
     * Called when the surface is created or recreated
     */
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.i(TAG, "Surface created")

        if (nativeHandle != 0L) {
            isInitialized = nativeInit(nativeHandle)
            if (!isInitialized) {
                Log.e(TAG, "Failed to initialize native renderer")
            }
        }
    }

    /**
     * Called when the surface changes size
     */
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.i(TAG, "Surface changed: ${width}x${height}")

        surfaceWidth = width
        surfaceHeight = height

        if (nativeHandle != 0L && isInitialized) {
            nativeResize(nativeHandle, width, height)
        }
    }

    /**
     * Called to draw the current frame
     * Target: 60 FPS (16.67ms per frame)
     */
    override fun onDrawFrame(gl: GL10?) {
        if (nativeHandle != 0L && isInitialized) {
            nativeRender(nativeHandle)

            // Track FPS
            frameCount++
            val currentTime = System.currentTimeMillis()
            val elapsed = currentTime - lastFpsTime

            if (elapsed >= 1000) {
                val fps = (frameCount * 1000.0 / elapsed).toInt()
                Log.d(TAG, "FPS: $fps")
                frameCount = 0
                lastFpsTime = currentTime
            }
        }
    }

    /**
     * Update texture from byte array
     * @param data RGBA byte array
     * @param width Texture width
     * @param height Texture height
     * @return true if successful
     */
    fun updateTexture(data: ByteArray, width: Int, height: Int): Boolean {
        if (nativeHandle == 0L || !isInitialized) {
            Log.e(TAG, "Renderer not initialized")
            return false
        }

        if (data.size < width * height * 4) {
            Log.e(TAG, "Invalid data size: ${data.size} < ${width * height * 4}")
            return false
        }

        return nativeUpdateTexture(nativeHandle, data, width, height)
    }

    /**
     * Set brightness multiplier
     * @param brightness Brightness factor (1.0 = normal, 0.0 = black, 2.0 = double bright)
     */
    fun setBrightness(brightness: Float) {
        if (nativeHandle != 0L && isInitialized) {
            nativeSetBrightness(nativeHandle, brightness)
        }
    }

    /**
     * Get current surface dimensions
     */
    fun getSurfaceDimensions(): Pair<Int, Int> = Pair(surfaceWidth, surfaceHeight)

    /**
     * Clean up resources
     * Must be called when renderer is no longer needed
     */
    fun destroy() {
        if (nativeHandle != 0L) {
            nativeDestroy(nativeHandle)
            nativeHandle = 0L
            isInitialized = false
            Log.i(TAG, "Renderer destroyed")
        }
    }

    // Native methods
    private external fun nativeCreate(): Long
    private external fun nativeInit(handle: Long): Boolean
    private external fun nativeUpdateTexture(handle: Long, data: ByteArray, width: Int, height: Int): Boolean
    private external fun nativeRender(handle: Long)
    private external fun nativeResize(handle: Long, width: Int, height: Int)
    private external fun nativeSetBrightness(handle: Long, brightness: Float)
    private external fun nativeDestroy(handle: Long)
}
