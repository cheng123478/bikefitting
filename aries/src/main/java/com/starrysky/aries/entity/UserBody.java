package com.starrysky.aries.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 用户身体信息
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/4 21:10
 */
@Entity
@Table(name = "fit_user_body")
@Comment("用户身体信息表")
@Setter
@Getter
public class UserBody {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("主键")
    private Long id;
    // 基础尺寸 (mm)
    @Comment("身高")
    private Integer height;
    @Comment("跨高")
    private Integer spanHeight;
    @Comment("胸高 (用于算躯干长)")
    private Integer breastHeight;

    @Comment("肩宽")
    private Integer shoulderWidth;
    @Comment("大臂长")
    private Integer longArmLength;
    @Comment("小臂长")
    private Integer shortArmLength;
    @Comment("大腿长")
    private Integer thighLength;
    @Comment("小腿长")
    private Integer calfLength;
    @Comment("坐骨间距")
    private Integer ischialDistance;
    @Comment("脚长")
    private Integer footLength;
    @Comment("体重(kg)")
    private Double weightKg;
    @Comment("腰围(mm)")
    private Integer waistCircumference;

    // 灵活性与稳定性测试
    @Comment("举臂角度(0-180)")
    private Double armRaiseAngle;
    @Comment("胸椎旋转角度(0-90)")
    private Double thoracicRotationAngle;
    @Comment("胸椎痛感等级(0-5)")
    private Integer thoracicPainLevel;
    @Comment("深蹲深度(负值越深越好)")
    private Double squatDepth;
    @Comment("深蹲稳定性评分(1-5)")
    private Integer squatStabilityScore;
    @Comment("平板支撑评分(1-5)")
    private Integer plankScore;

    @CreationTimestamp
    @Comment("创建时间")
    private LocalDateTime createTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
    @Comment("关联的用户对象")
    private User user;
}
