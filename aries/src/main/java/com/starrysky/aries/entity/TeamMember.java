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

/**
 * TeamMember
 * 车队成员实体（映射 team_member 表）
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/12
 */
@Entity
@Table(name = "team_member",
       uniqueConstraints = @UniqueConstraint(name = "uk_team_user", columnNames = {"team_id", "user_id"}),
       indexes = {
           @Index(name = "idx_user", columnList = "user_id"),
           @Index(name = "idx_team_status", columnList = "team_id, status")
       })
@Comment("车队成员表")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamMember {

    /** 角色: 队长 */
    public static final int ROLE_CAPTAIN = 0;
    /** 角色: 管理员 */
    public static final int ROLE_ADMIN = 1;
    /** 角色: 成员 */
    public static final int ROLE_MEMBER = 2;

    /** 状态: 正常 */
    public static final int STATUS_ACTIVE = 0;
    /** 状态: 已退出 */
    public static final int STATUS_LEFT = 1;
    /** 状态: 被踢出 */
    public static final int STATUS_KICKED = 2;

    /** 新成员默认骑行标签 */
    public static final String DEFAULT_TAG = "新秀";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    @Comment("所属车队")
    private Long teamId;

    @Column(name = "user_id", nullable = false)
    @Comment("用户ID")
    private Long userId;

    @Column(nullable = false)
    @Comment("角色: 0队长 1管理员 2成员")
    private Integer role = ROLE_MEMBER;

    @Column(length = 20)
    @Comment("骑行标签")
    private String tag = "新秀";

    @Column(name = "join_date", nullable = false)
    @Comment("入队日期")
    private LocalDate joinDate;

    @Column(nullable = false)
    @Comment("状态: 0正常 1已退出 2被踢出")
    private Integer status = STATUS_ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
