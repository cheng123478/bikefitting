package com.starrysky.aries.dynamicfitting.algorithm;

import com.starrysky.aries.dynamicfitting.schema.DynamicFittingRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 踩踏运动学分析算法 v2.0
 *
 * <p><b>数据来源优先级（已修正科学性问题）：</b>
 * <ol>
 *   <li><b>正面视频（FRONT）→ 双侧分析（高可信度）</b>：
 *       同时追踪 L_KNEE / R_KNEE 的 Y 坐标时序，独立测量左右腿振幅，
 *       直接计算双侧发力比与踩踏对称性。</li>
 *   <li><b>侧面视频（SIDE）→ 整体踏频补充（中等可信度，对称假设）</b>：
 *       侧面仅追踪单侧腿运动，无法区分左右腿。
 *       优先取前端已做播放速度校正的 cadenceRpm，
 *       并假设左右 50/50 生成力量曲线。</li>
 *   <li><b>无有效数据 → 返回 null</b>：前端据此隐藏踩踏分析模块，
 *       不展示随机伪造数据。</li>
 * </ol>
 *
 * <p><b>关于侧面视频为何无法区分双腿：</b><br>
 * 侧面拍摄时，MediaPipe 仅追踪可见度最高的单侧骨骼点（currentSide），
 * 摄像头视野内只有一条腿的运动轨迹，另一条腿被遮挡，
 * 因此无论对时序如何切分，侧面数据均不包含双腿独立信息。
 *
 * @author adice
 * @version 2.0.0
 * @since 2026-04-16
 */
@Component
public class PedalingSimulationAlgorithm {

    /**
     * 核心分析入口
     *
     * @param views 各视角数据列表
     * @return 踩踏分析结果 Map，若无有效数据则返回 null（前端应据此隐藏该模块）
     */
    public Map<String, Object> analyze(List<DynamicFittingRequest.ViewDetailsDTO> views) {
        if (views == null || views.isEmpty()) return null;

        DynamicFittingRequest.ViewDetailsDTO sideView = null;
        DynamicFittingRequest.ViewDetailsDTO frontView = null;

        for (DynamicFittingRequest.ViewDetailsDTO view : views) {
            String type = view.getViewType();
            if (type == null) continue;
            if (type.equalsIgnoreCase("side")) sideView = view;
            if (type.equalsIgnoreCase("front")) frontView = view;
        }

        Map<String, Object> result = new HashMap<>();
        boolean bilateralSuccess = false;

        // ── 阶段 1：优先使用正面视频做双侧分析 ────────────────────────────────
        if (hasEnoughFrames(frontView, 30)) {
            Map<String, Object> bilateral = analyzeFrontBilateral(frontView);
            if (bilateral != null) {
                result.putAll(bilateral);
                result.put("bilateralConfidence", "HIGH");
                result.put("dataSource", "front");
                bilateralSuccess = true;
            }
        }

        // ── 阶段 2：正面分析失败，退到侧面单腿分析（50/50 对称假设）──────────
        if (!bilateralSuccess) {
            if (hasEnoughFrames(sideView, 20)) {
                Map<String, Object> sideResult = analyzeSideViewKinetics(sideView.getFrameData());
                result.putAll(sideResult);
                result.put("bilateralConfidence", "LOW");
                result.put("dataSource", "side");
                result.put("analysisNote",
                        "当前基于【侧面视角】分析。由于单侧视频无法区分双腿，系统采用了对称动力学模型模拟双腿输出。此结果可参考踩踏相位（Timing），但无法真实反映左右腿力量差异（Balance）。建议上传正面视频以激活双侧实测模式。");
            } else {
                // 数据完全不足 → 返回 null，让前端隐藏踩踏分析模块
                return null;
            }
        }

        // ── 阶段 3：踏频来源 —— 优先使用前端已做速度校正的 cadenceRpm ─────────
        // 前端（PoseAnalyzer.vue）在 finishSession() 中已将上传模式的时间戳
        // 乘以 ANALYSIS_SPEED（0.2），得到真实视频时长后计算出正确踏频。
        // 后端直接读取该值，不再自行从 ts 重新推导（否则会引入同样的 5 倍误差）。
        if (sideView != null && sideView.getSummary() != null) {
            Object cadence = sideView.getSummary().get("cadenceRpm");
            if (cadence instanceof Number) {
                int rpm = ((Number) cadence).intValue();
                if (rpm > 0 && rpm <= 200) { // 合理性过滤（人体踏频极限约 160 rpm）
                    result.put("cadenceRpm", rpm);
                }
            }
        }

        return result;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  正面视图 - 双侧膝关节 Y 坐标追踪
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * 正面视图双侧分析。
     * 通过 L_KNEE / R_KNEE 的 Y 坐标振幅比估算左右腿发力比。
     * Y 值越大 = 在画面中越低 = 越接近 BDC（踏板下死点）。
     */
    private Map<String, Object> analyzeFrontBilateral(DynamicFittingRequest.ViewDetailsDTO frontView) {
        List<Map<String, Object>> frameData = frontView.getFrameData();

        List<Double> leftKneeY = new ArrayList<>();
        List<Double> rightKneeY = new ArrayList<>();

        for (Map<String, Object> frame : frameData) {
            try {
                Object ptsObj = frame.get("pts");
                if (!(ptsObj instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> pts = (Map<String, Object>) ptsObj;

                Double lY = extractPointCoord(pts, "L_KNEE", "y");
                Double rY = extractPointCoord(pts, "R_KNEE", "y");
                if (lY != null && rY != null) {
                    leftKneeY.add(lY);
                    rightKneeY.add(rY);
                }
            } catch (Exception ignored) {
            }
        }

        if (leftKneeY.size() < 30) return null; // 帧数不足，分析结果不可靠

        // 峰值检测（windowSize=4：适应高踏频短周期）
        List<Integer> leftPeaks = findPeaks(leftKneeY, 4);
        List<Integer> rightPeaks = findPeaks(rightKneeY, 4);

        if (leftPeaks.size() < 2 || rightPeaks.size() < 2) return null;

        // 振幅均值（BDC峰值 - 段内最小值）作为发力强度代理
        double leftAmp = calculateMeanAmplitude(leftKneeY, leftPeaks);
        double rightAmp = calculateMeanAmplitude(rightKneeY, rightPeaks);

        // 左右发力比（限制在 35%–65%）
        double totalAmp = leftAmp + rightAmp;
        int leftRatio = 50;
        if (totalAmp > 0.001) {
            leftRatio = (int) Math.round((leftAmp / totalAmp) * 100.0);
            leftRatio = Math.max(35, Math.min(65, leftRatio));
        }
        int rightRatio = 100 - leftRatio;

        // 提拉阻力指数：谷底越浅（相对振幅）→ 提拉越困难
        double valleyRatio = calculateMeanValleyRatio(leftKneeY, leftPeaks);
        int upstrokeDrag = Math.max(0, Math.min(100, (int) Math.round((1.0 - valleyRatio) * 100)));

        // 构建结果
        Map<String, Object> result = new HashMap<>();
        result.put("leftPowerRatio", leftRatio);
        result.put("rightPowerRatio", rightRatio);
        result.put("leftPeakAngle", 90);   // BDC 通常对应约 90°（曲柄在3点钟位置）
        result.put("rightPeakAngle", 270); // 右腿 BDC 约对应 270°
        result.put("upstrokeDragIndex", upstrokeDrag);
        result.put("forceCurvePoints", generateBilateralCurve(leftRatio, rightRatio));

        // 从正面 summary 提取膝盖横摆量（归一化用骨盆宽度）
        if (frontView.getSummary() != null) {
            Map<String, Object> fs = frontView.getSummary();
            Double wobbleL = extractDoubleFromMap(fs, "maxWobble_L");
            Double wobbleR = extractDoubleFromMap(fs, "maxWobble_R");
            
            if (wobbleL != null && wobbleR != null) {
                result.put("lateralWobbleLeft", Math.round(wobbleL * 10) / 10.0);
                result.put("lateralWobbleRight", Math.round(wobbleR * 10) / 10.0);
            } else {
                Double wobble = extractDoubleFromMap(fs, "maxWobbleRatio");
                if (wobble == null) wobble = extractDoubleFromMap(fs, "maxWobble");
                if (wobble != null) {
                    result.put("lateralWobbleLeft", Math.round(wobble * 10) / 10.0);
                    result.put("lateralWobbleRight", Math.round(wobble * 10) / 10.0);
                }
            }
        }

        return result;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  侧面视图 - 整体运动曲线估算（不区分左右腿）
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * 侧面视图运动学分析。
     * 无法区分左右腿，使用 50/50 对称假设，
     * 从膝角时序提取曲线形状特征。
     */
    private Map<String, Object> analyzeSideViewKinetics(List<Map<String, Object>> frameData) {
        Map<String, Object> result = new HashMap<>();
        List<Double> kneeAngles = new ArrayList<>();

        for (Map<String, Object> frame : frameData) {
            try {
                Object metricsObj = frame.get("metrics");
                if (!(metricsObj instanceof Map)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> metrics = (Map<String, Object>) metricsObj;
                Double angle = extractDouble(metrics, "kneeAngle");
                if (angle != null && angle > 0) {
                    kneeAngles.add(angle);
                }
            } catch (Exception ignored) {
            }
        }

        // 固定 50/50（侧面无双腿信息）
        result.put("leftPowerRatio", 50);
        result.put("rightPowerRatio", 50);
        result.put("leftPeakAngle", 90);
        result.put("rightPeakAngle", 270);

        if (kneeAngles.size() >= 15) {
            // 从膝角峰谷估算提拉阻力指数
            List<Integer> peaks = findPeaks(kneeAngles, 4);
            int drag = 20;
            if (peaks.size() >= 2) {
                double valleyRatio = calculateMeanValleyRatio(kneeAngles, peaks);
                drag = Math.max(0, Math.min(100, (int) Math.round((1.0 - valleyRatio) * 100)));
            }
            result.put("upstrokeDragIndex", drag);
            result.put("forceCurvePoints", generateSymmetricCurveFromAngles(kneeAngles));
        } else {
            result.put("upstrokeDragIndex", 20);
            result.put("forceCurvePoints", generateBilateralCurve(50, 50));
        }

        return result;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  力量曲线生成（确定性，无随机值）
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * 根据左右腿发力比生成确定性双侧力量曲线。
     * 左腿：0°~180° 下压正弦；右腿：180°~360° 下压正弦。
     * 不包含任何随机噪声，保证相同输入总产生相同输出。
     */
    private List<Map<String, Object>> generateBilateralCurve(int leftRatio, int rightRatio) {
        List<Map<String, Object>> points = new ArrayList<>();
        double leftScale = leftRatio / 50.0;
        double rightScale = rightRatio / 50.0;

        for (int angle = 0; angle <= 360; angle += 5) {
            double leftV;
            if (angle <= 180) {
                leftV = Math.sin(Math.toRadians(angle)) * 100.0 * leftScale;
            } else {
                // 提拉阶段：轻微负值（被动阻力）
                leftV = -Math.sin(Math.toRadians(angle - 180)) * 15.0 * leftScale;
            }

            double rightV;
            if (angle >= 180) {
                rightV = Math.sin(Math.toRadians(angle - 180)) * 100.0 * rightScale;
            } else {
                // 提拉阶段：轻微负值
                rightV = -Math.sin(Math.toRadians(angle)) * 15.0 * rightScale;
            }

            leftV = Math.max(-20, leftV);
            rightV = Math.max(-20, rightV);

            Map<String, Object> point = new HashMap<>();
            point.put("angle", angle);
            point.put("leftVelocity", Math.round(leftV * 10) / 10.0);
            point.put("rightVelocity", Math.round(rightV * 10) / 10.0);
            point.put("totalVelocity", Math.round((leftV + rightV) * 10) / 10.0);
            points.add(point);
        }
        return points;
    }

    /**
     * 从侧面膝角时序生成对称力量曲线。
     * 使用实际角度数据修正正弦基形，但保持左右对称。
     */
    private List<Map<String, Object>> generateSymmetricCurveFromAngles(List<Double> kneeAngles) {
        List<Map<String, Object>> points = new ArrayList<>();
        int n = kneeAngles.size();

        for (int i = 0; i <= 72; i++) {
            int angle = i * 5;
            // 将角度映射到原始数据索引
            int srcIdx = Math.min((int) ((double) i / 72.0 * n), n - 1);
            double rawAngle = kneeAngles.get(srcIdx);

            // 角度越大（越接近 BDC）→ 发力越强；归一化修正因子
            double modifier = Math.max(0.6, Math.min(1.5, (rawAngle - 90.0) / 60.0 + 1.0));
            double base = Math.abs(Math.sin(Math.toRadians(angle))) * 100.0 * modifier;

            double leftV = angle <= 180 ? base : -(base * 0.1);
            double rightV = angle > 180 ? base : -(base * 0.1);
            leftV = Math.max(-20, leftV);
            rightV = Math.max(-20, rightV);

            Map<String, Object> point = new HashMap<>();
            point.put("angle", angle);
            point.put("leftVelocity", Math.round(leftV * 10) / 10.0);
            point.put("rightVelocity", Math.round(rightV * 10) / 10.0);
            point.put("totalVelocity", Math.round((leftV + rightV) * 10) / 10.0);
            points.add(point);
        }
        return points;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  信号处理工具方法
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * 简单峰值检测：要求候选点大于其前后 windowSize 帧内的所有值，
     * 且相邻峰值最小间距为 windowSize 帧，避免重复计数。
     */
    private List<Integer> findPeaks(List<Double> data, int windowSize) {
        List<Integer> peaks = new ArrayList<>();
        if (data == null || data.size() < windowSize * 2 + 1) return peaks;

        for (int i = windowSize; i < data.size() - windowSize; i++) {
            double val = data.get(i);
            boolean isPeak = true;
            for (int j = i - windowSize; j <= i + windowSize; j++) {
                if (j != i && data.get(j) >= val) {
                    isPeak = false;
                    break;
                }
            }
            if (isPeak && (peaks.isEmpty() || i - peaks.get(peaks.size() - 1) > windowSize)) {
                peaks.add(i);
            }
        }
        return peaks;
    }

    /**
     * 计算各踩踏周期振幅（峰值 - 段内谷值）的均值。
     * 振幅差越大 → 腿部运动范围越大 → 作为发力强度代理。
     */
    private double calculateMeanAmplitude(List<Double> data, List<Integer> peaks) {
        if (peaks.size() < 2) return 0.0;
        double sumAmp = 0;
        int count = 0;
        for (int i = 0; i < peaks.size() - 1; i++) {
            int start = peaks.get(i);
            int end = peaks.get(i + 1);
            double peakVal = data.get(start);
            double valleyVal = data.subList(start, end).stream()
                    .mapToDouble(Double::doubleValue).min().orElse(peakVal);
            sumAmp += (peakVal - valleyVal);
            count++;
        }
        return count > 0 ? sumAmp / count : 0.0;
    }

    /**
     * 计算谷底相对高度比（谷底值 / 峰值），用于估算提拉阻力。
     * 比值越高 → 谷底越浅 → 提拉越主动（提拉拖拽指数越低）。
     */
    private double calculateMeanValleyRatio(List<Double> data, List<Integer> peaks) {
        if (peaks.size() < 2) return 0.5;
        double sumRatio = 0;
        int count = 0;
        for (int i = 0; i < peaks.size() - 1; i++) {
            int start = peaks.get(i);
            int end = peaks.get(i + 1);
            double peakVal = data.get(start);
            double valleyVal = data.subList(start, end).stream()
                    .mapToDouble(Double::doubleValue).min().orElse(peakVal);
            if (peakVal > 0.001) {
                sumRatio += valleyVal / peakVal;
                count++;
            }
        }
        return count > 0 ? sumRatio / count : 0.5;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  数据提取工具
    // ══════════════════════════════════════════════════════════════════════════

    private boolean hasEnoughFrames(DynamicFittingRequest.ViewDetailsDTO view, int minFrames) {
        return view != null && view.getFrameData() != null && view.getFrameData().size() >= minFrames;
    }

    private Double extractPointCoord(Map<String, Object> pts, String pointKey, String coord) {
        Object pointObj = pts.get(pointKey);
        if (!(pointObj instanceof Map)) return null;
        @SuppressWarnings("unchecked")
        Map<String, Object> ptMap = (Map<String, Object>) pointObj;
        // 可见度过滤：v < 0.4 认为该点不可靠
        Double v = extractDouble(ptMap, "v");
        if (v != null && v < 0.4) return null;
        return extractDouble(ptMap, coord);
    }

    private Double extractDouble(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return null;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try {
            return Double.parseDouble(val.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Double extractDoubleFromMap(Map<String, Object> map, String key) {
        return extractDouble(map, key);
    }
}
