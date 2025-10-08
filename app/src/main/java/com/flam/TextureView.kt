package com.flam

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log

/**
 * GLSurfaceView wrapper optimized for 60 FPS texture rendering
 * Provides simple interface for displaying processed frames
 */
class TextureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GLSurfaceView(context, attrs) {

    companion object {
        private const val TAG = "TextureView"
    }

    val renderer: GLTextureRenderer

    init {
        // Configure for OpenGL ES 2.0
        setEGLContextClientVersion(2)

        // Create renderer
        renderer = GLTextureRenderer()
        setRenderer(renderer)

        // Set render mode for optimal performance
        // RENDERMODE_CONTINUOUSLY = render as fast as possible (targets 60 FPS)
        // RENDERMODE_WHEN_DIRTY = render only when requested (for static content)
        renderMode = RENDERMODE_CONTINUOUSLY

        Log.i(TAG, "TextureView initialized with 60 FPS target")
    }

    /**
     * Update displayed texture from byte array
     * Thread-safe - can be called from any thread
     *
     * @param data RGBA byte array
     * @param width Texture width
     * @param height Texture height
     */
    fun updateTexture(data: ByteArray, width: Int, height: Int) {
        queueEvent {
            renderer.updateTexture(data, width, height)
        }
    }

    /**
     * Set brightness multiplier
     * @param brightness Brightness factor (1.0 = normal)
     */
    fun setBrightness(brightness: Float) {
        queueEvent {
            renderer.setBrightness(brightness)
        }
    }

    /**
     * Switch to continuous rendering mode (60 FPS target)
     */
    fun setContinuousRendering() {
        renderMode = RENDERMODE_CONTINUOUSLY
        Log.d(TAG, "Switched to continuous rendering mode")
    }

    /**
     * Switch to on-demand rendering mode (render only when updated)
     * More battery efficient for static content
     */
    fun setOnDemandRendering() {
        renderMode = RENDERMODE_WHEN_DIRTY
        Log.d(TAG, "Switched to on-demand rendering mode")
    }

    /**
     * Request a single render pass (only effective in RENDERMODE_WHEN_DIRTY)
     */
    fun requestRenderFrame() {
        requestRender()
    }

    /**
     * Clean up resources
     * Call this in Activity/Fragment onDestroy
     */
    fun cleanup() {
        queueEvent {
            renderer.destroy()
        }
        Log.i(TAG, "TextureView cleaned up")
    }

    override fun onDetachedFromWindow() {
        cleanup()
        super.onDetachedFromWindow()
    }
}
