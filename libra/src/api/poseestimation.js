import { ref, shallowRef, unref } from 'vue';
import { Pose } from '@mediapipe/pose';

export function usePoseEstimation(viewModeRef = 'side') {
    let pose = null;
    let currentSide = null; // 当前检测的侧面 ('left' 或 'right')

    const isReady = ref(false);
    const error = ref(null);

    // 使用 shallowRef 避免深层代理导致的性能问题
    const keypoints = shallowRef({});
    // 增加一个双侧关键点存储，用于正面绘图
    const dualKeypoints = shallowRef({ left: {}, right: {} });

    // 此时直接解构出当前的 mode，但逻辑中要支持动态
    const getMode = () => unref(viewModeRef);

    const videoDim = { w: 1, h: 1 };

    const metrics = ref({
        kneeAngle: 0,
        hipAngle: 0,
        torsoAngle: 0,
        ankleAngle: 0,
        reachAngle: 0,
        // 正面专属
        leftKneeAngle: 0,
        rightKneeAngle: 0,
        shoulderTilt: 0,
        hipTilt: 0,
        leftKneeWobble: 0,
        rightKneeWobble: 0,
        leftElbowFlare: 0,
        rightElbowFlare: 0,
        // 侧面极限捕获
        sideMaxKnee: 0,
        sideMinHip: 999,
        kopsOffset: 0,
        ankleAtBdc: 0,
        // 正面极限捕获
        frontMaxHipTilt: 0,
        frontMaxShoulderTilt: 0,
        frontMaxLWobble: 0,
        frontMaxRWobble: 0,
        frontMaxLKnee: 0,
        frontMaxRKnee: 0,
        frontMaxLFlare: 0,
        frontMaxRFlare: 0,
        // 背面专属
        pelvisSway: 0,
        spineTilt: 0,
        leftHipDrop: 0,
        rightHipDrop: 0
    });

    const isCollecting = ref(false);
    const collectedData = ref({
        timestamps: [],
        kneeAngles: [],
        hipAngles: [],
        ankleAngles: [],
        torsoAngles: [],
        // 正面专属数据数组
        leftKneeAngles: [],
        rightKneeAngles: [],
        shoulderTilts: [],
        hipTilts: [],
        leftKneeWobbles: [],
        rightKneeWobbles: [],
        leftElbowFlares: [],
        rightElbowFlares: [],
        // 背面专属数据数组
        pelvisSways: [],
        spineTilts: [],
        leftHipDrops: [],
        rightHipDrops: [],
        triggerMetrics: {
            // Standardized keys: [tag]Val
            bdcVal: -1,
            ankleAtBdc: 0,
            tdcVal: 999,
            k_extVal: -1,
            max_wobbleVal: 0,
            max_tiltVal: 0,
            max_swayVal: 0,
            max_dropVal: 0
        }
    });

    function toBdcPlantarflexionProxy(rawAnkle) {
        if (!Number.isFinite(rawAnkle)) return 0;
        return Number((rawAnkle - 90).toFixed(1));
    }

    // 2. 数据处理回调
    const onResults = (results) => {
        if (!results.poseLandmarks) return;
        if (!isReady.value) isReady.value = true;

        const lm = results.poseLandmarks;
        const extract = (pt) => (pt ? { x: pt.x, y: pt.y, v: pt.visibility } : null);

        const mode = getMode();

        if (mode === 'side') {
            // --- 原有的侧面逻辑 ---
            const leftVisibility = (lm[11]?.visibility || 0) + (lm[13]?.visibility || 0) + (lm[23]?.visibility || 0) + (lm[25]?.visibility || 0) + (lm[27]?.visibility || 0);
            const rightVisibility = (lm[12]?.visibility || 0) + (lm[14]?.visibility || 0) + (lm[24]?.visibility || 0) + (lm[26]?.visibility || 0) + (lm[28]?.visibility || 0);

            let isLeft = leftVisibility > rightVisibility;
            if (currentSide === 'left' && rightVisibility < leftVisibility * 1.1) isLeft = true;
            if (currentSide === 'right' && leftVisibility < rightVisibility * 1.1) isLeft = false;
            currentSide = isLeft ? 'left' : 'right';

            const idx = isLeft ? {
                ear: 7, shoulder: 11, elbow: 13, wrist: 15,
                hip: 23, knee: 25, ankle: 27, heel: 29, toe: 31
            } : {
                ear: 8, shoulder: 12, elbow: 14, wrist: 16,
                hip: 24, knee: 26, ankle: 28, heel: 30, toe: 32
            };

            keypoints.value = {
                ear: extract(lm[idx.ear]),
                shoulder: extract(lm[idx.shoulder]),
                elbow: extract(lm[idx.elbow]),
                wrist: extract(lm[idx.wrist]),
                hip: extract(lm[idx.hip]),
                knee: extract(lm[idx.knee]),
                ankle: extract(lm[idx.ankle]),
                heel: extract(lm[idx.heel]),
                toe: extract(lm[idx.toe])
            };

            const w = videoDim.w;
            const h = videoDim.h;

            const rawKnee = calculateAngle(lm[idx.hip], lm[idx.knee], lm[idx.ankle], w, h);
            const rawHip = calculateAngle(lm[idx.shoulder], lm[idx.hip], lm[idx.knee], w, h);
            const rawAnkle = calculateAngle(lm[idx.knee], lm[idx.ankle], lm[idx.toe], w, h);
            const rawTorso = calculateTorsoAngle(lm[idx.shoulder], lm[idx.hip], w, h);
            const rawElbow = calculateReachAngle(lm[idx.shoulder], lm[idx.hip], lm[idx.wrist], w, h);

            metrics.value.kneeAngle = applySmoothing(rawKnee, 'kneeAngle');
            metrics.value.hipAngle = applySmoothing(rawHip, 'hipAngle');
            metrics.value.torsoAngle = applySmoothing(rawTorso, 'torsoAngle');
            metrics.value.ankleAngle = applySmoothing(rawAnkle, 'ankleAngle');
            metrics.value.reachAngle = applySmoothing(rawElbow, 'reachAngle');

            // --- 极限捕获 (Peak Hold) & 专业动态捕获 ---
            if (rawKnee > metrics.value.sideMaxKnee) {
                metrics.value.sideMaxKnee = Number(rawKnee.toFixed(1));
                // MediaPipe 原始踝角为 ∠KNEE-ANKLE-TOE 内角；业务展示使用跖屈代理角。
                // 约 90° 表示脚掌接近中立位，因此 rawAnkle 109° 会转成约 19° 跖屈量。
                metrics.value.ankleAtBdc = toBdcPlantarflexionProxy(rawAnkle);
            }
            if (rawHip < metrics.value.sideMinHip) {
                metrics.value.sideMinHip = Number(rawHip.toFixed(1));
            }

            if (isCollecting.value) {
                // 侧面：KOPS proxy 捕获 - 找到前足代理点最靠前瞬间，计算膝盖与其水平偏移量
                // ── 单位修正 ──────────────────────────────────────────────────────
                // 旧方案：直接用像素差（× w），受拍摄距离和设备分辨率影响，无法跨设备比较。
                // 新方案：将像素差除以「胫骨长度（膝→踝）的像素长度」，得到不受设备影响的百分比。
                //         胫骨在同一拍摄场景下与整体画面等比缩放，商值（%）始终保持一致。
                // 参考点说明：MediaPipe 无踏板轴/锁片轴点，这里用脚跟→脚尖 70% 处近似前足/锁片区域，
                //            比直接使用脚尖更不受鞋码和脚尖姿态影响，但仍只能作为 2D 视觉代理。
                // 正值 = 膝过偏前（Knee Over）；负值 = 膝偏后（Knee Behind）
                // 参考范围：|kopsOffset| <= 15%。KOPS 在 2D 视频里只能作为前后位置代理，不能单独决定坐垫前后。
                const isFacingRight = lm[idx.shoulder].x > lm[idx.hip].x;
                const footRefX = (lm[idx.heel].x * 0.3) + (lm[idx.toe].x * 0.7);

                // 计算胫骨参考长度（像素）：保留画面比例畸变校正
                const tibiaLenPx = Math.sqrt(
                    Math.pow((lm[idx.knee].x - lm[idx.ankle].x) * w, 2) +
                    Math.pow((lm[idx.knee].y - lm[idx.ankle].y) * h, 2)
                );

                if (isFacingRight) {
                    if (footRefX > collectedData.value.triggerMetrics.max_foot_ref_x) {
                        collectedData.value.triggerMetrics.max_foot_ref_x = footRefX;
                        const rawPx = (lm[idx.knee].x - footRefX) * w; // 正数：膝在前足参考点前方
                        metrics.value.kopsOffset = tibiaLenPx > 5
                            ? Math.round(rawPx / tibiaLenPx * 100 * 10) / 10
                            : 0;
                    }
                } else {
                    if (footRefX < collectedData.value.triggerMetrics.min_foot_ref_x) {
                        collectedData.value.triggerMetrics.min_foot_ref_x = footRefX;
                        const rawPx = (footRefX - lm[idx.knee].x) * w; // 正数：膝在前足参考点前方
                        metrics.value.kopsOffset = tibiaLenPx > 5
                            ? Math.round(rawPx / tibiaLenPx * 100 * 10) / 10
                            : 0;
                    }
                }

                recordFrameData(rawKnee, rawHip, rawAnkle, rawTorso, lm[idx.ankle].y);
            }
        } else if (mode === 'front') {
            // --- 正面视角逻辑 (Frontal Mode) ---
            const leftSide = {
                shoulder: extract(lm[11]), elbow: extract(lm[13]), wrist: extract(lm[15]),
                hip: extract(lm[23]), knee: extract(lm[25]), ankle: extract(lm[27])
            };
            const rightSide = {
                shoulder: extract(lm[12]), elbow: extract(lm[14]), wrist: extract(lm[16]),
                hip: extract(lm[24]), knee: extract(lm[26]), ankle: extract(lm[28])
            };

            dualKeypoints.value = { left: leftSide, right: rightSide };

            // 为了保证 PoseAnalyzer.vue 的 if (keypoints.value.knee) 判断通过
            keypoints.value = leftSide;

            // 1. 计算角度 (Frontal angles using relative math instead of simple 2D angle for more stable readings)
            // Left/Right Knee Angles in frontal view shouldn't use calculateAngle because depth is lost.
            // But we keep the variable for compatibility. We'll use absolute 2D angle but mark it as less reliable
            const leftKnee = calculateAngle(lm[23], lm[25], lm[27], videoDim.w, videoDim.h);
            const rightKnee = calculateAngle(lm[24], lm[26], lm[28], videoDim.w, videoDim.h);

            // 2. 计算晃动 (Tilt) - 改为计算水平面夹角，不受距离远近影响
            const sTilt = calculateTorsoAngle(lm[11], lm[12], videoDim.w, videoDim.h); // 肩膀连线夹角
            const hTilt = calculateTorsoAngle(lm[23], lm[24], videoDim.w, videoDim.h); // 骨盆连线夹角

            // 3. 计算膝盖横向移动轨迹 (膝盖追踪)
            // 采用 Hybrid Robust Approach 中的 算法 2：骨盆归一化偏离比例 (Pelvic-Normalized Ratio)
            const calculateTrackingIndex = (hip, knee, ankle, pelvisW) => {
                if (!hip || !knee || !ankle || pelvisW < 0.001) return 0;
                const sx = hip.x * videoDim.w, sy = hip.y * videoDim.h;
                const ex = ankle.x * videoDim.w, ey = ankle.y * videoDim.h;
                const kx = knee.x * videoDim.w, ky = knee.y * videoDim.h;
                // 膝盖到髋踝连线的垂直距离 (Pixels)
                // 套用点到直线公式: |Ax0 + By0 + C| / sqrt(A^2 + B^2)
                const num = Math.abs((ex - sx)*(sy - ky) - (sx - kx)*(ey - sy));
                const den = Math.sqrt(Math.pow(ex - sx, 2) + Math.pow(ey - sy, 2));
                const devPx = den > 0 ? num / den : 0;
                // 归一化比例 (百分比 %)
                return (devPx / (pelvisW * videoDim.w)) * 100;
            };

            const pelvisW = Math.abs(lm[23].x - lm[24].x);
            // 这里 wobble 变成了“百分比” (例如 10.5 代表 10.5%)
            let lWobble = calculateTrackingIndex(lm[23], lm[25], lm[27], pelvisW);
            let rWobble = calculateTrackingIndex(lm[24], lm[26], lm[28], pelvisW);

            // 补充算法 1 所需：frontal Hip-Knee-Ankle 2D 夹角，直接沿用 leftKnee/rightKnee 即可


            // 4. 计算手肘外张 (Elbow Flare) - 点到直线的距离，并用肩宽归一化
            //    【修复 P13】原算法返回原始像素值(px)，受视频分辨率和拍摄距离影响，无法跨设备比较。
            //    现在除以肩宽归一化为%，使不同视频/设备的结果具有可比性。
            //    理想范围：|flare| < 15% 肩宽
            const calculateFlare = (shoulder, wrist, elbow, w, h) => {
                if (!shoulder || !wrist || !elbow) return 0;
                const sx = shoulder.x * w, sy = shoulder.y * h;
                const wx = wrist.x * w, wy = wrist.y * h;
                const ex = elbow.x * w, ey = elbow.y * h;
                const numerator = Math.abs((wx - sx)*(sy - ey) - (sx - ex)*(wy - sy));
                const denominator = Math.sqrt(Math.pow(wx - sx, 2) + Math.pow(wy - sy, 2));
                return denominator > 0 ? numerator / denominator : 0;
            };
            // 肩宽作为归一化参照
            const shoulderWidthPx = Math.abs(lm[11].x - lm[12].x) * videoDim.w;

            const lFlareRaw = calculateFlare(lm[11], lm[15], lm[13], videoDim.w, videoDim.h);
            const rFlareRaw = calculateFlare(lm[12], lm[16], lm[14], videoDim.w, videoDim.h);

            const lFlare = shoulderWidthPx > 1 ? (lFlareRaw / shoulderWidthPx) * 100 : 0;
            const rFlare = shoulderWidthPx > 1 ? (rFlareRaw / shoulderWidthPx) * 100 : 0;

            // 平滑处理并赋值
            metrics.value.leftKneeAngle = applySmoothing(leftKnee, 'leftKneeAngle');
            metrics.value.rightKneeAngle = applySmoothing(rightKnee, 'rightKneeAngle');
            metrics.value.shoulderTilt = applySmoothing(sTilt, 'shoulderTilt');
            metrics.value.hipTilt = applySmoothing(hTilt, 'hipTilt');
            metrics.value.leftKneeWobble = applySmoothing(lWobble, 'leftKneeWobble', 0.4);
            metrics.value.rightKneeWobble = applySmoothing(rWobble, 'rightKneeWobble', 0.4);
            metrics.value.leftElbowFlare = applySmoothing(lFlare, 'leftElbowFlare', 0.4);
            metrics.value.rightElbowFlare = applySmoothing(rFlare, 'rightElbowFlare', 0.4);

            // --- 正面极限捕获 (Frontal Peak Hold) ---
            if (Math.abs(hTilt) > Math.abs(metrics.value.frontMaxHipTilt)) {
                metrics.value.frontMaxHipTilt = Number(hTilt.toFixed(1));
            }
            if (Math.abs(sTilt) > Math.abs(metrics.value.frontMaxShoulderTilt)) {
                metrics.value.frontMaxShoulderTilt = Number(sTilt.toFixed(1));
            }
            if (Math.abs(lWobble) > Math.abs(metrics.value.frontMaxLWobble)) {
                metrics.value.frontMaxLWobble = Number(lWobble.toFixed(1));
            }
            if (Math.abs(rWobble) > Math.abs(metrics.value.frontMaxRWobble)) {
                metrics.value.frontMaxRWobble = Number(rWobble.toFixed(1));
            }
            if (leftKnee > metrics.value.frontMaxLKnee) {
                metrics.value.frontMaxLKnee = Number(leftKnee.toFixed(1));
            }
            if (rightKnee > metrics.value.frontMaxRKnee) {
                metrics.value.frontMaxRKnee = Number(rightKnee.toFixed(1));
            }
            if (lFlare > metrics.value.frontMaxLFlare) {
                metrics.value.frontMaxLFlare = Number(lFlare.toFixed(1));
            }
            if (rFlare > metrics.value.frontMaxRFlare) {
                metrics.value.frontMaxRFlare = Number(rFlare.toFixed(1));
            }

            if (isCollecting.value) {
                recordFrontalFrameData(leftKnee, rightKnee);
            }
        } else if (mode === 'back') {
            // --- 背面视角逻辑 (Back Mode) ---
            const leftSide = {
                shoulder: extract(lm[11]), elbow: extract(lm[13]), wrist: extract(lm[15]),
                hip: extract(lm[23]), knee: extract(lm[25]), ankle: extract(lm[27])
            };
            const rightSide = {
                shoulder: extract(lm[12]), elbow: extract(lm[14]), wrist: extract(lm[16]),
                hip: extract(lm[24]), knee: extract(lm[26]), ankle: extract(lm[28])
            };

            dualKeypoints.value = { left: leftSide, right: rightSide };
            // 为了保证 PoseAnalyzer.vue 的 if (keypoints.value.knee) 判断通过
            keypoints.value = leftSide;

            // 计算背面特征数据

            // 骨盆宽度作为参照物
            const pelvisWidth = Math.abs(lm[23].x - lm[24].x) || 0.01;
            const hipCenter = {
                x: (lm[23].x + lm[24].x) / 2,
                y: (lm[23].y + lm[24].y) / 2
            };
            const shoulderCenter = {
                x: (lm[11].x + lm[12].x) / 2,
                y: (lm[11].y + lm[12].y) / 2
            };

            // 1. 骨盆摇摆 (Pelvis Sway)：提取 Y轴骨盆高度差的绝对值，并用骨盆宽归一化
            //    【修复 P13】原算法 `|hipY_L - hipY_R| * h` 返回原始像素(px)，受视频分辨率和拍摄距离影响。
            //    现在除以骨盆宽归一化为%，与 hipDrop 使用相同的参照系，使结果跨设备可比。
            //    理想范围：< 10% 骨盆宽
            const swayPx = Math.abs(lm[23].y - lm[24].y) * videoDim.h;
            const pelvisWidthPx = Math.abs(lm[23].x - lm[24].x) * videoDim.w;
            const sway = pelvisWidthPx > 1 ? (swayPx / pelvisWidthPx) * 100 : 0;

            // 2. 脊柱侧偏角 (Spine Tilt)
            const spineAngleRaw = Math.atan2(hipCenter.y - shoulderCenter.y, hipCenter.x - shoulderCenter.x) * 180 / Math.PI;
            const sTilt = Math.abs(spineAngleRaw - 90);

            // 3. 左右骨盆下坠极值 (Hip Drop)
            const lDrop = (lm[23].y - lm[24].y) / pelvisWidth * 100;
            const rDrop = (lm[24].y - lm[23].y) / pelvisWidth * 100;

            // 4. 肩部水平高低差 (Shoulder Tilt / Asymmetry)
            const sdy = (lm[11].y - lm[12].y) * videoDim.h;
            const sdx = (lm[11].x - lm[12].x) * videoDim.w;
            let rawShoulderAngle = Math.atan2(sdy, sdx) * 180 / Math.PI;
            let asymmetryAngle = Math.abs(rawShoulderAngle);
            if (asymmetryAngle > 90) asymmetryAngle = 180 - asymmetryAngle; // 将角度规范到水平夹角 (0~90)

            metrics.value.pelvisSway = applySmoothing(sway, 'pelvisSway', 0.4);
            metrics.value.spineTilt = applySmoothing(sTilt, 'spineTilt', 0.4);
            metrics.value.leftHipDrop = applySmoothing(Math.max(0, lDrop), 'leftHipDrop', 0.4);
            metrics.value.rightHipDrop = applySmoothing(Math.max(0, rDrop), 'rightHipDrop', 0.4);
            metrics.value.shoulderTilt = applySmoothing(asymmetryAngle, 'shoulderTilt', 0.4);

            if (isCollecting.value) {
                recordBackFrameData();
            }
        }
    };

    // 背面模式专用数据记录
    const recordBackFrameData = () => {
        const dual = dualKeypoints.value;
        const currentMetrics = metrics.value;
        collectedData.value.frameData.push({
            fIdx: collectedData.value.frameData.length,
            ts: Date.now() - collectedData.value.startTime,
            pts: {
                L_SHOULDER: dual.left.shoulder, R_SHOULDER: dual.right.shoulder,
                L_HIP: dual.left.hip, R_HIP: dual.right.hip,
                L_KNEE: dual.left.knee, R_KNEE: dual.right.knee,
                L_ANKLE: dual.left.ankle, R_ANKLE: dual.right.ankle
            },
            metrics: {
                pelvisSway: currentMetrics.pelvisSway,
                spineTilt: currentMetrics.spineTilt,
                leftHipDrop: currentMetrics.leftHipDrop,
                rightHipDrop: currentMetrics.rightHipDrop,
                shoulderTilt: currentMetrics.shoulderTilt
            }
        });

        // 背面抓拍条件：当 pelvis sway 到达极值时
        if (metrics.value.pelvisSway > collectedData.value.triggerMetrics.max_swayVal) {
            collectedData.value.triggerMetrics.max_swayVal = metrics.value.pelvisSway;
            if (onSnapshotNeeded) onSnapshotNeeded('max_sway');
        }
        // 骨盆下坠极值
        const maxDrop = Math.max(metrics.value.leftHipDrop, metrics.value.rightHipDrop);
        if (maxDrop > collectedData.value.triggerMetrics.max_dropVal) {
            collectedData.value.triggerMetrics.max_dropVal = maxDrop;
            if (onSnapshotNeeded) onSnapshotNeeded('max_drop');
        }
    };

    // 正面模式专用数据记录
    const recordFrontalFrameData = (leftKnee, rightKnee) => {
        const dual = dualKeypoints.value;
        const currentMetrics = metrics.value;
        collectedData.value.frameData.push({
            fIdx: collectedData.value.frameData.length,
            ts: Date.now() - collectedData.value.startTime,
            pts: {
                L_SHOULDER: dual.left.shoulder, R_SHOULDER: dual.right.shoulder,
                L_HIP: dual.left.hip, R_HIP: dual.right.hip,
                L_KNEE: dual.left.knee, R_KNEE: dual.right.knee,
                L_ANKLE: dual.left.ankle, R_ANKLE: dual.right.ankle
            },
            metrics: {
                leftKneeAngle: currentMetrics.leftKneeAngle,
                rightKneeAngle: currentMetrics.rightKneeAngle,
                shoulderTilt: currentMetrics.shoulderTilt,
                hipTilt: currentMetrics.hipTilt,
                leftKneeWobble: currentMetrics.leftKneeWobble,
                rightKneeWobble: currentMetrics.rightKneeWobble,
                leftElbowFlare: currentMetrics.leftElbowFlare,
                rightElbowFlare: currentMetrics.rightElbowFlare
            }
        });

        // 1. 膝角最大伸展 (BDC 类似物)
        const maxCurrentKnee = Math.max(leftKnee || 0, rightKnee || 0);
        if (maxCurrentKnee > collectedData.value.triggerMetrics.k_extVal) {
            collectedData.value.triggerMetrics.k_extVal = maxCurrentKnee;
            if (onSnapshotNeeded) onSnapshotNeeded('k_ext');
        }

        // 2. 膝盖摆动最剧烈瞬间
        const maxWobble = Math.max(Math.abs(metrics.value.leftKneeWobble), Math.abs(metrics.value.rightKneeWobble));
        if (maxWobble > collectedData.value.triggerMetrics.max_wobbleVal) {
            collectedData.value.triggerMetrics.max_wobbleVal = maxWobble;
            if (onSnapshotNeeded) onSnapshotNeeded('max_wobble');
        }

        // 3. 躯干倾斜最剧烈瞬间
        const maxTilt = Math.max(Math.abs(metrics.value.shoulderTilt), Math.abs(metrics.value.hipTilt));
        if (maxTilt > collectedData.value.triggerMetrics.max_tiltVal) {
            collectedData.value.triggerMetrics.max_tiltVal = maxTilt;
            if (onSnapshotNeeded) onSnapshotNeeded('max_tilt');
        }
    };

    // 3. 收集每一帧的数据
    const recordFrameData = (knee, hip, ankle, torso, ankleY) => {
        if (isNaN(knee) || isNaN(hip)) return;

        const pts = keypoints.value;
        collectedData.value.frameData.push({
            fIdx: collectedData.value.frameData.length,
            ts: Date.now() - collectedData.value.startTime,
            pts: {
                EAR: pts.ear, SHOULDER: pts.shoulder, WRIST: pts.wrist, ELBOW: pts.elbow,
                HIP: pts.hip, KNEE: pts.knee, ANKLE: pts.ankle, HEEL: pts.heel, TOE: pts.toe
            },
            metrics: {
                kneeAngle: Number(knee.toFixed(1)),
                hipAngle: Number(hip.toFixed(1)),
                ankleAngle: Number(ankle.toFixed(1)),
                torsoAngle: Number(torso.toFixed(1)),
                ankleY: Number(ankleY.toFixed(3))
            }
        });

        // BDC 抓拍
        if (knee > collectedData.value.triggerMetrics.bdcVal) {
            collectedData.value.triggerMetrics.bdcVal = knee;
            collectedData.value.triggerMetrics.ankleAtBdc = toBdcPlantarflexionProxy(ankle);
            if (onSnapshotNeeded) onSnapshotNeeded('bdc');
        }

        // TDC 抓拍
        if (hip < collectedData.value.triggerMetrics.tdcVal) {
            collectedData.value.triggerMetrics.tdcVal = hip;
            if (onSnapshotNeeded) onSnapshotNeeded('tdc');
        }
    };

    const SMOOTHING_FACTOR = 0.8;

    const prevMetrics = {
        kneeAngle: 0, hipAngle: 0, torsoAngle: 0, ankleAngle: 0, reachAngle: 0,
        leftKneeAngle: 0, rightKneeAngle: 0, shoulderTilt: 0, hipTilt: 0,
        leftKneeWobble: 0, rightKneeWobble: 0,
        leftElbowFlare: 0, rightElbowFlare: 0,
        pelvisSway: 0, spineTilt: 0, leftHipDrop: 0, rightHipDrop: 0
    };

    let onSnapshotNeeded = null;
    const setSnapshotCallback = (cb) => { onSnapshotNeeded = cb; };

    // 1. 初始化模型
    const initPose = async () => {
        try {
            const poseInstance = new Pose({
                locateFile: (file) => `https://cdn.jsdelivr.net/npm/@mediapipe/pose/${file}`
            });

            poseInstance.setOptions({
                modelComplexity: 2,
                smoothLandmarks: true,
                enableSegmentation: false,
                smoothSegmentation: false,
                minDetectionConfidence: 0.6,
                minTrackingConfidence: 0.6
            });

            poseInstance.onResults(onResults);
            await poseInstance.initialize();

            pose = poseInstance;
            console.log("MediaPipe Pose Initialized (" + getMode() + " mode)");
        } catch (e) {
            error.value = "AI 模型加载失败: " + e.message;
            console.error(e);
        }
    };

    const sendFrame = async (videoElement) => {
        if (videoElement && (videoDim.w !== videoElement.videoWidth || videoDim.h !== videoElement.videoHeight)) {
            videoDim.w = videoElement.videoWidth || 1;
            videoDim.h = videoElement.videoHeight || 1;
        }
        if (pose) await pose.send({ image: videoElement });
    };

    const startCollection = () => {
        collectedData.value = {
            startTime: Date.now(),
            frameData: [],
            triggerMetrics: {
                bdcVal: -1, ankleAtBdc: 0, tdcVal: 999, max_foot_ref_x: -999, min_foot_ref_x: 999,
                k_extVal: -1, max_wobbleVal: 0, max_tiltVal: 0,
                max_swayVal: 0, max_dropVal: 0
            }
        };
        metrics.value.sideMaxKnee = 0;
        metrics.value.sideMinHip = 999;
        metrics.value.kopsOffset = 0;
        metrics.value.ankleAtBdc = 0;
        metrics.value.frontMaxHipTilt = 0;
        metrics.value.frontMaxShoulderTilt = 0;
        metrics.value.frontMaxLWobble = 0;
        metrics.value.frontMaxRWobble = 0;
        metrics.value.frontMaxLKnee = 0;
        metrics.value.frontMaxRKnee = 0;
        metrics.value.frontMaxLFlare = 0;
        metrics.value.frontMaxRFlare = 0;
        isCollecting.value = true;
    };

    const stopCollection = () => {
        isCollecting.value = false;
        // 关键点：移除冗余的 getAvg, getExtreme, getAbsMax 等未被调用的本地函数
        const mode = getMode();
        return collectedData.value;
    };

    // --- 数学算法 ---

    const calculateAngle = (a, b, c, w = 1, h = 1) => {
        if (!a || !b || !c) return 0;
        const radians = Math.atan2((c.y - b.y) * h, (c.x - b.x) * w) -
            Math.atan2((a.y - b.y) * h, (a.x - b.x) * w);
        let angle = Math.abs(radians * 180.0 / Math.PI);
        if (angle > 180.0) angle = 360 - angle;
        return angle;
    };

    // 【侧面 Reach 角度】：以 Shoulder 为顶点，计算躯干向量(Shoulder→Hip) 与手臂向量(Shoulder→Wrist) 的绝对夹角
    // 正确区间为 85°-95°（手臂近乎垂直于躯干）
    const calculateReachAngle = (shoulder, hip, wrist, w = 1, h = 1) => {
        if (!shoulder || !hip || !wrist) return 0;
        const vTorso = { x: (hip.x - shoulder.x) * w, y: (hip.y - shoulder.y) * h };
        const vArm   = { x: (wrist.x - shoulder.x) * w, y: (wrist.y - shoulder.y) * h };
        const dot    = vTorso.x * vArm.x + vTorso.y * vArm.y;
        const magT   = Math.sqrt(vTorso.x ** 2 + vTorso.y ** 2);
        const magA   = Math.sqrt(vArm.x ** 2 + vArm.y ** 2);
        if (magT < 1e-6 || magA < 1e-6) return 0;
        const cosVal = Math.max(-1, Math.min(1, dot / (magT * magA)));
        return Math.acos(cosVal) * (180 / Math.PI);
    };

    // 躯干/连线夹角计算（消除画面比例畸变，须传入真实宽高）
    // 返回值含义：所传入两点连线向量相对于「水平线」的夹角，范围 0°~90°。
    //   侧面用途：shoulder→hip 向量的俯仰角（0°=水平趴伏/极度流线型；90°=竖直直立）
    //   正/背面用途：左右肩连线或左右髋连线与水平线的侧倾角
    // ⚠ 说明：0° 表示完全水平，90° 表示完全竖直。
    //          Road 骑行建议目标 40°~50°（即相对水平面有 40~50° 的俯身倾斜）。
    const calculateTorsoAngle = (shoulder, hip, w = 1, h = 1) => {
        if (!shoulder || !hip) return 0;
        // 取绝对值，消除左右骑行方向的影响
        const dy = Math.abs(shoulder.y - hip.y) * h;
        const dx = Math.abs(shoulder.x - hip.x) * w;
        // atan2(dy, dx)：dy 为竖直分量，dx 为水平分量 → 结果即水平面夹角
        const radians = Math.atan2(dy, dx);
        return radians * 180.0 / Math.PI;
    };

    const applySmoothing = (newVal, key, factor = SMOOTHING_FACTOR) => {
        const prev = prevMetrics[key] || 0;
        const smoothed = (newVal * (1 - factor)) + (prev * factor);
        prevMetrics[key] = smoothed;
        return Math.round(smoothed * 10) / 10;
    };

    const closePose = () => {
        if (pose) {
            pose.close();
            pose = null;
        }
    };

    return {
        initPose, closePose, sendFrame, isReady, error, keypoints, dualKeypoints, metrics,
        startCollection, stopCollection, setSnapshotCallback
    };
}
