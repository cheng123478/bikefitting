package com.starrysky.aries.staticfitting.schema;

import lombok.Getter;
import lombok.Setter;

/**
 * 报告的最小原子，每一个测量项（如座高）都会生成一个这样的对象。
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/15 22:24
 */
@Setter
@Getter
public class StaticFittingMetricVO {
    private String label;        // 指标名 (e.g. "座垫高度")
    private String unit;         // 单位 (e.g. "mm")

    private Number idealValue;   // 理论金数 (Calculated)
    private Number currentValue; // 实车现状 (From Garage)
    private Number delta;        // 偏差值 (Ideal - Current)

    private String status;       // 状态: "PERFECT"(绿), "ADJUST"(黄), "WARNING"(红)
    private String action;       // 行动指令: "升高 5mm", "缩短 10mm", "完美"

    private String reason;       // 生物力学理由 (e.g. "基于您的跨高与股胫比")
}
