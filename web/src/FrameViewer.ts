/**
 * FrameViewer - Displays processed frames from Flam Android app
 * Shows base64 encoded images with real-time FPS and resolution info
 */

export interface FrameMetadata {
    fps: number;
    resolution: string;
    width: number;
    height: number;
    processingTime?: number;
    timestamp?: number;
}

export class FrameViewer {
    private canvas: HTMLCanvasElement;
    private ctx: CanvasRenderingContext2D;
    private fpsDisplay: HTMLElement;
    private resolutionDisplay: HTMLElement;
    private processingTimeDisplay: HTMLElement;
    private timestampDisplay: HTMLElement;

    private currentFrame: HTMLImageElement | null = null;
    private frameCount: number = 0;
    private startTime: number = Date.now();

    constructor(canvasId: string) {
        this.canvas = document.getElementById(canvasId) as HTMLCanvasElement;
        if (!this.canvas) {
            throw new Error(`Canvas element with id '${canvasId}' not found`);
        }

        const ctx = this.canvas.getContext('2d');
        if (!ctx) {
            throw new Error('Failed to get 2D context from canvas');
        }
        this.ctx = ctx;

        // Get display elements
        this.fpsDisplay = document.getElementById('fps-value')!;
        this.resolutionDisplay = document.getElementById('resolution-value')!;
        this.processingTimeDisplay = document.getElementById('processing-time-value')!;
        this.timestampDisplay = document.getElementById('timestamp-value')!;

        console.log('FrameViewer initialized');
    }

    /**
     * Display a frame from base64 encoded image data
     */
    public displayFrame(base64Data: string, metadata: FrameMetadata): void {
        const img = new Image();

        img.onload = () => {
            // Resize canvas to match image if needed
            if (this.canvas.width !== metadata.width || this.canvas.height !== metadata.height) {
                this.canvas.width = metadata.width;
                this.canvas.height = metadata.height;
            }

            // Clear canvas and draw image
            this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
            this.ctx.drawImage(img, 0, 0, metadata.width, metadata.height);

            // Update metadata displays
            this.updateMetadata(metadata);

            // Track frame count
            this.frameCount++;
            this.currentFrame = img;

            console.log(`Frame displayed: ${metadata.width}x${metadata.height}, FPS: ${metadata.fps}`);
        };

        img.onerror = () => {
            console.error('Failed to load base64 image');
        };

        // Set image source
        img.src = base64Data;
    }

    /**
     * Update metadata displays
     */
    private updateMetadata(metadata: FrameMetadata): void {
        this.fpsDisplay.textContent = metadata.fps.toFixed(1);
        this.resolutionDisplay.textContent = metadata.resolution;

        if (metadata.processingTime !== undefined) {
            this.processingTimeDisplay.textContent = `${metadata.processingTime}ms`;
        }

        if (metadata.timestamp !== undefined) {
            const date = new Date(metadata.timestamp);
            this.timestampDisplay.textContent = date.toLocaleTimeString();
        }
    }

    /**
     * Clear the canvas
     */
    public clear(): void {
        this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
        this.ctx.fillStyle = '#000000';
        this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);

        // Draw placeholder text
        this.ctx.fillStyle = '#FFFFFF';
        this.ctx.font = '24px Arial';
        this.ctx.textAlign = 'center';
        this.ctx.fillText('Waiting for frames...', this.canvas.width / 2, this.canvas.height / 2);
    }

    /**
     * Get current frame count
     */
    public getFrameCount(): number {
        return this.frameCount;
    }

    /**
     * Get uptime in seconds
     */
    public getUptime(): number {
        return (Date.now() - this.startTime) / 1000;
    }

    /**
     * Generate sample base64 image (edge detected pattern)
     */
    public static generateSampleFrame(width: number, height: number): string {
        const canvas = document.createElement('canvas');
        canvas.width = width;
        canvas.height = height;
        const ctx = canvas.getContext('2d')!;

        // Create gradient background
        const gradient = ctx.createLinearGradient(0, 0, width, height);
        gradient.addColorStop(0, '#000000');
        gradient.addColorStop(0.5, '#1a1a1a');
        gradient.addColorStop(1, '#000000');
        ctx.fillStyle = gradient;
        ctx.fillRect(0, 0, width, height);

        // Draw edge-like patterns (simulating Canny edge detection)
        ctx.strokeStyle = '#FFFFFF';
        ctx.lineWidth = 2;

        // Draw some geometric shapes (edges)
        ctx.beginPath();
        ctx.arc(width / 2, height / 2, 100, 0, Math.PI * 2);
        ctx.stroke();

        ctx.beginPath();
        ctx.rect(width / 4, height / 4, width / 2, height / 2);
        ctx.stroke();

        // Draw random edge-like lines
        for (let i = 0; i < 20; i++) {
            ctx.beginPath();
            ctx.moveTo(Math.random() * width, Math.random() * height);
            ctx.lineTo(Math.random() * width, Math.random() * height);
            ctx.stroke();
        }

        // Add "Flam Edge Detection" text
        ctx.fillStyle = '#FFFFFF';
        ctx.font = 'bold 48px Arial';
        ctx.textAlign = 'center';
        ctx.fillText('FLAM', width / 2, height / 2 - 20);
        ctx.font = '24px Arial';
        ctx.fillText('Edge Detection Demo', width / 2, height / 2 + 20);

        // Convert to base64
        return canvas.toDataURL('image/png');
    }
}
