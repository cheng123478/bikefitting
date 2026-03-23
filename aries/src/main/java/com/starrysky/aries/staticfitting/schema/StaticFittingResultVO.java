package com.starrysky.aries.staticfitting.schema;

import com.starrysky.aries.entity.DiagnosticDetail;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 静态Fitting结果展示VO
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/20 17:21
 */
@Setter
@Getter
public class StaticFittingResultVO {
    private Long id;
    // --- 核心展示参数 ---
    private Integer recSeatHeight;
    private Integer recSetback;
    private Double recReachPlus;
    private Double recHandlebarDrop;
    private Integer recHandlebarWidth;
    private Integer recSaddleWidth;
    // --- 评分与标签 ---
    private Integer thoracicScore;
    private List<String> riderTags;
    private List<DiagnosticDetail> details; // 当时的诊断文案
    private LocalDateTime fittingTime;
    // --- 关联标识 ---
    private Long userBodyInfoId;
    private Long userBikeInfoId;
    private String bikeName;
    private Integer bikeType;
}
