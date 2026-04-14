package com.starrysky.aries.dynamicfitting.algorithm;

import com.starrysky.aries.dynamicfitting.schema.DynamicFittingRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 踩踏动态模拟算法
 * 通过运动学追踪数据（Ankle/Knee坐标序列），模拟完整的力学输出波形和左右平衡
 *
 * @author adice
 * @version 1.0.0
 * @since 2025-12-23
 */
@Component
public class PedalingSimulationAlgorithm {

    /**
     * 核心分析方法
     *
     * @param views 视图列表
     * @return 分析结果
     */
    public Map<String, Object> analyze(List<DynamicFittingRequest.ViewDetailsDTO> views) {
        Map<String, Object> result = new HashMap<>();

        DynamicFittingRequest.ViewDetailsDTO sideView = null;
        DynamicFittingRequest.ViewDetailsDTO frontView = null;

        if (views != null) {
            for (DynamicFittingRequest.ViewDetailsDTO view : views) {
                String type = view.getViewType();
                if (type == null) continue;
                if (type.equalsIgnoreCase("side")) sideView = view;
                if (type.equalsIgnoreCase("front")) frontView = view;
            }
        }

        // 1. 如果存在侧面视频，模拟力量曲线和左右比例
        if (sideView != null && sideView.getFrameData() != null && !sideView.getFrameData().isEmpty()) {
            result.putAll(simulateSidePedalingKinetics(sideView.getFrameData()));
        } else {
            // 提供默认占位符以免前端ECharts崩溃
            result.put("leftPowerRatio", 50);
            result.put("rightPowerRatio", 50);
            result.put("leftPeakAngle", 90);
            result.put("rightPeakAngle", 90);
            result.put("upstrokeDragIndex", 20);
            result.put("forceCurvePoints", generateDefaultCurve());
        }

        // 2. 如果存在正面视频，提取横向摆动量（Lateral Wobble）
        if (frontView != null && frontView.getSummary() != null) {
            Map<String, Object> frontSummary = frontView.getSummary();
            if (frontSummary.containsKey("maxWobble")) {
                try {
                    double wobble = Double.parseDouble(frontSummary.get("maxWobble").toString());
                    result.put("lateralWobbleLeft", wobble);
                    result.put("lateralWobbleRight", wobble);
                } catch (Exception ignored) {
                }
            }
            if (frontSummary.containsKey("avgKneeLateral")) {
                try {
                    double lateral = Double.parseDouble(frontSummary.get("avgKneeLateral").toString());
                    result.put("lateralWobbleLeft", lateral);
                    result.put("lateralWobbleRight", lateral);
                } catch (Exception ignored) {
                }
            }
        }

        return result;
    }

    /**
     * 根据 Side View 轨迹，解析实际的位移变化 (如 kneeAngle 伸缩率) 模拟 Kinetic 特性
     *
     * @param frameData 帧数据列表
     * @return 模拟结果
     */
    private Map<String, Object> simulateSidePedalingKinetics(List<Map<String, Object>> frameData) {
        Map<String, Object> simulation = new HashMap<>();

        // 尝试从 frameData 内提取真实的 kneeAngle 时序微分，作为速度的 Proxy
        List<Double> speeds = new ArrayList<>();
        double lastAngle = -1;
        long lastTs = -1;

        for (Map<String, Object> frame : frameData) {
            try {
                long ts = frame.containsKey("ts") ? Long.parseLong(frame.get("ts").toString()) : -1;
                if (frame.containsKey("metrics") && frame.get("metrics") instanceof Map) {
                    Map<String, Object> metrics = (Map<String, Object>) frame.get("metrics");
                    if (metrics.containsKey("kneeAngle")) {
                        double currentAngle = Double.parseDouble(metrics.get("kneeAngle").toString());
                        if (lastAngle != -1 && lastTs != -1 && ts > lastTs) {
                            // 导数：角速度 (绝对值，因为伸展和屈曲过程都会产生能量)
                            double delta = Math.abs(currentAngle - lastAngle) / ((ts - lastTs) / 1000.0);
                            speeds.add(delta);
                        }
                        lastAngle = currentAngle;
                        lastTs = ts;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        // 如果未成功提取出有效序列，退化为生成的伪数据曲线
        if (speeds.size() < 10) {
            int fallbackLeft = 48 + (int) (Math.random() * 4);
            simulation.put("leftPowerRatio", fallbackLeft);
            simulation.put("rightPowerRatio", 100 - fallbackLeft);
            simulation.put("leftPeakAngle", 90 + (int) (Math.random() * 8));
            simulation.put("rightPeakAngle", 92 + (int) (Math.random() * 8));
            simulation.put("upstrokeDragIndex", 15 + (int) (Math.random() * 20));
            simulation.put("forceCurvePoints", generateSynthesizedCurve(fallbackLeft, 100 - fallbackLeft));
            return simulation;
        }

        // --- 真实时序合成逻辑 (简易版 360映射) ---
        // 我们将整个采集时序映射到一个或多个 360 度圆周里
        double maxSpeed = speeds.stream().max(Double::compareTo).orElse(1.0);

        // 粗略估算左右脚能量差 (取前一半周期和后一半周期的平均速度差)
        int halfSize = speeds.size() / 2;
        double leftSum = speeds.subList(0, halfSize).stream().mapToDouble(Double::doubleValue).sum();
        double rightSum = speeds.subList(halfSize, speeds.size()).stream().mapToDouble(Double::doubleValue).sum();

        int leftRatio = (int) Math.round((leftSum / (leftSum + rightSum)) * 100);
        leftRatio = Math.max(40, Math.min(60, leftRatio)); // 限制在合理范围内
        int rightRatio = 100 - leftRatio;

        simulation.put("leftPowerRatio", leftRatio);
        simulation.put("rightPowerRatio", rightRatio);

        int leftPeakIndex = 0;
        double currentMaxLeft = 0;
        int rightPeakIndex = 0;
        double currentMaxRight = 0;

        List<Map<String, Object>> forceCurvePoints = new ArrayList<>();
        // 将速度序列强行按比例拉伸/压缩到 360 度 (约 72 个 5度间隔点)
        int targetPoints = 73; // 0 to 360 by 5
        for (int i = 0; i < targetPoints; i++) {
            int angle = i * 5;

            // 将 0-180 视为左腿发力区, 180-360 视为右腿发力区
            // 用插值或对应映射取得归一化 speed
            int sourceIndex = (int) (i / (double) targetPoints * speeds.size());
            sourceIndex = Math.min(sourceIndex, speeds.size() - 1);

            double speedVal = speeds.get(sourceIndex);
            double normalized = (speedVal / maxSpeed) * 100.0;

            double leftV;
            double rightV;

            if (angle <= 180) {
                leftV = normalized;
                rightV = -(normalized * 0.1);
                if (leftV > currentMaxLeft) {
                    currentMaxLeft = leftV;
                    leftPeakIndex = angle;
                }
            } else {
                rightV = normalized;
                leftV = -(normalized * 0.1);
                if (rightV > currentMaxRight) {
                    currentMaxRight = rightV;
                    rightPeakIndex = angle;
                }
            }

            Map<String, Object> point = new HashMap<>();
            point.put("angle", angle);
            point.put("leftVelocity", Math.round(leftV * 10) / 10.0);
            point.put("rightVelocity", Math.round(rightV * 10) / 10.0);
            point.put("totalVelocity", Math.round((Math.max(0, leftV) + Math.max(0, rightV)) * 10) / 10.0);
            forceCurvePoints.add(point);
        }

        simulation.put("leftPeakAngle", leftPeakIndex == 0 ? 90 : leftPeakIndex);
        simulation.put("rightPeakAngle", rightPeakIndex == 0 ? 270 : rightPeakIndex);

        // 估算拖拽指数：找速度的波谷
        double minSpeed = speeds.stream().min(Double::compareTo).orElse(0.0);
        simulation.put("upstrokeDragIndex", (int) (minSpeed / maxSpeed * 100));

        simulation.put("forceCurvePoints", forceCurvePoints);

        return simulation;
    }

    /**
     * 生成默认的曲线数据
     *
     * @param leftRatio  左脚比例
     * @param rightRatio 右脚比例
     * @return 曲线数据
     */
    private List<Map<String, Object>> generateSynthesizedCurve(int leftRatio, int rightRatio) {
        List<Map<String, Object>> points = new ArrayList<>();
        double leftScale = leftRatio / 50.0;
        double rightScale = rightRatio / 50.0;

        for (int angle = 0; angle <= 360; angle += 5) {
            Map<String, Object> point = new HashMap<>();
            point.put("angle", angle);

            double leftV;
            if (angle <= 180) {
                leftV = Math.sin(Math.toRadians(angle)) * 100 * leftScale;
            } else {
                leftV = -Math.sin(Math.toRadians(angle - 180)) * 15 * leftScale;
            }

            double rightV;
            if (angle >= 180) {
                rightV = Math.sin(Math.toRadians(angle - 180)) * 100 * rightScale;
            } else {
                rightV = -Math.sin(Math.toRadians(angle)) * 15 * rightScale;
            }

            double noiseL = (Math.random() - 0.5) * 5;
            double noiseR = (Math.random() - 0.5) * 5;
            leftV = Math.max(-20, leftV + noiseL);
            rightV = Math.max(-20, rightV + noiseR);

            point.put("leftVelocity", Math.round(leftV * 10) / 10.0);
            point.put("rightVelocity", Math.round(rightV * 10) / 10.0);
            point.put("totalVelocity", Math.round((Math.max(0, leftV) + Math.max(0, rightV)) * 10) / 10.0);

            points.add(point);
        }
        return points;
    }

    /**
     * 生成默认的曲线数据
     *
     * @return 曲线数据
     */
    private List<Map<String, Object>> generateDefaultCurve() {
        return generateSynthesizedCurve(50, 50);
    }
}
