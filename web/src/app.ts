/**
 * Flam Web Viewer - Main Application
 * Displays processed frames from Flam Android app
 */

import { FrameViewer, FrameMetadata } from './FrameViewer';
import './styles.css';

class FlamWebApp {
    private frameViewer: FrameViewer;
    private isRunning: boolean = false;
    private frameInterval: number | null = null;

    constructor() {
        console.log('Flam Web Viewer starting...');

        // Initialize frame viewer
        this.frameViewer = new FrameViewer('frame-canvas');

        // Setup UI event handlers
        this.setupEventHandlers();

        // Display initial placeholder
        this.frameViewer.clear();

        // Auto-start demo
        this.startDemo();

        console.log('Flam Web Viewer initialized');
    }

    private setupEventHandlers(): void {
        const startButton = document.getElementById('start-btn') as HTMLButtonElement;
        const stopButton = document.getElementById('stop-btn') as HTMLButtonElement;
        const clearButton = document.getElementById('clear-btn') as HTMLButtonElement;

        startButton?.addEventListener('click', () => this.startDemo());
        stopButton?.addEventListener('click', () => this.stopDemo());
        clearButton?.addEventListener('click', () => this.frameViewer.clear());
    }

    private startDemo(): void {
        if (this.isRunning) {
            console.log('Demo already running');
            return;
        }

        console.log('Starting demo...');
        this.isRunning = true;

        // Update button states
        this.updateButtonStates();

        // Display demo frame at 15 FPS
        const fps = 15;
        const interval = 1000 / fps;

        this.frameInterval = window.setInterval(() => {
            this.displayDemoFrame();
        }, interval);

        // Display first frame immediately
        this.displayDemoFrame();
    }

    private stopDemo(): void {
        if (!this.isRunning) {
            return;
        }

        console.log('Stopping demo...');
        this.isRunning = false;

        if (this.frameInterval !== null) {
            clearInterval(this.frameInterval);
            this.frameInterval = null;
        }

        this.updateButtonStates();
    }

    private displayDemoFrame(): void {
        // Generate sample frame
        const base64Image = FrameViewer.generateSampleFrame(1920, 1080);

        // Create metadata
        const metadata: FrameMetadata = {
            fps: 15,
            resolution: '1920x1080',
            width: 1920,
            height: 1080,
            processingTime: 25 + Math.floor(Math.random() * 15), // 25-40ms
            timestamp: Date.now()
        };

        // Display frame
        this.frameViewer.displayFrame(base64Image, metadata);

        // Update stats
        this.updateStats();
    }

    private updateStats(): void {
        const frameCountEl = document.getElementById('frame-count-value');
        const uptimeEl = document.getElementById('uptime-value');

        if (frameCountEl) {
            frameCountEl.textContent = this.frameViewer.getFrameCount().toString();
        }

        if (uptimeEl) {
            uptimeEl.textContent = `${this.frameViewer.getUptime().toFixed(1)}s`;
        }
    }

    private updateButtonStates(): void {
        const startButton = document.getElementById('start-btn') as HTMLButtonElement;
        const stopButton = document.getElementById('stop-btn') as HTMLButtonElement;

        if (startButton && stopButton) {
            startButton.disabled = this.isRunning;
            stopButton.disabled = !this.isRunning;
        }
    }
}

// Initialize app when DOM is ready
window.addEventListener('DOMContentLoaded', () => {
    console.log('DOM loaded, initializing Flam Web Viewer...');
    new FlamWebApp();
});
