package com.starrysky.aries.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 存储具体的生物力学诊断逻辑：即“因为你身体某特征，所以我们要怎么调”
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/4 21:36
 */
@Entity
@Table(name = "fit_diagnostic_detail")
@Comment("静态Fitting诊断明细表")
@Getter
@Setter
public class DiagnosticDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("针对部位 (如下肢、车头、传动)")
    private String part;

    @Comment("识别的身体特征 (如长股骨、肩紧)")
    private String bodyFeature;

    @Comment("调教算法逻辑 (诊断结论)")
    @Column(columnDefinition = "TEXT")
    private String adjustmentLogic;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "result_id", foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
    private StaticFittingResult staticFittingResult;
}