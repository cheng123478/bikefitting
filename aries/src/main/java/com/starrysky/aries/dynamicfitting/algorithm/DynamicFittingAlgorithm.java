package com.starrysky.aries.dynamicfitting.algorithm;

import com.starrysky.aries.dynamicfitting.schema.DynamicFittingResultVO;
import com.starrysky.aries.dynamicfitting.schema.DynamicFittingAdviceVO;
import com.starrysky.aries.dynamicfitting.repository.DynamicFittingStandardRepository;
import com.starrysky.aries.entity.DynamicFittingStandard;
import com.starrysky.aries.util.BikeType;

import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 动态 Fitting 诊断算法引擎 - 数据库驱动扩展版
 * 支持多车型 (Road, MTB, TT, Gravel, City) 诊断标准，通过数据库灵活配置标准值。
 *
 * @author adice
 * @version 1.0
 * @since 2025-08-08 11:19:57
 */
@Component
public class DynamicFittingAlgorithm {

    @Resource
    private DynamicFittingStandardRepository standardRepository;

    /**
     * 各车型双侧角度类对称性偏差阈值（°）。
     * 车型越追求运动控制效率，容忍的双侧角度不对称程度越小。
     *
     * <p>
     * <b>Key</b>: {@link com.starrysky.aries.util.BikeType#code}（与
     * DynamicFittingStandard.bikeType 字段一致）<br>
     * <b>Value</b>: 允许的最大左右偏差角度（°），超出则生成对称性告警建议。
     *
     * <p>
     * 若车型不在此 Map 中，则使用 {@link #DEFAULT_BILATERAL_THRESHOLD} 兜底。
     */
    private static final Map<Integer, Double> BILATERAL_ASYMMETRY_THRESHOLDS;
    static {
        Map<Integer, Double> m = new HashMap<>();
        m.put(BikeType.ROAD_BIKE.code, 5.0); // ROAD_BIKE (公路车) : 追求效率，阈值严格
        m.put(BikeType.MOUNTAIN_BIKE.code, 8.0); // MOUNTAIN_BIKE(山地车) : 地形崎岖，阈值宽松
        m.put(BikeType.GRAVEL_BIKE.code, 7.0); // GRAVEL_BIKE (全地形公路车): 综合用途，适中
        m.put(BikeType.CITY_BIKE.code, 10.0); // CITY_BIKE (城市通勤车) : 舒适优先，最宽松
        m.put(BikeType.TT_BIKE.code, 3.0); // TT_BIKE (铁三/计时车): 对称性要求高，最严格
        BILATERAL_ASYMMETRY_THRESHOLDS = Collections.unmodifiableMap(m);
    }
    /** 未知车型的双侧对称性告警阈值默认值（°） */
    private static final double DEFAULT_BILATERAL_THRESHOLD = 8.0;

    /**
     * 核心诊断逻辑
     *
     * @param bikeType 车辆类型 (通过 BikeType 枚举获取 code)
     * @param summary  汇总数据 (包含各视角的统计中值)
     */
    public DynamicFittingResultVO analyze(Integer bikeType, Map<String, Object> summary) {
        Map<String, Object> safeSummary = summary != null ? summary : Collections.emptyMap();
        // 从数据库检索该车型的所有指标标准；缺失项使用保守的文献/主流平台兜底范围，避免空配置直接满分。
        List<DynamicFittingStandard> standards = resolveStandards(bikeType);

        List<DynamicFittingAdviceVO> advices = new ArrayList<>();
        double totalWeightedScore = 0;
        double totalWeight = 0;

        // 记录所有找到的指标，用于后续对称性分析
        Map<String, Double> detectedMetrics = new java.util.HashMap<>();
        Map<String, String> standardRanges = new java.util.HashMap<>();

        for (DynamicFittingStandard std : standards) {
            String metricKey = std.getMetricKey();
            String viewType = std.getViewType();

            // 视角感知检索指标值
            Double val;
            if (viewType != null && !viewType.isEmpty()) {
                val = findMetricInView(safeSummary, viewType.toLowerCase(), metricKey);
            } else {
                val = findMetricInSummary(safeSummary, metricKey);
            }

            if (val != null) {
                val = normalizeMetricValueForStandard(metricKey, val, std);
                detectedMetrics.put(metricKey, val);
                standardRanges.put(metricKey, String.format("%.1f-%.1f", std.getMinVal(), std.getMaxVal()));

                // 计算该指标得分 (非对称)
                double score = calculateAsymmetricScore(val, std);
                double weight = std.getWeight() != null ? std.getWeight() : 0.1;

                totalWeightedScore += score * weight;
                totalWeight += weight;

                // 生成调整建议 (带优先级和顺序)
                if (val < std.getMinVal() || val > std.getMaxVal()) {
                    String adviceCode = val < std.getMinVal() ? std.getLowAdvice()
                            : std.getHighAdvice();
                    String adviceContent = val < std.getMinVal() ? std.getLowAdviceContent()
                            : std.getHighAdviceContent();
                    advices.add(createAdvice(std, val, adviceCode, adviceContent));
                }
            }
        }

        // --- 2. 双侧对称性分析 (Bilateral Asymmetry) ---
        // 展平所有视角的指标以供双侧对比，不局限于数据库中配置的指标
        Map<String, Double> allMetrics = new HashMap<>();
        for (Object viewSum : safeSummary.values()) {
            if (viewSum instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) viewSum;
                map.forEach((k, v) -> {
                    Double val = convertToDouble(v);
                    if (val != null) {
                        allMetrics.put(k, val);
                    }
                });
            }
        }
        performBilateralAudit(allMetrics, advices, bikeType, standardRanges);

        // 3. 结果排序：按专业调整顺序 (锁片 -> 座垫 -> 车把)
        advices.sort(java.util.Comparator.comparing(DynamicFittingAdviceVO::getSequence)
                .thenComparing(DynamicFittingAdviceVO::getImportance));

        // 4. 最终匹配得分
        BigDecimal finalScore = totalWeight > 0
                ? BigDecimal.valueOf(totalWeightedScore / totalWeight).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(100.0);

        return DynamicFittingResultVO.builder()
                .matchScore(finalScore)
                .advices(advices)
                .standardRanges(standardRanges)
                .build();
    }

    /**
     * 创建调整建议
     *
     * @param std           标准
     * @param val           指标值
     * @param adviceCode    建议代码
     * @param adviceContent 建议内容
     * @return 建议
     */
    private DynamicFittingAdviceVO createAdvice(DynamicFittingStandard std, Double val, String adviceCode,
            String adviceContent) {
        // 获取顺序: 1:Cleat, 2:Seat, 3:Handlebar/Stem
        int sequence = 3;
        if ("cleat".equalsIgnoreCase(std.getPart()) || "crank".equalsIgnoreCase(std.getPart()))
            sequence = 1;
        else if ("seat".equalsIgnoreCase(std.getPart()))
            sequence = 2;

        // 获取重要性: 偏离程度越大越重要
        double deviation = Math.abs(val - (std.getMinVal() + std.getMaxVal()) / 2.0);
        double range = (std.getMaxVal() - std.getMinVal()) / 2.0;
        int importance = (deviation > range * 1.5) ? 1 : 2;

        return DynamicFittingAdviceVO.builder()
                .part(std.getPart())
                .type("adjust")
                .code(adviceCode)
                .adviceContent(adviceContent)
                .metric(std.getMetricKey())
                .val(val)
                .range(std.getMinVal() + "-" + std.getMaxVal())
                .importance(importance)
                .sequence(sequence)
                .build();
    }

    private List<DynamicFittingStandard> resolveStandards(Integer bikeType) {
        List<DynamicFittingStandard> dbStandards = standardRepository != null
                ? standardRepository.findByBikeType(bikeType)
                : Collections.emptyList();

        List<DynamicFittingStandard> fallback = fallbackStandards();
        if (dbStandards == null || dbStandards.isEmpty()) {
            return fallback;
        }

        List<DynamicFittingStandard> merged = new ArrayList<>(dbStandards);
        java.util.Set<String> configuredKeys = new java.util.HashSet<>();
        for (DynamicFittingStandard std : dbStandards) {
            configuredKeys.add(standardIdentity(std));
        }
        for (DynamicFittingStandard std : fallback) {
            if (!configuredKeys.contains(standardIdentity(std))) {
                merged.add(std);
            }
        }
        return merged;
    }

    private static String standardIdentity(DynamicFittingStandard std) {
        String viewType = std.getViewType() != null ? std.getViewType().toLowerCase() : "";
        return viewType + ":" + std.getMetricKey();
    }

    private static List<DynamicFittingStandard> fallbackStandards() {
        List<DynamicFittingStandard> standards = new ArrayList<>();
        standards.add(standard("side", "avgTorso", 40.0, 50.0, 0.10, "handlebar",
                "躯干角偏小，建议复核把位落差、把立长度与核心稳定性。",
                "躯干角偏大，建议复核把位高度与骑行姿态目标。"));
        standards.add(standard("side", "minHip", 40.0, 55.0, 0.08, "seat",
                "上死点髋角偏小，建议复核坐垫高度、曲柄长度与髋部活动度。",
                "上死点髋角偏大，建议复核坐垫高度和踩踏顶部控制。"));
        standards.add(standard("side", "maxHip", 70.0, 80.0, 0.06, "seat",
                "下压阶段髋部伸展不足，建议复核坐垫高度和髋部活动范围。",
                "髋角偏大，建议复核坐垫高度并观察骨盆是否下沉。"));
        standards.add(standard("side", "maxKnee", 140.0, 150.0, 0.16, "seat",
                "膝部伸展不足，建议优先复核座高。",
                "膝部伸展角偏大，建议优先复核座高并关注膝后侧反馈。"));
        standards.add(standard("side", "ankleAtBdc", 10.0, 25.0, 0.06, "cleat",
                "下死点跖屈代理角偏小，建议复核锁片前后、鞋底支撑和脚跟下坠习惯。",
                "下死点跖屈代理角偏大，建议复核锁片位置、鞋底支撑和脚尖下压习惯。"));
        standards.add(standard("side", "kopsOffset", -10.0, 10.0, 0.08, "seat",
                "KOPS 代理值偏后，建议结合曲柄相位和踏板轴复核坐垫前后位置。",
                "KOPS 代理值偏前，建议结合曲柄相位和踏板轴复核坐垫前后位置。"));
        standards.add(standard("side", "cadenceRpm", 80.0, 100.0, 0.04, "crank",
                "踏频偏低，建议尝试更轻齿比与稳定节奏训练。",
                "踏频偏高，建议复核齿比与骨盆稳定性。"));
        standards.add(standard("front", "maxWobble", 0.0, 15.0, 0.10, "cleat",
                "膝部横向偏移在参考范围内。",
                "膝部横向偏移偏大，建议复核锁片角度、鞋垫支撑与髋部控制。"));
        standards.add(standard("front", "maxElbowFlare", 0.0, 15.0, 0.05, "handlebar",
                "手肘外张在参考范围内。",
                "手肘外张偏大，建议复核车把宽度和上身紧张度。"));
        standards.add(standard("front", "maxShoulderTilt", 0.0, 5.0, 0.05, "handlebar",
                "肩部偏角在参考范围内。",
                "肩部偏角偏大，建议复核握把压力和躯干稳定性。"));
        standards.add(standard("front", "maxHipTilt", 0.0, 5.0, 0.06, "seat",
                "骨盆侧倾在参考范围内。",
                "骨盆侧倾偏大，建议复核座高、坐垫支撑和核心稳定性。"));
        standards.add(standard("back", "maxPelvisSway", 0.0, 10.0, 0.06, "seat",
                "骨盆横摆在参考范围内。",
                "骨盆横摆偏大，建议复核座高和核心稳定性。"));
        standards.add(standard("back", "maxSpineTilt", 0.0, 5.0, 0.05, "handlebar",
                "脊柱侧向偏移在参考范围内。",
                "脊柱侧向偏移偏大，建议复核坐姿对称性和拍摄角度。"));
        standards.add(standard("back", "maxHipDrop_L", 0.0, 8.0, 0.04, "seat",
                "左侧骨盆下坠在参考范围内。",
                "左侧骨盆下坠偏大，建议复核座高和单侧髋部控制。"));
        standards.add(standard("back", "maxHipDrop_R", 0.0, 8.0, 0.04, "seat",
                "右侧骨盆下坠在参考范围内。",
                "右侧骨盆下坠偏大，建议复核座高和单侧髋部控制。"));
        standards.add(standard("side", "kneeAnglePhaseStdDev", 0.0, 4.0, 0.04, "crank",
                "膝角相位一致性在参考范围内。",
                "膝角相位一致性不足，建议练习稳定踏频和圆踩。"));
        standards.add(standard("side", "hipAnglePhaseStdDev", 0.0, 4.0, 0.03, "crank",
                "髋角相位一致性在参考范围内。",
                "髋角相位一致性不足，建议复核髋部控制与踏频稳定性。"));
        standards.add(standard("side", "ankleAnglePhaseStdDev", 0.0, 5.0, 0.03, "cleat",
                "踝角相位一致性在参考范围内。",
                "踝角相位一致性不足，建议复核锁片位置和脚踝参与方式。"));
        return standards;
    }

    private static DynamicFittingStandard standard(String viewType, String metricKey, double minVal, double maxVal,
            double weight, String part, String lowAdviceContent, String highAdviceContent) {
        DynamicFittingStandard standard = new DynamicFittingStandard();
        standard.setViewType(viewType);
        standard.setMetricKey(metricKey);
        standard.setMinVal(minVal);
        standard.setMaxVal(maxVal);
        standard.setWeight(weight);
        standard.setSensitivity(3.0);
        standard.setPart(part);
        standard.setLowAdvice("advice.dynamic." + metricKey + ".low");
        standard.setHighAdvice("advice.dynamic." + metricKey + ".high");
        standard.setLowAdviceContent(lowAdviceContent);
        standard.setHighAdviceContent(highAdviceContent);
        return standard;
    }

    private double normalizeMetricValueForStandard(String metricKey, double value, DynamicFittingStandard standard) {
        if (!"ankleAtBdc".equals(metricKey) || standard == null
                || standard.getMinVal() == null || standard.getMaxVal() == null) {
            return value;
        }
        if (standard.getMaxVal() <= 45.0 && value > 60.0) {
            return value - 90.0;
        }
        if (standard.getMinVal() >= 60.0 && value <= 45.0) {
            return value + 90.0;
        }
        return value;
    }

    /**
     * 非对称得分计算
     *
     * @param val 指标值
     * @param std 标准
     * @return 得分
     */
    private double calculateAsymmetricScore(double val, DynamicFittingStandard std) {
        if (val >= std.getMinVal() && val <= std.getMaxVal())
            return 100.0;

        double center = (std.getMinVal() + std.getMaxVal()) / 2.0;
        double range = (std.getMaxVal() - std.getMinVal()) / 2.0;
        if (range <= 0)
            range = 2.0;

        double deviation = Math.abs(val - center) - range;

        // 选择敏感系数: 过高风险通常更大
        double sensitivity = std.getSensitivity() != null ? std.getSensitivity() : 3.0;
        if (val < std.getMinVal() && std.getLowSensitivity() != null) {
            sensitivity = std.getLowSensitivity();
        } else if (val > std.getMaxVal() && std.getHighSensitivity() != null) {
            sensitivity = std.getHighSensitivity();
        }

        double score = 100.0 - (Math.pow(deviation, 1.2) * sensitivity);
        return Math.max(0, score);
    }

    /**
     * 执行双侧对比分析。
     * 根据车型从 {@link #BILATERAL_ASYMMETRY_THRESHOLDS} 动态查取阈值，
     * 避免对所有车型硬编码同一个 8° 阈值。
     *
     * @param metrics  指标值映射（key 须带 _L / _R 后缀的才会参与对称性判断）
     * @param advices  调整建议列表，检测到不对称时追加告警条目
     * @param bikeType 车辆类型代号，用于查取对应阈值
     */
    private void performBilateralAudit(Map<String, Double> metrics,
            List<DynamicFittingAdviceVO> advices,
            Integer bikeType,
            Map<String, String> standardRanges) {
        // 根据车型查取阈值，未知车型使用默认值
        metrics.forEach((key, val) -> {
            if (key.endsWith("_L")) {
                String rightKey = key.replace("_L", "_R");
                if (metrics.containsKey(rightKey)) {
                    String baseMetric = key.substring(0, key.length() - 2);
                    MetricDescriptor descriptor = describeBilateralMetric(baseMetric, bikeType);
                    if (descriptor == null) return;

                    double leftVal = val;
                    double rightVal = metrics.get(rightKey);
                    double delta = Math.abs(leftVal - rightVal);

                    if (delta > descriptor.threshold) {
                        String adviceText = String.format(
                                "检测到双侧【%s】不平衡（左右差值 %.1f%s，超过参考容差 %.0f%s）。这通常提示相关肌群控制差异、核心稳定性不足、视频视角误差，或锁片/坐垫设置造成的代偿。建议先复核拍摄角度，再结合疼痛反馈与专业 fitting 检查。",
                                descriptor.metricName, delta, descriptor.unit, descriptor.threshold, descriptor.unit);
                                
                        String rangeStr = String.format("双侧差值 < %.0f%s", descriptor.threshold, descriptor.unit);
                        standardRanges.put(baseMetric, rangeStr);
                                
                        advices.add(DynamicFittingAdviceVO.builder()
                                .part("body")
                                .type("check")
                                .code("advice.asymmetry.warning." + baseMetric)
                                .adviceContent(adviceText)
                                .metric(baseMetric)
                                .val(delta)
                                .range(rangeStr)
                                .importance(1)
                                .sequence(0)
                                .build()); // sequence=0：双侧不对称告警最先展示
                    }
                }
            }
        });
    }

    private MetricDescriptor describeBilateralMetric(String baseMetric, Integer bikeType) {
        String key = baseMetric.toLowerCase();
        if (key.contains("cv") || key.contains("variance")) {
            return null;
        }
        if (key.contains("stddev")) {
            return new MetricDescriptor("双侧控制稳定性", 3.0, "°");
        }
        if (key.contains("wobble")) {
            return new MetricDescriptor("膝部横向偏移", 5.0, "%");
        }
        if (key.contains("hipdrop")) {
            return new MetricDescriptor("骨盆下坠幅度", 5.0, "%");
        }
        double angleThreshold = BILATERAL_ASYMMETRY_THRESHOLDS.getOrDefault(
                bikeType, DEFAULT_BILATERAL_THRESHOLD);
        if (key.contains("knee")) {
            return new MetricDescriptor("膝部伸展角度", angleThreshold, "°");
        }
        if (key.contains("elbow")) {
            return new MetricDescriptor("手肘外张角度", 5.0, "%");
        }
        return null;
    }

    private static class MetricDescriptor {
        private final String metricName;
        private final double threshold;
        private final String unit;

        private MetricDescriptor(String metricName, double threshold, String unit) {
            this.metricName = metricName;
            this.threshold = threshold;
            this.unit = unit;
        }
    }

    /**
     * 在特定视角中查找指标
     *
     * @param overallSummary 总览摘要
     * @param viewType       视角类型
     * @param metricKey      指标键
     * @return 指标值
     */
    private Double findMetricInView(Map<String, Object> overallSummary, String viewType, String metricKey) {
        Object viewData = overallSummary.get(viewType);
        if (viewData instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) viewData;
            return extractMetricValue(map, metricKey);
        }
        return null;
    }

    /**
     * 全局搜索指标 (回退逻辑)
     *
     * @param overallSummary 总览摘要
     * @param metricKey      指标键
     * @return 指标值
     */
    private Double findMetricInSummary(Map<String, Object> overallSummary, String metricKey) {
        for (Object viewSum : overallSummary.values()) {
            if (viewSum instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) viewSum;
                Double val = extractMetricValue(map, metricKey);
                if (val != null)
                    return val;
            }
        }
        return null;
    }

    /**
     * 从 Map 中根据 key 提取数值 (支持多种后缀)
     *
     * @param map       映射
     * @param metricKey 指标键
     * @return 数值
     */
    private Double extractMetricValue(Map<String, Object> map, String metricKey) {
        // 优先级 1: 精确匹配
        if (map.containsKey(metricKey))
            return convertToDouble(map.get(metricKey));

        // 优先级 2: 统计后缀匹配
        if (map.containsKey(metricKey + "Median"))
            return convertToDouble(map.get(metricKey + "Median"));
        if (map.containsKey(metricKey + "Mean"))
            return convertToDouble(map.get(metricKey + "Mean"));
        if (map.containsKey(metricKey + "Val"))
            return convertToDouble(map.get(metricKey + "Val"));
        // 【修复 P14】稳定性指标（StdDev/CV/Variance）参与诊断
        if (map.containsKey(metricKey + "StdDev"))
            return convertToDouble(map.get(metricKey + "StdDev"));
        if (map.containsKey(metricKey + "CV"))
            return convertToDouble(map.get(metricKey + "CV"));
        if (map.containsKey(metricKey + "Variance"))
            return convertToDouble(map.get(metricKey + "Variance"));

        return null;
    }

    /**
     * 将对象转换为 Double 类型
     *
     * @param obj 对象
     * @return Double 类型值
     */
    private Double convertToDouble(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        if (obj instanceof String) {
            try {
                return Double.parseDouble((String) obj);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

}
