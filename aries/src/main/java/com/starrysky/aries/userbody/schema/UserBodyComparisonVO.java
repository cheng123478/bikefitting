package com.starrysky.aries.userbody.schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 2次用户身体信息比较VO类
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/10 9:03
 */
@Setter
@Getter
public class UserBodyComparisonVO {
    private UserBodyVO baseRecord;    // 基准记录 (较早的)
    private UserBodyVO targetRecord;  // 目标记录 (较新的)

    // 差异矩阵：用于前端表格循环渲染
    private List<MetricDiff> diffMatrix;

    // 综合进化评价 (AI或规则引擎生成)
    private String evolutionSummary;

    @Setter
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MetricDiff {
        private String label;      // 维度名称: "胸椎旋转"
        private String unit;       // 单位: "°"
        private Object baseVal;    // 基准值
        private Object targetVal;  // 目标值
        private Double delta;      // 差值
        private String trend;      // "UP", "DOWN", "STABLE"
        private String significance; // 生物力学意义: "提高气动上限"
    }
}
