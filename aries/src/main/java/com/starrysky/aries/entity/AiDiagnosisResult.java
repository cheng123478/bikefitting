package com.starrysky.aries.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * AI智能诊断结果
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/6 10:46
 */
@Entity
@Table(name = "fit_ai_diagnosis_result")
@Comment("AI智能诊断结果表")
@Getter
@Setter
public class AiDiagnosisResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Comment("用户主诉/反馈问题")
    @Column(columnDefinition = "TEXT")
    private String userQuery;
    @Comment("AI深度分析文案")
    @Column(columnDefinition = "TEXT")
    private String aiAnalysisReport;
    @Comment("AI建议的调整指令(JSON格式)")
    @Column(columnDefinition = "TEXT")
    private String aiActionInstruction;
    @Comment("诊断结论状态 (1:待处理, 2:已采纳调整, 3:已驳回)")
    private Integer status;
    @Comment("生成时间")
    private LocalDateTime createTime;

    @Comment("关联用户ID")
    private Long userId;
    @Comment("关联的单车档案ID")
    private Long userBikeInfoId;
    @Comment("关联的身体档案ID")
    private Long userBodyInfoId;
    @Comment("参考的静态FittingID，可以为空")
    private Long staticFittingResultId;
    @Comment("参考的动态FittingID，可以为空")
    private Long dynamicFittingResultId;
    @Comment("参考的实时监控记录ID，可以为空")
    private Long liveFittingResultId;
}
