# Flam Web Viewer

TypeScript web application for displaying processed frames from the Flam Android app.

## Features

- **Canvas-based frame display** with HTML5 Canvas API
- **Real-time metrics** showing FPS, resolution, and processing time
- **Auto-updating statistics** (frame count, uptime)
- **Demo mode** with simulated edge-detected frames at 15 FPS
- **Responsive design** with modern UI

## Project Structure

```
/web/
├── src/
│   ├── index.ts              # Public API exports
│   ├── app.ts                # Main application logic
│   ├── FrameViewer.ts        # Frame display and rendering
│   ├── styles.css            # Application styles
│   └── index.html            # HTML template
├── dist/                     # Build output (generated)
├── package.json              # NPM dependencies
├── tsconfig.json             # TypeScript configuration
└── webpack.config.js         # Webpack build configuration
```

## Getting Started

### Prerequisites

- Node.js 18+ and npm

### Installation

```bash
cd web
npm install
```

### Development

Start development server with hot reload:

```bash
npm run dev
```

This will:
- Build the TypeScript project
- Start webpack-dev-server on http://localhost:8080
- Auto-open in your default browser
- Enable hot module replacement

### Build for Production

```bash
npm run build
```

Output will be in `dist/` directory.

### Serve Production Build

```bash
npm run serve
```

Access at http://localhost:8080

## Usage

### Demo Mode

The application starts automatically in demo mode, displaying simulated edge-detected frames:

- **FPS**: 15 (dummy value)
- **Resolution**: 1920x1080 (dummy value)
- **Processing Time**: 25-40ms (randomized)
- **Frame**: Base64-encoded PNG with edge-like patterns

### Controls

- **Start Demo**: Begin frame generation at 15 FPS
- **Stop Demo**: Pause frame generation
- **Clear Canvas**: Reset canvas to blank state

### Metrics Display

**Performance Metrics:**
- FPS: Current frames per second
- Resolution: Frame dimensions
- Processing Time: Time per frame (ms)
- Timestamp: Frame capture time

**Statistics:**
- Total Frames: Cumulative frame count
- Uptime: Application runtime

## Architecture

### FrameViewer Class

Main class for rendering frames to canvas:

```typescript
const viewer = new FrameViewer('canvas-id');

// Display frame from base64 data
viewer.displayFrame(base64Image, {
    fps: 15,
    resolution: '1920x1080',
    width: 1920,
    height: 1080,
    processingTime: 25,
    timestamp: Date.now()
});
```

### FlamWebApp Class

Application controller managing:
- Frame viewer initialization
- Demo frame generation
- UI event handling
- Statistics updates

### Sample Frame Generation

`FrameViewer.generateSampleFrame()` creates edge-detected style images:
- Black gradient background
- White edge patterns (circles, rectangles, lines)
- "FLAM Edge Detection Demo" text overlay
- Converts to base64 PNG

## Integration with Android App

To integrate real frames from Flam Android app:

1. **WebSocket/HTTP Server** on Android side
2. **Base64 encode** processed frames
3. **Send metadata** with each frame:
   ```typescript
   {
       fps: number,
       resolution: string,
       width: number,
       height: number,
       processingTime: number,
       timestamp: number
   }
   ```
4. **Client connection** in `app.ts` to receive frames

Example integration:

```typescript
// Replace demo mode with WebSocket connection
const socket = new WebSocket('ws://android-device:8080');

socket.onmessage = (event) => {
    const { base64Data, metadata } = JSON.parse(event.data);
    frameViewer.displayFrame(base64Data, metadata);
};
```

## Technology Stack

- **TypeScript**: Type-safe JavaScript
- **Webpack**: Module bundler
- **HTML5 Canvas**: Frame rendering
- **CSS3**: Modern styling with gradients and animations
- **ES6+**: Modern JavaScript features

## Build Configuration

### TypeScript (tsconfig.json)

- Target: ES2020
- Module: ESNext
- Strict mode enabled
- Source maps for debugging

### Webpack (webpack.config.js)

- Entry: `src/app.ts`
- Output: `dist/bundle.[contenthash].js`
- Loaders: ts-loader, css-loader, style-loader
- Dev server: Port 8080 with hot reload
- HTML plugin for template injection

## Performance

- **Lightweight bundle**: ~50KB minified (production)
- **60 FPS capable**: Canvas rendering optimized
- **Efficient updates**: Only redraw on new frames
- **Responsive**: Adapts to viewport size

## Browser Support

- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+

Requires ES6+ and Canvas API support.

## Scripts

```bash
# Development with hot reload
npm run dev

# Production build
npm run build

# Watch mode (rebuild on changes)
npm run watch

# Clean build artifacts
npm run clean

# Serve production build
npm run serve
```

## License

Copyright © 2025 Flam Project
