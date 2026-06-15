package com.starrysky.aries.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * TeamGarage
 * 车队车库实体（映射 team_garage 表）
 * <p>
 * 同步用户个人车库中的单车至车队车库。
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/15
 */
@Entity
@Table(name = "team_garage",
       uniqueConstraints = @UniqueConstraint(name = "uk_team_bike", columnNames = {"team_id", "bike_id"}),
       indexes = {
           @Index(name = "idx_user", columnList = "user_id"),
           @Index(name = "idx_team_status", columnList = "team_id, status")
       })
@Comment("车队车库表")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamGarage {

    /** 状态: 正常 */
    public static final int STATUS_ACTIVE = 0;
    /** 状态: 已移除 */
    public static final int STATUS_REMOVED = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    @Comment("所属车队")
    private Long teamId;

    @Column(name = "user_id", nullable = false)
    @Comment("车主用户ID")
    private Long userId;

    @Column(name = "bike_id", nullable = false)
    @Comment("关联个人车库单车ID")
    private Long bikeId;

    @Column(nullable = false)
    @Comment("状态: 0正常 1已移除")
    private Integer status = STATUS_ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
