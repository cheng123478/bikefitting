package com.starrysky.aries.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 用户单车设置信息
 *
 * @author adi
 * @version 1.0
 * @since 2026/3/9 16:48
 */
@Entity
@Table(name = "fit_user_bike_setup")
@Comment("用户单车设置信息表")
@Setter
@Getter
public class UserBikeSetup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("主键")
    private Long id;
    // --- 当前实车设定 (Current Setup - 关键数据) ---
    @Comment("曲柄长度")
    private Integer crankLength;
    @Comment("把立长度")
    private Integer stemLength;
    @Comment("把横宽度")
    private Integer handlebarWidth;
    @Comment("把立角度(度)")
    private Double stemAngle;
    @Comment("把立下方垫圈高度")
    private Integer spacersHeight;
    @Comment("车头落差(Drop)")
    private Integer handlebarDrop;
    @Comment("座垫宽度")
    private Integer saddleWidth;
    @Comment("当前座垫高度(五通中心至顶端)")
    private Integer saddleHeight;
    @Comment("实车座垫后移量(Setback)")
    private Integer saddleSetback;
    @Comment("座垫倾角(Tilt)")
    private Double saddleTilt;
    @Comment("踏板类型（1：平踏+运动鞋，2：公路锁踏+锁鞋，3：山地锁踏+锁鞋）")
    private Integer pedalType;
    @Comment("创建时间，同时作为一次的版本号")
    private LocalDateTime createTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fit_user_bike_id", foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
    @Comment("关联的用户单车")
    private UserBike userBike;
}
