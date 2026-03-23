package com.starrysky.aries.entity;

import com.starrysky.aries.staticfitting.service.PainTypeListConverter;
import com.starrysky.aries.staticfitting.service.StringListConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.starrysky.aries.staticfitting.schema.RidingPainType;

/**
 * 静态Fitting结果
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/4 21:25
 */
@Entity
@Table(name = "fit_static_result")
@Comment("静态Fitting结果主表")
@Setter
@Getter
public class StaticFittingResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("主键")
    private Long id;
    // --- 核心推荐数值 (单位: mm) ---
    @Comment("建议座垫高度")
    private Integer recSeatHeight;
    @Comment("建议座垫后移量(Setback)")
    private Integer recSetback;
    @Comment("建议曲柄长度")
    private Integer recCrankLength;
    @Comment("建议座垫宽度")
    private Integer recSaddleWidth;
    @Comment("建议把横宽度")
    private Integer recHandlebarWidth;
    @Comment("建议把立长度")
    private Double recStemLength;
    // --- 空间坐标数据 (Stack/Reach Plus) ---
    @Comment("建议车把堆高坐标(Stack+)")
    private Double recStackPlus;
    @Comment("建议空间前伸坐标(Reach+)")
    private Double recReachPlus;
    @Comment("建议车把落差(Drop)")
    private Double recHandlebarDrop;
    // --- 中间评分(用于展示报告雷达图) ---
    @Comment("计算出的胸椎灵活性得分")
    private Integer thoracicScore;
    @Comment("计算出的深蹲稳定性得分")
    private Integer squatScore;
    // --- 细诊断条目 (关联表) ---
    @OneToMany(mappedBy = "staticFittingResult", cascade = CascadeType.ALL, orphanRemoval = true)
    @Comment("生物力学诊断明细")
    private List<DiagnosticDetail> details = new ArrayList<>();
    // --- 诊断文案与标签 ---
    @Comment("骑士画像标签 (存储为逗号分隔的字符串，如：大长腿,柔韧极佳)")
    @Column(name = "rider_tags", length = 500) // 预留足够的长度
    @Convert(converter = StringListConverter.class)
    private List<String> riderTags = new ArrayList<>();

    @Comment("本次Fitting评估时用户反馈的疼痛症状 (逗号分隔存储)")
    @Column(name = "pain_symptoms", length = 255) // 长度视枚举数量定
    @Convert(converter = PainTypeListConverter.class) // 使用转换器
    private List<RidingPainType> inputPainTypes;

    @Comment("综合骑姿诊断报告")
    @Column(columnDefinition = "TEXT")
    private String postureSuggestion;
    @Comment("测试时间")
    private LocalDateTime fittingTime;
    @Comment("记录名称/备注 (如: Fitting记录-20260205)")
    private String remark;

    // --- 基础关联信息 ---
    @Comment("单车信息ID")
    private Long userBikeId;
    @Comment("单车类型 (1:公路, 2:山地, 3:Gravel, 4:TT, 5:城市)")
    private Integer bikeType; // 必填：无论是买车还是调教，必须知道车型
    @Comment("关联的身体测量数据ID")
    private Long userBodyId;

}
