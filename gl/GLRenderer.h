#ifndef FLAM_GLRENDERER_H
#define FLAM_GLRENDERER_H

class GLRenderer {
public:
    GLRenderer();
    ~GLRenderer();

    void init();
    void render();
    void resize(int width, int height);

private:
    // Add member variables as needed
};

#endif // FLAM_GLRENDERER_H
