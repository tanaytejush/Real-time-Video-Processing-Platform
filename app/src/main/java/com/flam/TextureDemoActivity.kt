package com.flam

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.nio.ByteBuffer

/**
 * Demo activity showing 60 FPS OpenGL texture rendering
 * Demonstrates rendering from byte arrays with real-time updates
 */
class TextureDemoActivity : AppCompatActivity() {

    private lateinit var textureView: TextureView
    private lateinit var brightnessSeekBar: SeekBar
    private lateinit var fpsText: TextView

    private var currentFrame = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_texture_demo)

        textureView = findViewById(R.id.textureView)
        brightnessSeekBar = findViewById(R.id.brightnessSeekBar)
        fpsText = findViewById(R.id.fpsText)

        setupControls()
        startDemoAnimation()
    }

    private fun setupControls() {
        // Brightness control
        brightnessSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val brightness = progress / 100.0f
                textureView.setBrightness(brightness)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    /**
     * Demo: Generate animated test pattern
     * In real usage, this would be replaced with camera frames or processed images
     */
    private fun startDemoAnimation() {
        val width = 512
        val height = 512

        Thread {
            while (!isDestroyed) {
                val testPattern = generateTestPattern(width, height, currentFrame)
                textureView.updateTexture(testPattern, width, height)

                currentFrame++

                // Target 60 FPS = 16.67ms per frame
                Thread.sleep(16)
            }
        }.start()
    }

    /**
     * Generate animated test pattern
     * Creates a colorful gradient that animates over time
     */
    private fun generateTestPattern(width: Int, height: Int, frame: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Create animated gradient pattern
        for (y in 0 until height) {
            for (x in 0 until width) {
                val r = ((x + frame) % 256)
                val g = ((y + frame) % 256)
                val b = ((x + y + frame) % 256)
                val color = Color.rgb(r, g, b)
                bitmap.setPixel(x, y, color)
            }
        }

        // Convert to RGBA byte array
        val buffer = ByteBuffer.allocate(width * height * 4)
        bitmap.copyPixelsToBuffer(buffer)

        return buffer.array()
    }

    override fun onPause() {
        super.onPause()
        textureView.onPause()
    }

    override fun onResume() {
        super.onResume()
        textureView.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        textureView.cleanup()
    }
}
