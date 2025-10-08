# 🎥 Load Your Own Video Frames - Quick Guide

## Right Now (5 Minutes)

You can load your own video frames into the web viewer that's currently running at **http://localhost:8080**!

---

## Quick Method: Using Sample Images

### Step 1: Find a Video Frame

You can use ANY image file (PNG, JPG, etc.):
- Screenshot from a video
- Photo from your phone
- Downloaded image
- Screen capture

### Step 2: Convert to Base64

**On macOS**:
```bash
# Convert image to base64
base64 -i /path/to/your/image.png | pbcopy

# Now the base64 is copied to clipboard!
```

**On Linux**:
```bash
base64 -w 0 /path/to/your/image.png | xclip -selection clipboard
```

**On Windows (PowerShell)**:
```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\path\to\image.png")) | Set-Clipboard
```

**Online Tool**:
- Go to: https://www.base64-image.de/
- Upload image
- Copy the base64 string

### Step 3: Edit app.ts

Open `web/src/app.ts` in any text editor:

```bash
# Using VS Code
code "/Users/tejushtanay/Projects /Flam/web/src/app.ts"

# Or any editor
nano "/Users/tejushtanay/Projects /Flam/web/src/app.ts"
```

Find line ~10 and replace the `SAMPLE_FRAME_BASE64` with your base64:

```typescript
// Replace this:
const SAMPLE_FRAME_BASE64 = "data:image/png;base64,iVBORw0KGgo...";

// With your base64 (paste from clipboard):
const SAMPLE_FRAME_BASE64 = "data:image/png;base64,YOUR_BASE64_HERE";
```

### Step 4: Save and Watch Auto-Reload!

The webpack dev server will automatically reload the page and show your image!

---

## Advanced: Load Multiple Video Frames

### Create a Video Frame Array

Edit `web/src/app.ts`:

```typescript
// Add array of frames
const videoFrames = [
    "data:image/png;base64,FRAME_1_BASE64_HERE",
    "data:image/png;base64,FRAME_2_BASE64_HERE",
    "data:image/png;base64,FRAME_3_BASE64_HERE",
    "data:image/png;base64,FRAME_4_BASE64_HERE",
    "data:image/png;base64,FRAME_5_BASE64_HERE",
];

let currentFrame = 0;

function playVideoSequence() {
    setInterval(() => {
        viewer.displayFrame(videoFrames[currentFrame], {
            fps: 15,
            width: 1920,
            height: 1080,
            processingTime: 67,
            timestamp: Date.now()
        });

        currentFrame = (currentFrame + 1) % videoFrames.length;
    }, 1000 / 15);  // 15 FPS
}

// Auto-start video playback
playVideoSequence();
```

---

## Even Faster: Use a URL

If you have images hosted online:

```typescript
const imageUrl = "https://example.com/your-frame.png";

// Load from URL
fetch(imageUrl)
    .then(response => response.blob())
    .then(blob => {
        const reader = new FileReader();
        reader.onloadend = () => {
            const base64 = reader.result as string;
            viewer.displayFrame(base64, {
                fps: 15,
                width: 1920,
                height: 1080,
                processingTime: 67,
                timestamp: Date.now()
            });
        };
        reader.readAsDataURL(blob);
    });
```

---

## Extract Frames from Video File

### Using Online Tools (No Installation)

1. **EZGif** - https://ezgif.com/video-to-jpg
   - Upload video
   - Click "Convert to Images"
   - Download ZIP of frames
   - Convert each to base64

2. **Kapwing** - https://www.kapwing.com/tools/extract-frames
   - Upload video
   - Set frame rate (e.g., 1 frame per second)
   - Download frames

### Using VLC Player (Already on Mac)

1. Open video in VLC
2. **Video** menu → **Take Snapshot** (⌘⌥S)
3. Frames saved to: `~/Pictures/`
4. Convert to base64 and use!

### Using QuickTime Player (Mac)

1. Open video in QuickTime
2. Pause at desired frame
3. **Edit** → **Copy**
4. Open Preview
5. **File** → **New from Clipboard**
6. Save as PNG
7. Convert to base64

---

## Real Example

Let's say you have a video at `~/Downloads/my_video.mp4`:

```bash
# 1. Extract frame at 5 seconds using ffmpeg
ffmpeg -i ~/Downloads/my_video.mp4 -ss 00:00:05 -vframes 1 ~/Downloads/frame.png

# 2. Convert to base64
base64 -i ~/Downloads/frame.png | pbcopy

# 3. Edit app.ts
code "/Users/tejushtanay/Projects /Flam/web/src/app.ts"

# 4. Paste base64 and save
# Web viewer auto-reloads with your frame!
```

---

## Test with Sample Images

### Quick Test Images

Try these sample base64 images:

**Red Square** (Tiny - for testing):
```typescript
const SAMPLE_FRAME_BASE64 = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAAFUlEQVR42mP8z8BQz0AEYBxVSF+FABJADveWkH6oAAAAAElFTkSuQmCC";
```

**Blue Circle**:
```typescript
const SAMPLE_FRAME_BASE64 = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAAH0lEQVR42mNk+M9Qz0AEGBjGQxWO0gxVOMoACAAlYQOLvQUiZAAAAABJRU5ErkJggg==";
```

Just paste these and see them appear instantly!

---

## Current Web Viewer Controls

Once your frames are loaded, use:

- **▶️ Start**: Begin playback
- **⏸️ Stop**: Pause playback
- **🗑️ Clear**: Reset counter

---

## File Size Considerations

**Base64 increases size by ~33%**:

| Image Size | Base64 Size | Recommended |
|-----------|-------------|-------------|
| 10 KB | 13 KB | ✅ Perfect |
| 100 KB | 133 KB | ✅ Good |
| 500 KB | 665 KB | ⚠️ Okay |
| 1 MB | 1.3 MB | ⚠️ Slow |
| 5 MB | 6.5 MB | ❌ Too large |

**Tip**: Resize images to 1920x1080 or smaller before converting.

---

## Optimize Images First

```bash
# Resize to 1920x1080
ffmpeg -i input.png -vf scale=1920:1080 output.png

# Compress PNG
pngquant --quality=80-100 input.png -o output.png

# Convert to JPG (smaller)
convert input.png -quality 85 output.jpg
```

---

## Troubleshooting

### "Image not showing"
- Check base64 string starts with `data:image/png;base64,`
- Ensure no line breaks in base64 string
- Try a smaller test image first

### "Page not reloading"
```bash
# Restart dev server
cd "/Users/tejushtanay/Projects /Flam/web"
npm run dev
```

### "Out of memory"
- Use smaller images (<500KB each)
- Limit number of frames (max 10-20)
- Resize images before converting

---

## Quick Test Right Now!

1. **Find any image** on your computer
2. **Run**: `base64 -i /path/to/image.png | pbcopy`
3. **Edit**: `web/src/app.ts` line 10
4. **Paste** clipboard contents
5. **Save** file
6. **Watch** browser auto-reload!

**Total time**: 30 seconds! 🚀

---

## Next Steps

Once you've tested with images:

1. Extract multiple frames from a video
2. Create a frame array for playback
3. Adjust FPS for smooth playback
4. Add your own UI controls

**Your video frames are now playing in the web viewer!** 🎬
