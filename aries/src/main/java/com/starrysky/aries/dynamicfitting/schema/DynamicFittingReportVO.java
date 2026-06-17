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

    // 诊断标准范围映射
    private Map<String, String> standardRanges;

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
        private Integer leftPowerRatio; // 左腿运动幅度占比代理值(百分比)，非功率计实测功率
        private Integer rightPowerRatio; // 右腿运动幅度占比代理值(百分比)，非功率计实测功率
        private Integer leftPeakAngle; // 左腿运动代理曲线峰值参考相位
        private Integer rightPeakAngle; // 右腿运动代理曲线峰值参考相位
        private Integer upstrokeDragIndex; // 上提阶段不平顺代理指数 (0-100)
        private Integer cadenceRpm; // 踏频(RPM)，来自前端按真实视频时长修正后的估算
        private Double lateralWobbleLeft; // 左膝横向摆动量(% 骨盆宽) - 可选
        private Double lateralWobbleRight; // 右膝横向摆动量(% 骨盆宽) - 可选
        private String bilateralConfidence; // HIGH: 正面双侧运动学代理; LOW: 侧面对称假设
        private String dataSource; // front / side
        private String analysisNote; // 数据可信度说明

        // 连续曲线数据，用于 echarts 渲染；数值为运动学代理输出指数
        // 每个元素包含：{"angle": 0, "leftVelocity": 85, "rightVelocity": 20, "totalVelocity": 105}
        private List<Map<String, Object>> forceCurvePoints;
    }
}
