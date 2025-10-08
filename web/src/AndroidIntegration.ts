/**
 * Android Integration Module
 * Supports 3 methods: JSON file loading, HTTP polling, WebSocket streaming
 */

export interface AndroidFrameData {
    image: string;  // base64 data URL
    fps: number;
    resolution: string;
    width: number;
    height: number;
    timestamp: string;
    timestampMs: number;
    processingTimeMs: number;
    cannyLowThreshold: number;
    cannyHighThreshold: number;
    deviceInfo: string;
    androidVersion: number;
    frameNumber: number;
}

export type FrameCallback = (frame: AndroidFrameData) => void;
export type ErrorCallback = (error: Error) => void;

/**
 * Method 1: JSON File Loader
 */
export class JsonFileLoader {
    constructor(
        private onFrame: FrameCallback,
        private onError: ErrorCallback
    ) {}

    loadFile(file: File): void {
        const reader = new FileReader();

        reader.onload = (e) => {
            try {
                const json = JSON.parse(e.target?.result as string) as AndroidFrameData;
                this.onFrame(json);
            } catch (error) {
                this.onError(new Error(`Failed to parse JSON: ${error}`));
            }
        };

        reader.onerror = () => {
            this.onError(new Error('Failed to read file'));
        };

        reader.readAsText(file);
    }

    setupFileInput(inputElement: HTMLInputElement): void {
        inputElement.addEventListener('change', (e) => {
            const file = (e.target as HTMLInputElement).files?.[0];
            if (file) {
                this.loadFile(file);
            }
        });
    }

    setupDragDrop(element: HTMLElement): void {
        element.addEventListener('dragover', (e) => {
            e.preventDefault();
            element.style.border = '3px dashed #2196F3';
        });

        element.addEventListener('dragleave', () => {
            element.style.border = '';
        });

        element.addEventListener('drop', (e) => {
            e.preventDefault();
            element.style.border = '';

            const file = e.dataTransfer?.files[0];
            if (file && file.name.endsWith('.json')) {
                this.loadFile(file);
            } else {
                this.onError(new Error('Please drop a JSON file'));
            }
        });
    }
}

/**
 * Method 2: HTTP Polling
 */
export class HttpPoller {
    private intervalId?: number;
    private isRunning = false;

    constructor(
        private baseUrl: string,
        private onFrame: FrameCallback,
        private onError: ErrorCallback,
        private pollIntervalMs: number = 100  // 10 FPS default
    ) {}

    async start(): Promise<void> {
        if (this.isRunning) {
            return;
        }

        this.isRunning = true;
        console.log(`Starting HTTP polling: ${this.baseUrl} every ${this.pollIntervalMs}ms`);

        // Initial fetch
        await this.fetchFrame();

        // Start polling
        this.intervalId = window.setInterval(() => {
            this.fetchFrame();
        }, this.pollIntervalMs);
    }

    stop(): void {
        if (this.intervalId) {
            clearInterval(this.intervalId);
            this.intervalId = undefined;
        }
        this.isRunning = false;
        console.log('HTTP polling stopped');
    }

    private async fetchFrame(): Promise<void> {
        try {
            const response = await fetch(`${this.baseUrl}/frame`);

            if (response.ok) {
                const json = await response.json() as AndroidFrameData;
                this.onFrame(json);
            } else if (response.status === 404) {
                // No frame available yet - not an error
                console.debug('No frame available (404)');
            } else {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
        } catch (error) {
            this.onError(new Error(`Fetch failed: ${error}`));
        }
    }

    async checkStatus(): Promise<boolean> {
        try {
            const response = await fetch(`${this.baseUrl}/status`);
            return response.ok;
        } catch {
            return false;
        }
    }
}

/**
 * Method 3: WebSocket Streaming
 */
export class WebSocketStreamer {
    private ws?: WebSocket;
    private reconnectTimeoutId?: number;
    private reconnectAttempts = 0;
    private maxReconnectAttempts = 5;
    private reconnectDelayMs = 2000;

    constructor(
        private wsUrl: string,
        private onFrame: FrameCallback,
        private onError: ErrorCallback,
        private onConnectionChange?: (connected: boolean) => void
    ) {}

    connect(): void {
        if (this.ws?.readyState === WebSocket.OPEN) {
            console.log('WebSocket already connected');
            return;
        }

        console.log(`Connecting to WebSocket: ${this.wsUrl}`);

        try {
            this.ws = new WebSocket(this.wsUrl);

            this.ws.onopen = () => {
                console.log('WebSocket connected');
                this.reconnectAttempts = 0;
                this.onConnectionChange?.(true);

                // Request latest frame
                this.ws?.send('get_latest');
            };

            this.ws.onmessage = (event) => {
                try {
                    const json = JSON.parse(event.data) as AndroidFrameData;

                    // Handle array (history) or single frame
                    if (Array.isArray(json)) {
                        // Got history - display last frame
                        if (json.length > 0) {
                            this.onFrame(json[json.length - 1]);
                        }
                    } else if (json.image) {
                        // Single frame
                        this.onFrame(json);
                    }
                } catch (error) {
                    this.onError(new Error(`Failed to parse WebSocket message: ${error}`));
                }
            };

            this.ws.onerror = (error) => {
                console.error('WebSocket error:', error);
                this.onError(new Error('WebSocket connection error'));
            };

            this.ws.onclose = (event) => {
                console.log(`WebSocket closed (code: ${event.code}, reason: ${event.reason})`);
                this.onConnectionChange?.(false);

                // Auto-reconnect
                if (this.reconnectAttempts < this.maxReconnectAttempts) {
                    this.reconnectAttempts++;
                    console.log(`Reconnecting in ${this.reconnectDelayMs}ms (attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts})`);

                    this.reconnectTimeoutId = window.setTimeout(() => {
                        this.connect();
                    }, this.reconnectDelayMs);
                } else {
                    this.onError(new Error('Max reconnection attempts reached'));
                }
            };

        } catch (error) {
            this.onError(new Error(`Failed to create WebSocket: ${error}`));
        }
    }

    disconnect(): void {
        if (this.reconnectTimeoutId) {
            clearTimeout(this.reconnectTimeoutId);
            this.reconnectTimeoutId = undefined;
        }

        if (this.ws) {
            this.ws.close();
            this.ws = undefined;
        }

        this.reconnectAttempts = 0;
        console.log('WebSocket disconnected');
    }

    requestLatest(): void {
        if (this.ws?.readyState === WebSocket.OPEN) {
            this.ws.send('get_latest');
        }
    }

    requestHistory(): void {
        if (this.ws?.readyState === WebSocket.OPEN) {
            this.ws.send('get_history');
        }
    }

    isConnected(): boolean {
        return this.ws?.readyState === WebSocket.OPEN;
    }
}

/**
 * Unified Android Integration Manager
 * Handles all 3 methods from a single interface
 */
export class AndroidIntegrationManager {
    private jsonLoader?: JsonFileLoader;
    private httpPoller?: HttpPoller;
    private wsStreamer?: WebSocketStreamer;
    private currentMethod: 'json' | 'http' | 'websocket' | null = null;

    constructor(
        private onFrame: FrameCallback,
        private onError: ErrorCallback,
        private onStatusChange?: (status: string) => void
    ) {}

    // Method 1: Setup JSON file loading
    setupJsonLoading(fileInput: HTMLInputElement, dragDropElement?: HTMLElement): void {
        this.jsonLoader = new JsonFileLoader(this.onFrame, this.onError);
        this.jsonLoader.setupFileInput(fileInput);

        if (dragDropElement) {
            this.jsonLoader.setupDragDrop(dragDropElement);
        }

        this.currentMethod = 'json';
        this.onStatusChange?.('JSON file loading ready');
    }

    // Method 2: Start HTTP polling
    startHttpPolling(androidIp: string, port: number = 8080, intervalMs: number = 100): void {
        this.stopAll();

        const baseUrl = `http://${androidIp}:${port}`;
        this.httpPoller = new HttpPoller(baseUrl, this.onFrame, this.onError, intervalMs);
        this.httpPoller.start();

        this.currentMethod = 'http';
        this.onStatusChange?.(`HTTP polling: ${baseUrl}`);
    }

    // Method 3: Start WebSocket streaming
    startWebSocket(androidIp: string, port: number = 8080): void {
        this.stopAll();

        const wsUrl = `ws://${androidIp}:${port}`;
        this.wsStreamer = new WebSocketStreamer(
            wsUrl,
            this.onFrame,
            this.onError,
            (connected) => {
                this.onStatusChange?.(connected ? 'WebSocket connected' : 'WebSocket disconnected');
            }
        );
        this.wsStreamer.connect();

        this.currentMethod = 'websocket';
        this.onStatusChange?.(`WebSocket connecting: ${wsUrl}`);
    }

    stopAll(): void {
        this.httpPoller?.stop();
        this.wsStreamer?.disconnect();
        this.currentMethod = null;
        this.onStatusChange?.('Stopped');
    }

    getCurrentMethod(): string {
        return this.currentMethod || 'none';
    }

    async testHttpConnection(androidIp: string, port: number = 8080): Promise<boolean> {
        const poller = new HttpPoller(
            `http://${androidIp}:${port}`,
            () => {},
            () => {}
        );
        return await poller.checkStatus();
    }
}
