package com.starrysky.aries.dynamicfitting.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 动态 Fitting 报告详细数据 VO (扁平化结构)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DynamicFittingReportVO {
    private Long id;
    private Long sessionId;
    private Long bikeId;
    private BigDecimal matchScore;
    private String matchLevel;
    private String summary;
    private Integer viewsCount;
    private String bikeName;
    private LocalDateTime createTime;

    // 结构化数据，对应前端 report.sideData.metrics
    private ViewData sideData;
    private ViewData frontData;
    private ViewData backData;

    // 建议列表
    private List<String> equipmentSuggestions;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ViewData {
        private List<DynamicFittingMetricVO> metrics;
    }
}
