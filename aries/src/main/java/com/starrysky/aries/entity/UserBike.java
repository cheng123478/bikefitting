package com.starrysky.aries.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.time.LocalDateTime;

/**
 * 用户的单车信息
 *
 * @author adi
 * @version 1.0
 * @since 2026/2/4 21:19
 */
@Entity
@Table(name = "fit_user_bike")
@Comment("用户单车信息表")
@Setter
@Getter
public class UserBike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("主键")
    private Long id;
    @Comment("备注(例如: 我的爬坡主力车)")
    private String remark;
    @Comment("状态:1-现役，2-退役")
    private Integer status;
    @CreationTimestamp
    @Comment("创建时间")
    private LocalDateTime createTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bike_model_id", foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
    @Comment("关联的单车几何信息")
    @NotFound(action = NotFoundAction.IGNORE)
    private BikeModel bikeModel;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
    @Comment("关联的用户")
    private User user;
}
