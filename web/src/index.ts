/**
 * Flam Web TypeScript Module
 * Public API for web-based functionality
 */

export { FrameViewer, FrameMetadata } from './FrameViewer';

/**
 * Main Flam Web API class
 */
export class FlamWeb {
    private initialized: boolean = false;

    constructor() {
        console.log('FlamWeb initialized');
    }

    public async init(): Promise<void> {
        if (this.initialized) {
            console.warn('FlamWeb already initialized');
            return;
        }

        this.initialized = true;
        console.log('FlamWeb ready');
    }

    /**
     * Process base64 image data
     */
    public processData(data: any): any {
        // Processing logic
        return data;
    }

    /**
     * Get API version
     */
    public getVersion(): string {
        return '1.0.0';
    }
}

export default FlamWeb;
