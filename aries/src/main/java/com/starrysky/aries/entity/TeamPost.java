package com.starrysky.aries.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * TeamPost
 * 车队动态实体（映射 team_post 表）
 *
 * @author adi
 * @version 1.0
 * @since 2026/06/15
 */
@Entity
@Table(name = "team_post",
       indexes = {
           @Index(name = "idx_team_status_time", columnList = "team_id, status, created_at DESC"),
           @Index(name = "idx_author", columnList = "author_id")
       })
@Comment("车队动态表")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamPost {

    /** 状态: 正常 */
    public static final int STATUS_ACTIVE = 0;
    /** 状态: 已删除 */
    public static final int STATUS_DELETED = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    @Comment("所属车队")
    private Long teamId;

    @Column(name = "author_id", nullable = false)
    @Comment("发布者用户ID")
    private Long authorId;

    @Column(length = 500)
    @Comment("动态文本内容")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    @Comment("图片URL列表")
    private List<String> images;

    @Column(name = "like_count", nullable = false)
    @Comment("点赞数(冗余)")
    private Integer likeCount = 0;

    @Column(name = "comment_count", nullable = false)
    @Comment("评论数(冗余)")
    private Integer commentCount = 0;

    @Column(nullable = false)
    @Comment("状态: 0正常 1已删除")
    private Integer status = STATUS_ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
