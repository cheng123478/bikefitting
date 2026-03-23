package com.starrysky.aries.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 实时骑行监控结果
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/6 11:17
 */
@Entity
@Table(name = "fit_live_fitting_result")
@Comment("实时骑行监控结果表")
@Getter
@Setter
public class LiveFittingResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // --- 统计数据 (由前端计算好后上报) ---
    @Comment("开始时间")
    private LocalDateTime startTime;
    @Comment("结束时间")
    private LocalDateTime endTime;
    @Comment("有效监控时长(秒)")
    private Integer activeSeconds;
    @Comment("下死点膝角平均值")
    private Double avgKneeAngleBottom;
    @Comment("膝内扣/异常晃动累计次数")
    private Integer kneeAnomalyCount;
    @Comment("骨盆失稳累计次数")
    private Integer pelvicAnomalyCount;
    // --- 过程数据 (时序快照) ---
    @Comment("每分钟数据快照 (JSON格式，存[时间点,膝角,背角]等核心指标)")
    @Column(columnDefinition = "TEXT")
    private String timeSeriesDataJson;
    // --- 结论 ---
    @Comment("本次骑行即时评语(AI生成或规则引擎生成)")
    @Column(columnDefinition = "TEXT")
    private String sessionSummary;
    @Comment("创建时间")
    private LocalDateTime createTime;

    @Comment("关联用户ID")
    private Long userId;
    @Comment("关联单车ID")
    private Long userBikeInfoId;
    @Comment("关联身体档案ID")
    private Long userBodyInfoId;
}
