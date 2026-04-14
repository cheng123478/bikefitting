package com.starrysky.aries.dynamicfitting.schema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 动态Fitting报告详细数据VO(扁平化结构)
 *
 * @author adi
 * @version 1.0
 * @since 2026/3/7 21:23
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

    // 踩踏分析专属数据
    private PedalingData pedalingData;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ViewData {
        private List<DynamicFittingMetricVO> metrics;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PedalingData {
        private Integer leftPowerRatio; // 左腿发力占比(百分比)
        private Integer rightPowerRatio; // 右腿发力占比(百分比)
        private Integer leftPeakAngle; // 左腿发力峰值角度
        private Integer rightPeakAngle; // 右腿发力峰值角度
        private Integer upstrokeDragIndex; // 提拉阶段不平顺指数 (0-100)
        private Double lateralWobbleLeft; // 左膝横向摆动量(mm) - 可选
        private Double lateralWobbleRight; // 右膝横向摆动量(mm) - 可选

        // 连续曲滑线数据，用于 echarts 渲染
        // 每个元素包含：{"angle": 0, "leftVelocity": 85, "rightVelocity": 20, "totalVelocity": 105}
        private List<Map<String, Object>> forceCurvePoints;
    }
}
