package com.starrysky.aries.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * TeamJoinApplication
 * 车队加入申请表
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/12
 */
@Entity
@Table(name = "team_join_application",
       indexes = {
           @Index(name = "idx_team_status", columnList = "team_id, status"),
           @Index(name = "idx_applicant", columnList = "applicant_id")
       })
@Comment("车队加入申请表")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamJoinApplication {

    /** 状态: 待审核 */
    public static final int STATUS_PENDING = 0;
    /** 状态: 通过 */
    public static final int STATUS_APPROVED = 1;
    /** 状态: 拒绝 */
    public static final int STATUS_REJECTED = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    @Comment("目标车队")
    private Long teamId;

    @Column(name = "applicant_id", nullable = false)
    @Comment("申请人用户ID")
    private Long applicantId;

    @Column(length = 200, nullable = false)
    @Comment("申请理由")
    private String reason;

    @Column(nullable = false)
    @Comment("状态: 0待审核 1通过 2拒绝")
    private Integer status = 0;

    @Column(name = "reviewer_id")
    @Comment("审核人用户ID")
    private Long reviewerId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    @Comment("申请时间")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @Comment("更新时间")
    private LocalDateTime updatedAt;
}
