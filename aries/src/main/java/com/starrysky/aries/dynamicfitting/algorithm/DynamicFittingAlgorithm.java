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
import java.util.Set;

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
     * 各车型的横向偏移类指标百分比阈值（wobble/hipDrop 等）。
     * 与角度阈值类似，车型越竞技，允许的偏移百分比越小。
     * 注意：此为百分比阈值，独立于角度阈值 BILATERAL_ASYMMETRY_THRESHOLDS。
     */
    private static final Map<Integer, Double> LATERAL_DEVIATION_PCT_THRESHOLDS;
    static {
        Map<Integer, Double> m = new HashMap<>();
        m.put(BikeType.ROAD_BIKE.code, 6.0);
        m.put(BikeType.MOUNTAIN_BIKE.code, 8.0);
        m.put(BikeType.GRAVEL_BIKE.code, 7.0);
        m.put(BikeType.CITY_BIKE.code, 10.0);
        m.put(BikeType.TT_BIKE.code, 5.0);
        LATERAL_DEVIATION_PCT_THRESHOLDS = Collections.unmodifiableMap(m);
    }
    private static final double DEFAULT_LATERAL_PCT_THRESHOLD = 8.0;
    private static final Set<String> OBSERVATION_ONLY_METRICS = Set.of("cadenceRpm");

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
            std = normalizeBusinessStandard(std);
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

                if (OBSERVATION_ONLY_METRICS.contains(metricKey)) {
                    continue;
                }

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

        // 计算检测覆盖率：记录共多少标准，实际检测到多少
        int totalStandards = standards.size();
        int detectedCount = detectedMetrics.size();

        return DynamicFittingResultVO.builder()
                .matchScore(finalScore)
                .advices(advices)
                .standardRanges(standardRanges)
                .detectedMetricsCount(detectedCount)
                .totalStandardMetricsCount(totalStandards)
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

    private DynamicFittingStandard normalizeBusinessStandard(DynamicFittingStandard std) {
        if (std == null || std.getMetricKey() == null) {
            return std;
        }

        DynamicFittingStandard normalized = copyStandard(std);
        switch (std.getMetricKey()) {
            case "maxKnee":
                // 参考Holmes (1994), Bini (2011) 文献：多数研究建议不超过150°以减少膝后侧损伤风险
                // 公路车/铁三更严格，这里统一使用145-150范围，比原155更保守安全
                normalized.setMinVal(145.0);
                normalized.setMaxVal(150.0);
                break;
            case "ankleAtBdc":
                // 跖屈代理角：Silberman (2005) 建议10-25度，保留0-30以容纳个体差异
                normalized.setMinVal(0.0);
                normalized.setMaxVal(30.0);
                normalized.setWeight(Math.min(defaultDouble(std.getWeight(), 0.04), 0.04));
                break;
            case "kopsOffset":
                // KOPS标准：主流平台建议±10mm范围，这里保留-15+15但权重适当降低
                normalized.setMinVal(-15.0);
                normalized.setMaxVal(15.0);
                normalized.setWeight(Math.min(defaultDouble(std.getWeight(), 0.05), 0.05));
                break;
            case "cadenceRpm":
                // 经济性踏频80-100，保留70-110以容纳骑行风格差异
                normalized.setMinVal(70.0);
                normalized.setMaxVal(110.0);
                normalized.setWeight(0.0);
                break;
            case "maxHipTilt":
            case "maxShoulderTilt":
            case "maxSpineTilt":
                // 2D视觉测量有噪声，保留7度阈值
                normalized.setMinVal(0.0);
                normalized.setMaxVal(7.0);
                break;
            case "spineRockingVar":
                normalized.setMinVal(0.0);
                normalized.setMaxVal(5.0);
                break;
            default:
                return std;
        }
        return normalized;
    }

    private DynamicFittingStandard copyStandard(DynamicFittingStandard std) {
        DynamicFittingStandard copy = new DynamicFittingStandard();
        copy.setId(std.getId());
        copy.setBikeType(std.getBikeType());
        copy.setViewType(std.getViewType());
        copy.setMetricKey(std.getMetricKey());
        copy.setMinVal(std.getMinVal());
        copy.setMaxVal(std.getMaxVal());
        copy.setWeight(std.getWeight());
        copy.setSensitivity(std.getSensitivity());
        copy.setLowSensitivity(std.getLowSensitivity());
        copy.setHighSensitivity(std.getHighSensitivity());
        copy.setPart(std.getPart());
        copy.setLowAdvice(std.getLowAdvice());
        copy.setHighAdvice(std.getHighAdvice());
        copy.setLowAdviceContent(std.getLowAdviceContent());
        copy.setHighAdviceContent(std.getHighAdviceContent());
        return copy;
    }

    private double defaultDouble(Double value, double defaultValue) {
        return value != null ? value : defaultValue;
    }

    private static List<DynamicFittingStandard> fallbackStandards() {
        List<DynamicFittingStandard> standards = new ArrayList<>();
        standards.add(standard("side", "avgTorso", 40.0, 50.0, 0.10, "handlebar",
                "躯干角度偏小（姿势偏低趴），建议检查把立高度和长度，确保上半身不至于过度拉伸。",
                "躯干角度偏大（姿势偏直立），建议检查把位高度，如需更低趴姿势可逐步调整。"));
        standards.add(standard("side", "minHip", 40.0, 55.0, 0.08, "seat",
                "上死点髋角偏小，大腿过于贴近腹部，影响呼吸和舒适度。建议检查坐垫高度和曲柄长度。",
                "上死点髋角偏大，踩踏顶部控制偏弱。建议检查坐垫高度，练习单腿踩踏改善顶部控制。"));
        standards.add(standard("side", "maxHip", 70.0, 80.0, 0.06, "seat",
                "下压阶段髋部伸展不足，踩踏发力不充分。建议检查坐垫高度和髋部活动范围。",
                "髋角偏大，骨盆可能在下沉。建议检查坐垫高度，踩踏时注意保持骨盆水平。"));
        standards.add(standard("side", "maxKnee", 145.0, 150.0, 0.16, "seat",
                "膝部伸展不足，膝盖偏弯曲。建议优先检查座高，座高可能偏低。",
                "膝部伸展过大，膝盖接近锁死。建议优先检查座高并关注膝盖后方是否有不适感。"));
        standards.add(standard("side", "ankleAtBdc", 0.0, 30.0, 0.04, "cleat",
                "下死点脚踝角度偏小，脚跟偏下沉。建议检查锁片前后位置和鞋底支撑，注意踩踏时脚跟不要过度下坠。",
                "下死点脚踝角度偏大，脚尖下压偏多。建议检查锁片位置和鞋底支撑，注意踩踏时脚掌平稳过渡。"));
        standards.add(standard("side", "kopsOffset", -15.0, 15.0, 0.05, "seat",
                "膝盖位置偏后（在踏板轴后方），建议结合曲柄相位检查坐垫前后位置是否过靠后。",
                "膝盖位置偏前（超过踏板轴），建议结合曲柄相位检查坐垫前后位置是否过靠前。"));
        standards.add(standard("side", "cadenceRpm", 70.0, 110.0, 0.00, "crank",
                "踏频偏低，腿部负担较重。建议尝试更轻齿比，目标 80-90 RPM，保持稳定节奏。",
                "踏频偏高，可能引起骨盆跳动。建议检查齿比，收紧核心，保持骨盆稳定。"));
        standards.add(standard("front", "maxWobble", 0.0, 15.0, 0.10, "cleat",
                "膝盖横向偏移在参考范围内，力线表现良好。",
                "膝盖横向偏移偏大，踩踏时膝盖轨迹不是直线。建议检查锁片角度、鞋垫支撑和髋部控制。"));
        standards.add(standard("front", "maxElbowFlare", 0.0, 15.0, 0.05, "handlebar",
                "手肘外张在参考范围内，姿态良好。",
                "手肘外张偏大，会增加风阻。建议检查车把宽度是否合适，并注意放松上半身、肘部内收。"));
        standards.add(standard("front", "maxShoulderTilt", 0.0, 7.0, 0.04, "handlebar",
                "肩部偏角在参考范围内，两侧基本对称。",
                "肩部偏角偏大，左右肩膀高度不一致。建议检查握把是否对称发力，加强弱侧肩胛稳定性。"));
        standards.add(standard("front", "maxHipTilt", 0.0, 7.0, 0.05, "seat",
                "骨盆侧倾在参考范围内，骨盆稳定性良好。",
                "骨盆侧倾偏大，踩踏时骨盆左右晃动。建议检查座高是否过高、坐垫支撑是否足够，同时加强核心和臀中肌力量。"));
        standards.add(standard("back", "maxPelvisSway", 0.0, 10.0, 0.06, "seat",
                "骨盆横摆在参考范围内，稳定性良好。",
                "骨盆横摆偏大，踩踏时骨盆左右晃动明显。建议检查座高和核心稳定性，多做核心训练。"));
        standards.add(standard("back", "maxSpineTilt", 0.0, 7.0, 0.04, "handlebar",
                "脊柱中线偏移在参考范围内。",
                "脊柱中线偏移偏大，可能存在左右发力不均。建议检查坐姿对称性，必要时咨询物理治疗师。"));
        standards.add(standard("back", "spineRockingVar", 0.0, 5.0, 0.04, "handlebar",
                "脊柱侧偏波动在参考范围内，稳定性良好。",
                "脊柱侧偏波动偏大，踩踏中脊柱晃动明显。建议加强核心稳定性，保持匀速踏频。"));
        standards.add(standard("back", "maxShoulderTilt", 0.0, 7.0, 0.04, "handlebar",
                "高低肩偏角在参考范围内。",
                "高低肩偏角偏大，左右肩膀高度不一致。建议检查握把是否对称发力，加强肩胛稳定训练。"));
        standards.add(standard("back", "hipDrop_L", 0.0, 8.0, 0.04, "seat",
                "左侧骨盆下坠在参考范围内。",
                "左侧骨盆下坠偏大，踩踏时左侧骨盆下沉偏多。建议检查座高，加强左侧臀中肌力量。"));
        standards.add(standard("back", "hipDrop_R", 0.0, 8.0, 0.04, "seat",
                "右侧骨盆下坠在参考范围内。",
                "右侧骨盆下坠偏大，踩踏时右侧骨盆下沉偏多。建议检查座高，加强右侧臀中肌力量。"));
        standards.add(standard("side", "kneeAnglePhaseStdDev", 0.0, 4.0, 0.04, "crank",
                "膝角相位一致性在参考范围内，踩踏节奏稳定。",
                "膝角相位一致性不足，每圈踩踏膝角变化不一致。建议练习稳定踏频和「画圆」踩踏。"));
        standards.add(standard("side", "hipAnglePhaseStdDev", 0.0, 4.0, 0.03, "crank",
                "髋角相位一致性在参考范围内。",
                "髋角相位一致性不足，髋部动作不够稳定。建议保持匀速踏频，加强髋部控制。"));
        standards.add(standard("side", "ankleAnglePhaseStdDev", 0.0, 5.0, 0.03, "cleat",
                "踝角相位一致性在参考范围内。",
                "踝角相位一致性不足，脚踝在踩踏中姿态不够稳定。建议检查锁片位置，练习脚踝平滑过渡。"));
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
        // 跖屈代理角转换：标准范围在0-30度时，若value在90-120度范围则判断为旧版内角格式
        // 旧版 ∠KNEE-ANKLE-TOE 内角约90-120°，新版跖屈代理角 = 内角 - 90
        // 使用更严格的阈值范围避免误判：内角低于80度理论上不可行，跖屈代理角高于50度也不合理
        if (standard.getMaxVal() <= 45.0 && value > 80.0 && value <= 125.0) {
            return value - 90.0;
        }
        if (standard.getMinVal() >= 60.0 && value >= -45.0 && value <= 45.0) {
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
                                "检测到双侧【%s】不平衡（左右差值 %.1f%s，超过参考容差 %.0f%s）。这可能意味着左右腿发力不均、核心稳定性不足、或锁片/坐垫设置存在偏差。建议先确认拍摄角度是否正面居中，再结合骑行中的不适感与专业 fitting 检查综合判断。",
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
        if (key.contains("avgknee") || key.contains("kneevariance")) {
            return null;
        }
        double angleThreshold = BILATERAL_ASYMMETRY_THRESHOLDS.getOrDefault(
                bikeType, DEFAULT_BILATERAL_THRESHOLD);
        if (key.contains("wobble")) {
            // 膝部横向偏移：使用独立的百分比阈值，车型越竞技越严格
            double pctThreshold = LATERAL_DEVIATION_PCT_THRESHOLDS.getOrDefault(
                    bikeType, DEFAULT_LATERAL_PCT_THRESHOLD);
            return new MetricDescriptor("膝部横向偏移", pctThreshold, "%");
        }
        if (key.contains("hipdrop")) {
            // 骨盆下坠幅度：使用独立的百分比阈值
            double pctThreshold = LATERAL_DEVIATION_PCT_THRESHOLDS.getOrDefault(
                    bikeType, DEFAULT_LATERAL_PCT_THRESHOLD);
            return new MetricDescriptor("骨盆下坠幅度", pctThreshold, "%");
        }
        if (key.contains("knee")) {
            return new MetricDescriptor("膝部伸展角度", angleThreshold, "°");
        }
        if (key.contains("elbow")) {
            return new MetricDescriptor("手肘外张角度", 8.0, "%");
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
