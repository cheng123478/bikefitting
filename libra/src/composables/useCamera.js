/**
 * @file useCamera.js
 * @description Composable for managing MediaPipe Camera lifecycle and configuration.
 */
import { ref } from 'vue';
import { Camera } from '@mediapipe/camera_utils';

export function useCamera(videoElement, options = {}) {
    const isRunning = ref(false);
    let camera = null;

    const defaultOptions = {
        width: 1280,
        height: 720,
        ...options
    };

    /**
     * Starts the camera stream.
     * @param {Function} onFrame - Callback to execute on each video frame.
     */
    const startCamera = (onFrame) => {
        if (isRunning.value || !videoElement.value) return;

        camera = new Camera(videoElement.value, {
            onFrame: async () => {
                if (onFrame) await onFrame();
            },
            width: defaultOptions.width,
            height: defaultOptions.height,
        });

        camera.start();
        isRunning.value = true;
    };

    /**
     * Stops the camera stream and releases resources.
     */
    const stopCamera = () => {
        if (camera) {
            camera.stop();
            camera = null;
        }
        isRunning.value = false;
    };

    return {
        isRunning,
        startCamera,
        stopCamera
    };
}
