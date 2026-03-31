package com.starrysky.aries.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.io.Serial;
import java.io.Serializable;

/**
 * 动态 Fitting 标准实体
 *
 * @author adi
 * @version 1.0
 * @since 2026/3/4 14:06
 */
@Entity
@Table(name = "fit_dynamic_standard")
@Comment("动态Fitting标准表")
@Setter
@Getter
public class DynamicFittingStandard implements Serializable {
    @Serial
    private static final long serialVersionUID = 1470464342392309427L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer bikeType; // 1:Road, 2:MTB, 3:Gravel, 4:TT, 5:City
    private String viewType; // 视角 SIDE/FRONT/BACK
    private String metricKey; // 直接对齐JSON中的key
    private Double minVal; // 最小值
    private Double maxVal; // 最大值
    private Double weight; // 评分权重 (0.0-1.0)
    private Double sensitivity; // 默认减分敏感系数
    private Double lowSensitivity; // 偏低偏差敏感系数 (val < min)
    private Double highSensitivity; // 偏高偏差敏感系数 (val > max)
    private String part; // 调整部位: seat, handlebar, stem, crank, cleat
    private String lowAdvice; // 偏低建议码: advice.seat.low
    private String highAdvice; // 偏高建议码: advice.seat.high
}
