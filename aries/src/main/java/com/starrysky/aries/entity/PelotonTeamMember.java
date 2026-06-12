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
 * PelotonTeamMember
 * 车队成员关联实体
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/12
 */
@Entity
@Table(name = "plt_team_member")
@Comment("车队成员表")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PelotonTeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Comment("主键")
    private Long id;

    @Column(name = "team_id", nullable = false)
    @Comment("车队ID")
    private Long teamId;

    @Column(name = "user_id", nullable = false)
    @Comment("用户ID")
    private Long userId;

    @Column(length = 20)
    @Comment("角色: 队长, 管理员, 成员")
    private String role;

    @Column(length = 50)
    @Comment("成员标签，如: 爬坡手, 破风手, 冲刺手")
    private String tag;

    @CreationTimestamp
    @Comment("加入时间")
    private LocalDateTime joinDate;
}
