package com.starrysky.aries.userbody.schema;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户身体信息响应VO
 * 包含原始数据、计算得出的画像分析及评分
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/6 15:39
 */
@Setter
@Getter
public class UserBodyVO {
    private Long id;
    private Long userId;
    private LocalDateTime createTime;

    // --- 原始数据展示 ---
    private Byte gender; //从User聚合而来
    private Integer age;
    private Integer height;
    private Integer spanHeight;
    private Integer breastHeight;
    private Integer shoulderWidth;
    private Integer longArmLength;
    private Integer shortArmLength;
    private Integer thighLength;
    private Integer calfLength;
    private Integer ischialDistance;
    private Integer footLength;
    private Double armRaiseAngle;
    private Double thoracicRotationAngle;
    private Double thoracicExtensionAngle;
    private Integer thoracicPainLevel;
    private Double squatDepth;
    private Integer squatStabilityScore;
    // --- Fittor生物力学分析结果 ---
    private Double apeIndex;          // 猿臂比
    private Double femurTibiaRatio;   // 股胫比
    private Double trunkRatio;        // 躯干占比
    private List<String> riderTags;   // 骑士画像标签 (如: "长股骨特征", "气动天赋")
    private String expertComment;     // 专家寄语/诊断文案
    // 评分与初判建议
    private Integer flexibilityScore; // 柔韧性得分 (0-100)
    private Integer stabilityScore;   // 稳定性得分 (0-100)
    private Integer recCrankLength;   // 建议曲柄长度基准 (mm)
    private String saddleWidthAdvice; // 建议座垫宽度范围
}
