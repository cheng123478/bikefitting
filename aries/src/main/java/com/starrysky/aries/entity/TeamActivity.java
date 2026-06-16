package com.starrysky.aries.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * TeamActivity
 * 车队活动实体
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/16
 */
@Entity
@Table(name = "team_activity",
       indexes = {
           @Index(name = "idx_team_status_date", columnList = "team_id, status, event_date DESC"),
           @Index(name = "idx_author", columnList = "author_id")
       })
@Comment("车队活动表")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamActivity {

    /** 状态: 正常 */
    public static final int STATUS_ACTIVE = 0;
    /** 状态: 已取消 */
    public static final int STATUS_CANCELLED = 1;
    /** 状态: 已删除 */
    public static final int STATUS_DELETED = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    @Comment("所属车队")
    private Long teamId;

    @Column(name = "author_id", nullable = false)
    @Comment("发布者用户ID")
    private Long authorId;

    @Column(nullable = false, length = 50)
    @Comment("活动标题")
    private String title;

    @Column(name = "activity_type", nullable = false, length = 20)
    @Comment("活动类型: ride/race/training/social")
    private String activityType;

    @Column(name = "event_date", nullable = false)
    @Comment("活动日期")
    private LocalDate eventDate;

    @Column(name = "event_time")
    @Comment("开始时间")
    private LocalTime eventTime;

    @Column(nullable = false, length = 100)
    @Comment("活动地点")
    private String location;

    @Column(length = 200)
    @Comment("骑行路线")
    private String route;

    @Column(length = 20)
    @Comment("难度等级: easy/medium/hard")
    private String difficulty;

    @Comment("预计里程(km)")
    private Integer distance;

    @Column(name = "max_participants")
    @Comment("人数上限")
    private Integer maxParticipants;

    @Column(nullable = false, columnDefinition = "TEXT")
    @Comment("活动描述")
    private String description;

    @Column(name = "cover_image", length = 500)
    @Comment("封面图URL")
    private String coverImage;

    @Column(name = "participant_count", nullable = false)
    @Comment("报名人数(冗余)")
    private Integer participantCount = 0;

    @Column(nullable = false)
    @Comment("状态: 0正常 1已取消 2已删除")
    private Integer status = STATUS_ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
