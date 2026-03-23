package com.starrysky.aries.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 动态采集会话表 - 核心锚点
 *
 * @author antigravity
 * @version 1.0
 * @since 2026/03/03
 */
@Entity
@Table(name = "fit_dynamic_session")
@Comment("动态采集会话基础表")
@Getter
@Setter
public class DynamicFittingSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("主键 ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
    @Comment("归属用户")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_bike_id", foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
    @Comment("关联单车")
    private UserBike userBike;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_body_id", foreignKey = @ForeignKey(name = "none", value = ConstraintMode.NO_CONSTRAINT))
    @Comment("关联身体数据")
    private UserBody userBody;

    @Comment("状态: 1:待分析, 2:已完成, 3:失败")
    private Integer status = 1;

    @Comment("采集设备元数据(FPS, 分辨率等)")
    @Column(columnDefinition = "JSON")
    private String deviceInfo;

    @Comment("前端采集时间")
    private LocalDateTime sessionTime;

    @Comment("创建时间")
    @CreationTimestamp
    private LocalDateTime createTime;

    @Comment("更新时间")
    @UpdateTimestamp
    private LocalDateTime updateTime;

    @Comment("软删除: 0:未删除, 1:已删除")
    private Integer isDeleted = 0;
}
