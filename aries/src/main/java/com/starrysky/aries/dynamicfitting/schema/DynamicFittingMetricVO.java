package com.starrysky.aries.dynamicfitting.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 动态指标评价VO(单一指标)
 *
 * @author adice
 * @version 1.0
 * @since 2025-08-08 11:19:57
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynamicFittingMetricVO {
    private String label; // 指标中文名: e.g., "膝角 (伸展度)"
    private String status; // 状态: OK, ADJUST, WARNING
    private String currentValue; // 实测值: e.g., "142.5"
    private String idealRange; // 理想范围: e.g., "140-150"
    private String advice; // 针对该指标的具体建议（调车方向）
    private String riderAdvice; // 针对骑车人自身动作纠正/习惯改善的建议
}
