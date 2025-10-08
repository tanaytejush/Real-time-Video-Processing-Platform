#ifndef FLAM_SHADERS_H
#define FLAM_SHADERS_H

namespace flam {
namespace shaders {

// Vertex shader for texture rendering
// Optimized for minimal vertex processing
const char* const TEXTURE_VERTEX_SHADER = R"(
    #version 100
    precision highp float;

    attribute vec4 aPosition;
    attribute vec2 aTexCoord;

    varying vec2 vTexCoord;

    void main() {
        gl_Position = aPosition;
        vTexCoord = aTexCoord;
    }
)";

// Fragment shader for texture rendering
// Optimized for direct texture sampling without extra processing
const char* const TEXTURE_FRAGMENT_SHADER = R"(
    #version 100
    precision mediump float;

    varying vec2 vTexCoord;
    uniform sampler2D uTexture;

    void main() {
        gl_FragColor = texture2D(uTexture, vTexCoord);
    }
)";

// High-performance fragment shader with brightness adjustment
const char* const TEXTURE_FRAGMENT_SHADER_ENHANCED = R"(
    #version 100
    precision mediump float;

    varying vec2 vTexCoord;
    uniform sampler2D uTexture;
    uniform float uBrightness;

    void main() {
        vec4 color = texture2D(uTexture, vTexCoord);
        gl_FragColor = vec4(color.rgb * uBrightness, color.a);
    }
)";

// Fragment shader for YUV to RGB conversion (for camera frames)
const char* const YUV_TO_RGB_FRAGMENT_SHADER = R"(
    #version 100
    precision mediump float;

    varying vec2 vTexCoord;
    uniform sampler2D uTextureY;
    uniform sampler2D uTextureUV;

    void main() {
        float y = texture2D(uTextureY, vTexCoord).r;
        vec2 uv = texture2D(uTextureUV, vTexCoord).ra - vec2(0.5, 0.5);

        float r = y + 1.402 * uv.y;
        float g = y - 0.344 * uv.x - 0.714 * uv.y;
        float b = y + 1.772 * uv.x;

        gl_FragColor = vec4(r, g, b, 1.0);
    }
)";

} // namespace shaders
} // namespace flam

#endif // FLAM_SHADERS_H
