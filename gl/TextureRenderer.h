#ifndef FLAM_TEXTURE_RENDERER_H
#define FLAM_TEXTURE_RENDERER_H

#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <memory>
#include <mutex>

namespace flam {

/**
 * High-performance OpenGL ES 2.0 texture renderer
 * Optimized for 60 FPS rendering from byte arrays
 */
class TextureRenderer {
public:
    TextureRenderer();
    ~TextureRenderer();

    // Prevent copying
    TextureRenderer(const TextureRenderer&) = delete;
    TextureRenderer& operator=(const TextureRenderer&) = delete;

    /**
     * Initialize OpenGL resources (shaders, buffers, textures)
     * Must be called from GL thread
     * @return true if successful
     */
    bool initialize();

    /**
     * Clean up OpenGL resources
     * Must be called from GL thread
     */
    void cleanup();

    /**
     * Update texture from byte array (RGBA format)
     * @param data RGBA byte data
     * @param width Texture width
     * @param height Texture height
     * @return true if successful
     */
    bool updateTexture(const unsigned char* data, int width, int height);

    /**
     * Render the current texture to screen
     * Optimized for 60 FPS
     */
    void render();

    /**
     * Handle viewport resize
     * @param width New viewport width
     * @param height New viewport height
     */
    void resize(int width, int height);

    /**
     * Set brightness multiplier
     * @param brightness Brightness factor (1.0 = normal, 0.0 = black, 2.0 = double bright)
     */
    void setBrightness(float brightness);

    /**
     * Check if renderer is initialized
     */
    bool isInitialized() const { return m_initialized; }

    /**
     * Get current texture dimensions
     */
    void getTextureDimensions(int& width, int& height) const {
        width = m_textureWidth;
        height = m_textureHeight;
    }

private:
    /**
     * Compile shader from source
     */
    GLuint compileShader(GLenum type, const char* source);

    /**
     * Link shader program
     */
    GLuint linkProgram(GLuint vertexShader, GLuint fragmentShader);

    /**
     * Create and setup vertex buffer objects
     */
    bool createBuffers();

    /**
     * Log OpenGL errors
     */
    void checkGLError(const char* operation);

    // Shader program and attributes
    GLuint m_program;
    GLint m_positionHandle;
    GLint m_texCoordHandle;
    GLint m_textureHandle;
    GLint m_brightnessHandle;

    // Vertex buffer objects
    GLuint m_vbo;
    GLuint m_ibo;

    // Texture
    GLuint m_textureId;
    int m_textureWidth;
    int m_textureHeight;

    // State
    bool m_initialized;
    float m_brightness;
    int m_viewportWidth;
    int m_viewportHeight;

    // Thread safety
    mutable std::mutex m_mutex;
};

} // namespace flam

#endif // FLAM_TEXTURE_RENDERER_H
