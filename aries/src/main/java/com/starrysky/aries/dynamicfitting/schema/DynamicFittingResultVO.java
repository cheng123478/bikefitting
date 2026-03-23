package com.starrysky.aries.dynamicfitting.schema;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 动态Fitting结果VO - 诊断核心结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynamicFittingResultVO {
    private Long id;
    private BigDecimal matchScore;
    private String matchLevel; // e.g., "专业推荐"
    private String summary; // 总体简报
    private LocalDateTime createTime;

    // 诊断生成的详细建议列表
    private List<DynamicFittingAdviceVO> advices;
}
