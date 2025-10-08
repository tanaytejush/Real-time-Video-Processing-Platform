/**
 * Flam Web Viewer - Android Integration
 * Supports 3 connection methods: JSON files, HTTP polling, WebSocket streaming
 */

import './styles.css';
import { FrameViewer } from './FrameViewer';
import { AndroidIntegrationManager, AndroidFrameData } from './AndroidIntegration';

console.log('🔥 Flam Web Viewer - Android Integration Mode');

// Initialize frame viewer
const viewer = new FrameViewer('frameCanvas');

// Status display
const statusElement = document.createElement('div');
statusElement.id = 'connectionStatus';
statusElement.style.cssText = `
    position: fixed;
    top: 10px;
    left: 50%;
    transform: translateX(-50%);
    background: #2196F3;
    color: white;
    padding: 10px 20px;
    border-radius: 20px;
    font-size: 14px;
    font-weight: bold;
    z-index: 1000;
    box-shadow: 0 2px 10px rgba(0,0,0,0.2);
`;
statusElement.textContent = 'Ready';
document.body.appendChild(statusElement);

// Frame counter
let frameCount = 0;
const updateFrameCounter = () => {
    const counterEl = document.querySelector('.stat-value');
    if (counterEl) {
        counterEl.textContent = frameCount.toString();
    }
};

// Integration manager
const integration = new AndroidIntegrationManager(
    // On frame received
    (frame: AndroidFrameData) => {
        viewer.displayFrame(frame.image, {
            fps: frame.fps,
            resolution: frame.resolution,
            width: frame.width,
            height: frame.height,
            processingTime: frame.processingTimeMs,
            timestamp: frame.timestampMs
        });

        frameCount++;
        updateFrameCounter();

        console.log(`Frame ${frame.frameNumber}: ${frame.resolution} @ ${frame.fps.toFixed(1)} FPS`);
    },

    // On error
    (error: Error) => {
        console.error('Integration error:', error);
        statusElement.textContent = `Error: ${error.message}`;
        statusElement.style.background = '#F44336';
    },

    // On status change
    (status: string) => {
        console.log('Status:', status);
        statusElement.textContent = status;
        statusElement.style.background = status.includes('connected') ? '#4CAF50' : '#2196F3';
    }
);

// Create UI controls
const controlPanel = document.createElement('div');
controlPanel.style.cssText = `
    position: fixed;
    bottom: 20px;
    left: 50%;
    transform: translateX(-50%);
    background: white;
    padding: 20px;
    border-radius: 10px;
    box-shadow: 0 4px 20px rgba(0,0,0,0.15);
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
    max-width: 90%;
    z-index: 999;
`;

// Method selector
const methodSelector = document.createElement('select');
methodSelector.style.cssText = 'padding: 10px; border-radius: 5px; border: 2px solid #2196F3;';
methodSelector.innerHTML = `
    <option value="">Select Method</option>
    <option value="json">📂 JSON File</option>
    <option value="http">🌐 HTTP Server</option>
    <option value="websocket">⚡ WebSocket</option>
`;

// Android IP input
const ipInput = document.createElement('input');
ipInput.type = 'text';
ipInput.placeholder = 'Android IP (e.g., 192.168.1.100)';
ipInput.value = 'localhost';
ipInput.style.cssText = 'padding: 10px; border-radius: 5px; border: 2px solid #ddd; min-width: 200px;';

// Port input
const portInput = document.createElement('input');
portInput.type = 'number';
portInput.placeholder = 'Port';
portInput.value = '8080';
portInput.style.cssText = 'padding: 10px; border-radius: 5px; border: 2px solid #ddd; width: 80px;';

// Connect button
const connectButton = document.createElement('button');
connectButton.textContent = '▶️ Connect';
connectButton.style.cssText = `
    padding: 10px 20px;
    background: #4CAF50;
    color: white;
    border: none;
    border-radius: 5px;
    font-weight: bold;
    cursor: pointer;
`;

// Stop button
const stopButton = document.createElement('button');
stopButton.textContent = '⏸️ Stop';
stopButton.style.cssText = `
    padding: 10px 20px;
    background: #F44336;
    color: white;
    border: none;
    border-radius: 5px;
    font-weight: bold;
    cursor: pointer;
`;

// Test connection button
const testButton = document.createElement('button');
testButton.textContent = '🔍 Test';
testButton.style.cssText = `
    padding: 10px 20px;
    background: #FF9800;
    color: white;
    border: none;
    border-radius: 5px;
    font-weight: bold;
    cursor: pointer;
`;

// File input (hidden)
const fileInput = document.createElement('input');
fileInput.type = 'file';
fileInput.accept = '.json';
fileInput.style.display = 'none';

// Assemble control panel
controlPanel.appendChild(methodSelector);
controlPanel.appendChild(ipInput);
controlPanel.appendChild(portInput);
controlPanel.appendChild(connectButton);
controlPanel.appendChild(stopButton);
controlPanel.appendChild(testButton);
controlPanel.appendChild(fileInput);
document.body.appendChild(controlPanel);

// Method selection handler
methodSelector.addEventListener('change', () => {
    const method = methodSelector.value;

    if (method === 'json') {
        ipInput.style.display = 'none';
        portInput.style.display = 'none';
        testButton.style.display = 'none';
    } else {
        ipInput.style.display = 'block';
        portInput.style.display = 'block';
        testButton.style.display = 'block';
    }
});

// Connect button handler
connectButton.addEventListener('click', () => {
    const method = methodSelector.value;
    const ip = ipInput.value || 'localhost';
    const port = parseInt(portInput.value) || 8080;

    switch (method) {
        case 'json':
            fileInput.click();
            break;

        case 'http':
            integration.startHttpPolling(ip, port, 100);  // 100ms = 10 FPS
            break;

        case 'websocket':
            integration.startWebSocket(ip, port);
            break;

        default:
            alert('Please select a connection method');
    }
});

// Stop button handler
stopButton.addEventListener('click', () => {
    integration.stopAll();
    statusElement.textContent = 'Stopped';
    statusElement.style.background = '#757575';
});

// Test button handler
testButton.addEventListener('click', async () => {
    const ip = ipInput.value || 'localhost';
    const port = parseInt(portInput.value) || 8080;

    statusElement.textContent = 'Testing connection...';
    statusElement.style.background = '#FF9800';

    const isReachable = await integration.testHttpConnection(ip, port);

    if (isReachable) {
        statusElement.textContent = `✅ Server reachable at ${ip}:${port}`;
        statusElement.style.background = '#4CAF50';
    } else {
        statusElement.textContent = `❌ Cannot reach ${ip}:${port}`;
        statusElement.style.background = '#F44336';
    }

    setTimeout(() => {
        statusElement.textContent = 'Ready';
        statusElement.style.background = '#2196F3';
    }, 3000);
});

// Setup JSON file loading
const canvas = document.getElementById('frameCanvas');
if (canvas) {
    integration.setupJsonLoading(fileInput, canvas);
}

// Instructions panel
const instructions = document.createElement('div');
instructions.style.cssText = `
    position: fixed;
    top: 60px;
    right: 20px;
    background: white;
    padding: 15px;
    border-radius: 10px;
    box-shadow: 0 4px 20px rgba(0,0,0,0.15);
    max-width: 300px;
    font-size: 12px;
    z-index: 999;
`;
instructions.innerHTML = `
    <h3 style="margin: 0 0 10px 0;">📱 Android Integration</h3>
    <p style="margin: 5px 0;"><strong>Method 1: JSON File</strong><br>
    Export frame in Android app → Load file here</p>
    <p style="margin: 5px 0;"><strong>Method 2: HTTP Server</strong><br>
    Start server in app → Enter IP → Connect</p>
    <p style="margin: 5px 0;"><strong>Method 3: WebSocket</strong><br>
    Real-time streaming with auto-reconnect</p>
    <button onclick="this.parentElement.style.display='none'"
            style="margin-top: 10px; padding: 5px 10px; background: #2196F3; color: white; border: none; border-radius: 5px; cursor: pointer;">
        Got it!
    </button>
`;
document.body.appendChild(instructions);

// Keyboard shortcuts
document.addEventListener('keydown', (e) => {
    if (e.key === '1') {
        methodSelector.value = 'json';
        methodSelector.dispatchEvent(new Event('change'));
    } else if (e.key === '2') {
        methodSelector.value = 'http';
        methodSelector.dispatchEvent(new Event('change'));
    } else if (e.key === '3') {
        methodSelector.value = 'websocket';
        methodSelector.dispatchEvent(new Event('change'));
    } else if (e.key === ' ' || e.key === 'Enter') {
        connectButton.click();
    } else if (e.key === 'Escape') {
        stopButton.click();
    }
});

console.log('💡 Keyboard shortcuts:');
console.log('  1 = JSON File');
console.log('  2 = HTTP Server');
console.log('  3 = WebSocket');
console.log('  Space/Enter = Connect');
console.log('  Escape = Stop');
console.log('');
console.log('🚀 Ready to receive frames from Android!');
