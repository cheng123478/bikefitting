package com.starrysky.aries.dynamicfitting.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 动态 Fitting 建议 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynamicFittingAdviceVO {
    private String part; // 部位: cleat, seat, handlebar, body
    private String type; // 类型: adjust, check
    private String code; // 建议编码 (用于 i18n): e.g., "advice.seat.too_high"
    private String metric; // 关联指标 Key
    private Double val; // 当前实测值
    private String range; // 理想范围描述
    private Integer importance; // 重要性: 1(高), 2(常规)
    private Integer sequence; // 调整顺序: 1(锁片), 2(座垫), 3(车把)
}
