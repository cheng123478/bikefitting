package com.starrysky.aries.staticfitting.schema;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 静态调教方案报告
 * 包含：匹配度评分、几何偏差分析、器材更换建议
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/15 22:23
 */
@Setter
@Getter
public class StaticFittingReportVO {
    private String fittingResultId;
    private String overallStatus;      // PASS, WARNING, DANGER
    private String createTime;
    private Integer matchScore;        // 综合匹配度 (0-100)
    private String matchLevel;         // 评价等级
    private String summary;            // Fittor 专家综述

    // --- 2. 车架几何适用性 (Frame Compatibility) ---
    // 判断这辆车的尺码本身对不对，这是改不了的“底子”
    private StaticFittingMetricVO frameStackCheck;
    private StaticFittingMetricVO frameReachCheck;

    // --- 3. 机械调校处方 (Mechanical Adjustments) ---
    private List<StaticFittingMetricVO> mechanicalAdjustments;

    // --- 4. 专家级病理诊断 (Expert Diagnoses) ---
    private List<String> expertDiagnoses;

    // --- 5. 专属康复与纠偏计划 (Recovery Plan) ---
    private List<RecoveryPlanVO> recoveryPlan;

    // --- 6. 硬件升级防坑建议 (Hardware Upgrades) ---
    private List<HardwareUpgradeVO> hardwareUpgrades;
}
