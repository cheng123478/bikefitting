package com.starrysky.aries.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 动态分析报告表 - 结论输出
 *
 * @author antigravity
 * @version 1.0
 * @since 2026/03/03
 */
@Entity
@Table(name = "fit_dynamic_result")
@Comment("动态分析诊断报告")
@Getter
@Setter
public class DynamicFittingResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("主键 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
    @Comment("关联会话")
    private DynamicFittingSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_body_id", foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
    @Comment("记录诊断时参考的身体测量数据")
    private UserBody userBody;

    @Comment("姿态匹配得分")
    private BigDecimal matchScore;

    @Comment("各视角统计中值/平均值汇总")
    @Column(columnDefinition = "JSON")
    private String summaryJson;

    @Comment("结构化调整建议")
    @Column(columnDefinition = "JSON")
    private String adviceJson;

    @Comment("指标稳定性分析数据 (CV/标准差)")
    @Column(columnDefinition = "JSON")
    private String stabilityJson;

    @Comment("生成时间")
    @CreationTimestamp
    private LocalDateTime createTime;
}
