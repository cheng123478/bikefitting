/**
 * @file usePoseDetection.js
 * @description Composable for MediaPipe Pose detection and biomechanical angle analysis.
 */
import { reactive, ref } from 'vue';
import { Pose } from '@mediapipe/pose';

export function usePoseDetection(options = {}) {
    const currentAngles = reactive({ knee: 0, hip: 0, elbow: 0 });
    const feedback = reactive({ visible: false, type: 'normal', message: '', icon: '' });

    let pose = null;

    const defaultOptions = {
        modelComplexity: 1,
        smoothLandmarks: true,
        minDetectionConfidence: 0.5,
        minTrackingConfidence: 0.5,
        ...options
    };

    /**
     * Initializes the Pose detection engine.
     */
    const initPose = (onResultsCallback) => {
        pose = new Pose({
            locateFile: (file) => `/vendor/mediapipe/pose/${file}`
        });

        pose.setOptions(defaultOptions);
        pose.onResults((results) => {
            processResults(results);
            if (onResultsCallback) onResultsCallback(results);
        });
    };

    /**
     * Sends an image frame to the Pose engine for analysis.
     */
    const sendFrame = async (image) => {
        if (pose) await pose.send({ image });
    };

    /**
     * Core result processing: Landmark mapping and angle calculation.
     */
    const processResults = (results) => {
        if (!results.poseLandmarks) {
            resetData();
            return;
        }

        const lm = results.poseLandmarks;
        // Determine orientation (Left/Right side view) based on shoulder visibility
        const isLeftView = lm[11].visibility > lm[12].visibility;

        // Joint mapping indices
        const idx = isLeftView ?
            { shoulder: 11, hip: 23, knee: 25, ankle: 27, elbow: 13, wrist: 15 } :
            { shoulder: 12, hip: 24, knee: 26, ankle: 28, elbow: 14, wrist: 16 };

        // Update real-time metrics
        currentAngles.knee = calculateAngle(lm[idx.hip], lm[idx.knee], lm[idx.ankle]);
        currentAngles.hip = calculateAngleToHorizon(lm[idx.shoulder], lm[idx.hip]);
        currentAngles.elbow = calculateAngle(lm[idx.shoulder], lm[idx.elbow], lm[idx.wrist]);

        analyzeAndFeedback();
    };

    /**
     * Calculates angle between three points (A, B, C) where B is the vertex.
     */
    const calculateAngle = (a, b, c) => {
        const radians = Math.atan2(c.y - b.y, c.x - b.x) - Math.atan2(a.y - b.y, a.x - b.x);
        let angle = Math.abs(radians * 180.0 / Math.PI);
        if (angle > 180.0) angle = 360 - angle;
        return Math.round(angle);
    };

    /**
     * Calculates angle of a line (A to B) relative to the horizontal axis.
     */
    const calculateAngleToHorizon = (a, b) =>
        Math.round(Math.abs(Math.atan2(a.y - b.y, a.x - b.x) * 180.0 / Math.PI));

    /**
     * Generates feedback based on biomechanical thresholds.
     */
    const analyzeAndFeedback = () => {
        feedback.visible = true;
        const { knee, elbow } = currentAngles;

        // Knee Extension Logic
        if (knee > 120) {
            if (knee < 135) {
                setFeedback('warning', 'Saddle too low', 'fas fa-arrow-up');
                return;
            } else if (knee > 155) {
                setFeedback('warning', 'Saddle too high', 'fas fa-arrow-down');
                return;
            }
        }

        // Elbow Lock Logic
        if (elbow > 170) {
            setFeedback('warning', 'Elbows locked', 'fas fa-exclamation-triangle');
            return;
        }

        setFeedback('good', 'Posture good', 'fas fa-check');
    };

    const setFeedback = (type, message, icon) => {
        feedback.type = type;
        feedback.message = message;
        feedback.icon = icon;
    };

    const resetData = () => {
        currentAngles.knee = 0;
        currentAngles.hip = 0;
        currentAngles.elbow = 0;
        feedback.visible = false;
    };

    const closePose = () => {
        if (pose) {
            pose.close();
            pose = null;
        }
    };

    return {
        currentAngles,
        feedback,
        initPose,
        sendFrame,
        closePose,
        resetData
    };
}
