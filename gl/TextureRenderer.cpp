#include "TextureRenderer.h"
#include "shaders.h"
#include <android/log.h>
#include <cstring>

#define LOG_TAG "TextureRenderer"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

namespace flam {

// Full-screen quad vertices (position + texcoord)
static const GLfloat QUAD_VERTICES[] = {
    // Position (x, y)    // TexCoord (u, v)
    -1.0f, -1.0f,         0.0f, 1.0f,  // Bottom-left
     1.0f, -1.0f,         1.0f, 1.0f,  // Bottom-right
     1.0f,  1.0f,         1.0f, 0.0f,  // Top-right
    -1.0f,  1.0f,         0.0f, 0.0f   // Top-left
};

// Indices for two triangles forming a quad
static const GLushort QUAD_INDICES[] = {
    0, 1, 2,  // First triangle
    0, 2, 3   // Second triangle
};

TextureRenderer::TextureRenderer()
    : m_program(0)
    , m_positionHandle(-1)
    , m_texCoordHandle(-1)
    , m_textureHandle(-1)
    , m_brightnessHandle(-1)
    , m_vbo(0)
    , m_ibo(0)
    , m_textureId(0)
    , m_textureWidth(0)
    , m_textureHeight(0)
    , m_initialized(false)
    , m_brightness(1.0f)
    , m_viewportWidth(0)
    , m_viewportHeight(0) {
    LOGI("TextureRenderer created");
}

TextureRenderer::~TextureRenderer() {
    cleanup();
    LOGI("TextureRenderer destroyed");
}

bool TextureRenderer::initialize() {
    std::lock_guard<std::mutex> lock(m_mutex);

    if (m_initialized) {
        LOGD("Already initialized");
        return true;
    }

    LOGI("Initializing TextureRenderer");

    // Compile vertex shader
    GLuint vertexShader = compileShader(GL_VERTEX_SHADER,
                                        shaders::TEXTURE_VERTEX_SHADER);
    if (vertexShader == 0) {
        LOGE("Failed to compile vertex shader");
        return false;
    }

    // Compile fragment shader with brightness support
    GLuint fragmentShader = compileShader(GL_FRAGMENT_SHADER,
                                          shaders::TEXTURE_FRAGMENT_SHADER_ENHANCED);
    if (fragmentShader == 0) {
        LOGE("Failed to compile fragment shader");
        glDeleteShader(vertexShader);
        return false;
    }

    // Link program
    m_program = linkProgram(vertexShader, fragmentShader);
    glDeleteShader(vertexShader);
    glDeleteShader(fragmentShader);

    if (m_program == 0) {
        LOGE("Failed to link shader program");
        return false;
    }

    // Get attribute/uniform locations
    m_positionHandle = glGetAttribLocation(m_program, "aPosition");
    m_texCoordHandle = glGetAttribLocation(m_program, "aTexCoord");
    m_textureHandle = glGetUniformLocation(m_program, "uTexture");
    m_brightnessHandle = glGetUniformLocation(m_program, "uBrightness");

    LOGD("Shader locations - Position: %d, TexCoord: %d, Texture: %d, Brightness: %d",
         m_positionHandle, m_texCoordHandle, m_textureHandle, m_brightnessHandle);

    // Create buffers
    if (!createBuffers()) {
        LOGE("Failed to create buffers");
        cleanup();
        return false;
    }

    // Create texture
    glGenTextures(1, &m_textureId);
    glBindTexture(GL_TEXTURE_2D, m_textureId);

    // Set texture parameters for optimal performance
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

    glBindTexture(GL_TEXTURE_2D, 0);

    checkGLError("initialize");

    m_initialized = true;
    LOGI("TextureRenderer initialized successfully");
    return true;
}

void TextureRenderer::cleanup() {
    std::lock_guard<std::mutex> lock(m_mutex);

    if (!m_initialized) {
        return;
    }

    LOGI("Cleaning up TextureRenderer");

    if (m_textureId != 0) {
        glDeleteTextures(1, &m_textureId);
        m_textureId = 0;
    }

    if (m_vbo != 0) {
        glDeleteBuffers(1, &m_vbo);
        m_vbo = 0;
    }

    if (m_ibo != 0) {
        glDeleteBuffers(1, &m_ibo);
        m_ibo = 0;
    }

    if (m_program != 0) {
        glDeleteProgram(m_program);
        m_program = 0;
    }

    m_initialized = false;
    LOGI("TextureRenderer cleaned up");
}

bool TextureRenderer::updateTexture(const unsigned char* data, int width, int height) {
    if (!m_initialized) {
        LOGE("Renderer not initialized");
        return false;
    }

    if (data == nullptr || width <= 0 || height <= 0) {
        LOGE("Invalid texture data or dimensions");
        return false;
    }

    std::lock_guard<std::mutex> lock(m_mutex);

    glBindTexture(GL_TEXTURE_2D, m_textureId);

    // If dimensions changed, reallocate texture
    if (m_textureWidth != width || m_textureHeight != height) {
        LOGD("Texture size changed: %dx%d -> %dx%d",
             m_textureWidth, m_textureHeight, width, height);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0,
                     GL_RGBA, GL_UNSIGNED_BYTE, data);

        m_textureWidth = width;
        m_textureHeight = height;
    } else {
        // Reuse existing texture (faster)
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height,
                        GL_RGBA, GL_UNSIGNED_BYTE, data);
    }

    glBindTexture(GL_TEXTURE_2D, 0);

    checkGLError("updateTexture");
    return true;
}

void TextureRenderer::render() {
    if (!m_initialized) {
        LOGE("Renderer not initialized");
        return;
    }

    std::lock_guard<std::mutex> lock(m_mutex);

    // Clear screen
    glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT);

    // Use shader program
    glUseProgram(m_program);

    // Bind texture
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, m_textureId);
    glUniform1i(m_textureHandle, 0);

    // Set brightness
    glUniform1f(m_brightnessHandle, m_brightness);

    // Bind vertex buffer
    glBindBuffer(GL_ARRAY_BUFFER, m_vbo);

    // Enable and set vertex attributes
    glEnableVertexAttribArray(m_positionHandle);
    glVertexAttribPointer(m_positionHandle, 2, GL_FLOAT, GL_FALSE,
                          4 * sizeof(GLfloat), (void*)0);

    glEnableVertexAttribArray(m_texCoordHandle);
    glVertexAttribPointer(m_texCoordHandle, 2, GL_FLOAT, GL_FALSE,
                          4 * sizeof(GLfloat), (void*)(2 * sizeof(GLfloat)));

    // Bind index buffer and draw
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, m_ibo);
    glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, 0);

    // Cleanup
    glDisableVertexAttribArray(m_positionHandle);
    glDisableVertexAttribArray(m_texCoordHandle);
    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
    glBindTexture(GL_TEXTURE_2D, 0);
    glUseProgram(0);

    checkGLError("render");
}

void TextureRenderer::resize(int width, int height) {
    std::lock_guard<std::mutex> lock(m_mutex);

    LOGI("Viewport resized to %dx%d", width, height);

    m_viewportWidth = width;
    m_viewportHeight = height;

    glViewport(0, 0, width, height);
    checkGLError("resize");
}

void TextureRenderer::setBrightness(float brightness) {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_brightness = brightness;
    LOGD("Brightness set to %.2f", brightness);
}

GLuint TextureRenderer::compileShader(GLenum type, const char* source) {
    GLuint shader = glCreateShader(type);
    if (shader == 0) {
        LOGE("Failed to create shader");
        return 0;
    }

    glShaderSource(shader, 1, &source, nullptr);
    glCompileShader(shader);

    GLint compiled = 0;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &compiled);

    if (!compiled) {
        GLint infoLen = 0;
        glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &infoLen);

        if (infoLen > 1) {
            char* infoLog = new char[infoLen];
            glGetShaderInfoLog(shader, infoLen, nullptr, infoLog);
            LOGE("Shader compilation error:\n%s", infoLog);
            delete[] infoLog;
        }

        glDeleteShader(shader);
        return 0;
    }

    return shader;
}

GLuint TextureRenderer::linkProgram(GLuint vertexShader, GLuint fragmentShader) {
    GLuint program = glCreateProgram();
    if (program == 0) {
        LOGE("Failed to create program");
        return 0;
    }

    glAttachShader(program, vertexShader);
    glAttachShader(program, fragmentShader);
    glLinkProgram(program);

    GLint linked = 0;
    glGetProgramiv(program, GL_LINK_STATUS, &linked);

    if (!linked) {
        GLint infoLen = 0;
        glGetProgramiv(program, GL_INFO_LOG_LENGTH, &infoLen);

        if (infoLen > 1) {
            char* infoLog = new char[infoLen];
            glGetProgramInfoLog(program, infoLen, nullptr, infoLog);
            LOGE("Program linking error:\n%s", infoLog);
            delete[] infoLog;
        }

        glDeleteProgram(program);
        return 0;
    }

    return program;
}

bool TextureRenderer::createBuffers() {
    // Create and populate VBO (vertices + texcoords)
    glGenBuffers(1, &m_vbo);
    glBindBuffer(GL_ARRAY_BUFFER, m_vbo);
    glBufferData(GL_ARRAY_BUFFER, sizeof(QUAD_VERTICES), QUAD_VERTICES, GL_STATIC_DRAW);

    // Create and populate IBO (indices)
    glGenBuffers(1, &m_ibo);
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, m_ibo);
    glBufferData(GL_ELEMENT_ARRAY_BUFFER, sizeof(QUAD_INDICES), QUAD_INDICES, GL_STATIC_DRAW);

    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

    checkGLError("createBuffers");
    return true;
}

void TextureRenderer::checkGLError(const char* operation) {
    GLenum error;
    while ((error = glGetError()) != GL_NO_ERROR) {
        LOGE("OpenGL error after %s: 0x%x", operation, error);
    }
}

} // namespace flam
