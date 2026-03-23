package com.starrysky.aries.dynamicfitting.algorithm;

import com.starrysky.aries.dynamicfitting.schema.DynamicFittingResultVO;
import com.starrysky.aries.dynamicfitting.schema.DynamicFittingAdviceVO;
import com.starrysky.aries.dynamicfitting.repository.DynamicFittingStandardRepository;
import com.starrysky.aries.entity.DynamicFittingStandard;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 动态 Fitting 诊断算法引擎 - 数据库驱动扩展版
 * 支持多车型 (Road, MTB, TT, Gravel, City) 诊断标准，通过数据库灵活配置标准值。
 */
@Component
public class DynamicFittingAlgorithm {

        @Resource
        private DynamicFittingStandardRepository standardRepository;

        /**
         * 核心诊断逻辑
         *
         * @param bikeType 车辆类型 (通过 BikeType 枚举获取 code)
         * @param summary  汇总数据 (包含各视角的统计中值)
         */
        public DynamicFittingResultVO analyze(Integer bikeType, Map<String, Object> summary) {
                // 从数据库检索该车型的所有指标标准 (带缓存)
                List<DynamicFittingStandard> standards = standardRepository.findByBikeType(bikeType);

                // 如果该车型尚无配置，可以退回到默认车型 (如 Road Bike = 1)
                if (standards.isEmpty()) {
                        standards = standardRepository.findByBikeType(1);
                }

                List<DynamicFittingAdviceVO> advices = new ArrayList<>();
                double totalWeightedScore = 0;
                double totalWeight = 0;

                // 记录所有找到的指标，用于后续对称性分析
                Map<String, Double> detectedMetrics = new java.util.HashMap<>();

                for (DynamicFittingStandard std : standards) {
                        String metricKey = std.getMetricKey();
                        String viewType = std.getViewType();

                        // 视角感知检索指标值
                        Double val = null;
                        if (viewType != null && !viewType.isEmpty()) {
                                val = findMetricInView(summary, viewType.toLowerCase(), metricKey);
                        } else {
                                val = findMetricInSummary(summary, metricKey);
                        }

                        if (val != null) {
                                detectedMetrics.put(metricKey, val);

                                // 计算该指标得分 (非对称)
                                double score = calculateAsymmetricScore(val, std);
                                double weight = std.getWeight() != null ? std.getWeight() : 0.1;

                                totalWeightedScore += score * weight;
                                totalWeight += weight;

                                // 生成调整建议 (带优先级和顺序)
                                if (val < std.getMinVal() || val > std.getMaxVal()) {
                                        String adviceCode = val < std.getMinVal() ? std.getLowAdvice()
                                                        : std.getHighAdvice();
                                        advices.add(createAdvice(std, val, adviceCode));
                                }
                        }
                }

                // --- 2. 双侧对称性分析 (Bilateral Asymmetry) ---
                performBilateralAudit(detectedMetrics, advices);

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
                                .build();
        }

        private DynamicFittingAdviceVO createAdvice(DynamicFittingStandard std, Double val, String adviceCode) {
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
                                .metric(std.getMetricKey())
                                .val(val)
                                .range(std.getMinVal() + "-" + std.getMaxVal())
                                .importance(importance)
                                .sequence(sequence)
                                .build();
        }

        /**
         * 非对称得分计算
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
         * 执行双侧对比分析
         */
        private void performBilateralAudit(Map<String, Double> metrics, List<DynamicFittingAdviceVO> advices) {
                metrics.forEach((key, val) -> {
                        if (key.endsWith("_L")) {
                                String rightKey = key.replace("_L", "_R");
                                if (metrics.containsKey(rightKey)) {
                                        double leftVal = val;
                                        double rightVal = metrics.get(rightKey);
                                        double delta = Math.abs(leftVal - rightVal);

                                        // 如果两侧偏差超过 10% 或 阈值(通常为 5-8度)
                                        if (delta > 8.0) {
                                                advices.add(DynamicFittingAdviceVO.builder()
                                                                .part("body")
                                                                .type("check")
                                                                .code("advice.asymmetry.warning")
                                                                .metric(key.substring(0, key.length() - 2))
                                                                .val(null)
                                                                .range("Delta < 5")
                                                                .importance(1)
                                                                .sequence(0)
                                                                .build()); // 顺序为0，最先展示
                                        }
                                }
                        }
                });
        }

        /**
         * 在特定视角中查找指标
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

                return null;
        }

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
